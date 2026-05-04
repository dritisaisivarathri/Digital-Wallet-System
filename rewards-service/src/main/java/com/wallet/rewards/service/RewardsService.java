package com.wallet.rewards.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wallet.rewards.dto.RewardRedeemResponse;
import com.wallet.rewards.dto.WalletCreditRequest;
import com.wallet.rewards.entity.RewardCatalog;
import com.wallet.rewards.entity.RewardEventLog;
import com.wallet.rewards.entity.RewardPoints;
import com.wallet.rewards.repository.RewardCatalogRepository;
import com.wallet.rewards.repository.RewardEventLogRepository;
import com.wallet.rewards.repository.RewardPointsRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class RewardsService {

    private static final Logger log = LoggerFactory.getLogger(RewardsService.class);
    private static final Pattern AMOUNT_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)");
    private static final String INTERNAL_SECRET_HEADER = "X-Internal-Gateway-Token";
    private static final String INTERNAL_SECRET_VALUE = "DigitalWalletInternalSecret2026";
    private static final String WALLET_API_PREFIX = "/api/wallet";
    private static final String NOTIFICATION_API_PREFIX = "/api/notifications";

    @Autowired
    private RewardPointsRepository pointsRepository;

    @Autowired
    private RewardCatalogRepository catalogRepository;

    @Autowired
    private RewardEventLogRepository rewardEventLogRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${services.wallet-service.internal-base-url:http://localhost:8083}")
    private String walletServiceBaseUrl;

    @Value("${services.notification-service.internal-base-url:http://localhost:8086}")
    private String notificationServiceBaseUrl;

    // 1 point for every 100 spent or topped up
    private int calculatePoints(BigDecimal amount) {
        return amount.divideToIntegralValue(new BigDecimal("100")).intValue();
    }

    private RewardPoints getOrCreatePoints(UUID userId) {
        pointsRepository.ensureUserExists(userId);
        return pointsRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Rewards record should exist for user: " + userId));
    }

    @KafkaListener(topics = "wallet.topup.success", groupId = "rewards-group")
    @Transactional
    public void handleTopUp(Map<String, Object> event) {
        log.info("Rewards Service received Top-Up Event: {}", event);
        try {
            syncTopUp(event);
        } catch (Exception e) {
            log.error("Rewards Service Top-Up error: {}", event, e);
        }
    }

    @KafkaListener(topics = "wallet.transfer.completed", groupId = "rewards-group")
    @Transactional
    public void handleTransfer(Map<String, Object> event) {
        log.info("Rewards Service received Transfer Event: {}", event);
        try {
            syncTransfer(event);
        } catch (Exception e) {
            log.error("Rewards Service Transfer error: {}", event, e);
        }
    }

    @Transactional
    public boolean syncTopUp(Map<String, Object> event) {
        log.info("Processing top-up sync event: {}", event);
        UUID userId = UUID.fromString(event.get("userId").toString());
        BigDecimal amount = new BigDecimal(event.get("amount").toString());
        UUID transactionId = UUID.fromString(event.get("transactionId").toString());
        if (rewardEventLogRepository.existsById(transactionId)) {
            log.info("Skipping duplicate reward top-up event {}", transactionId);
            return false;
        }

        int points = calculatePoints(amount);
        if (points > 0) {
            RewardPoints rp = getOrCreatePoints(userId);
            rp.addPoints(points);
            pointsRepository.saveAndFlush(rp);
            log.info("Awarded {} points to user: {}", points, userId);
        }

        rewardEventLogRepository.save(buildEventLog(transactionId, userId, "TOPUP"));
        return true;
    }

    @Transactional
    public boolean syncTransfer(Map<String, Object> event) {
        UUID fromUserId = UUID.fromString(event.get("fromUserId").toString());
        BigDecimal amount = new BigDecimal(event.get("amount").toString());
        UUID transactionId = UUID.fromString(event.get("transactionId").toString());
        if (rewardEventLogRepository.existsById(transactionId)) {
            log.info("Skipping duplicate reward transfer event {}", transactionId);
            return false;
        }

        int points = calculatePoints(amount);
        if (points > 0) {
            RewardPoints rp = getOrCreatePoints(fromUserId);
            rp.addPoints(points);
            pointsRepository.saveAndFlush(rp);
            log.info("Awarded {} points to sender: {}", points, fromUserId);
        }

        rewardEventLogRepository.save(buildEventLog(transactionId, fromUserId, "TRANSFER"));
        return true;
    }

    @Transactional
    public RewardPoints getSummary(UUID userId) {
        return getOrCreatePoints(userId);
    }

    public List<RewardCatalog> getCatalog() {
        return catalogRepository.findAll();
    }

    @Transactional
    public RewardRedeemResponse redeemItem(UUID userId, UUID catalogId) {
        RewardPoints rp = getOrCreatePoints(userId);
        RewardCatalog item = catalogRepository.findById(catalogId)
                .orElseThrow(() -> new RuntimeException("Catalog item not found"));

        if (!canRedeemTier(rp.getTier(), item.getRequiredTier())) {
            throw new RuntimeException("Tier too low to redeem this item");
        }

        int costInPoints = item.getCostInPoints();
        log.info("REDEEM: User {} has {} points. Attempting to redeem {} for {} points.",
                userId, rp.getTotalPoints(), item.getName(), costInPoints);

        if (rp.getTotalPoints() < costInPoints) {
            throw new RuntimeException("Insufficient points");
        }

        if (item.getStockQuantity() <= 0) {
            throw new RuntimeException("Item out of stock");
        }

        // Persist the deduction before any external notification work.
        rp.deductPoints(costInPoints);
        RewardPoints updatedPoints = pointsRepository.saveAndFlush(rp);
        log.info("REDEEM: Points deducted. User {} now has {} points.", userId, updatedPoints.getTotalPoints());

        item.setStockQuantity(item.getStockQuantity() - 1);
        catalogRepository.save(item);

        String rewardType = resolveRewardType(item);
        BigDecimal cashbackAmount = resolveCashbackAmount(item);
        BigDecimal walletBalance = null;

        if ("CASHBACK".equalsIgnoreCase(rewardType) || (cashbackAmount != null && cashbackAmount.compareTo(BigDecimal.ZERO) > 0)) {
            if (cashbackAmount == null || cashbackAmount.compareTo(BigDecimal.ZERO) <= 0) {
                log.warn("Reward identified as CASHBACK but amount is 0 or null. Falling back to 100 for item: {}", item.getName());
                cashbackAmount = new BigDecimal("100"); 
            }
            log.info("Processing CASHBACK redemption for user: {}, amount: {}", userId, cashbackAmount);
            walletBalance = creditWallet(userId, cashbackAmount, item.getName());
            rewardType = "CASHBACK"; 
        } else {
            log.info("Processing VOUCHER redemption for user: {}", userId);
        }

        rewardEventLogRepository.save(buildEventLog(UUID.randomUUID(), userId, "REDEEM_" + rewardType));

        RewardRedeemResponse response = new RewardRedeemResponse();
        response.setRewardId(item.getId());
        response.setRewardName(item.getName());
        response.setRewardType(rewardType);
        response.setPointsSpent(item.getCostInPoints());
        response.setRemainingPoints(updatedPoints.getTotalPoints());
        response.setRemainingStock(item.getStockQuantity());
        response.setCashbackCredited(cashbackAmount);
        response.setWalletBalance(walletBalance);
        response.setRedeemedAt(LocalDateTime.now());
        response.setMessage(rewardType.equals("CASHBACK")
                ? String.format("Reward redeemed successfully. %s cashback of %s has been added to your wallet. Current balance: %s", item.getName(), cashbackAmount.stripTrailingZeros().toPlainString(), walletBalance != null ? walletBalance.toString() : "Updating...")
                : String.format("Reward redeemed successfully. %s voucher details are now available in notifications.", item.getName()));
        log.info("REDEEM: Redeemed {} for user {}. Type: {} Cashback: {}",
                item.getName(), userId, rewardType, cashbackAmount);

        Map<String, Object> notificationEvent = buildRedeemEvent(userId, response);
        syncRewardNotification(notificationEvent);
        publishRewardRedeemed(notificationEvent);
        return response;
    }

    @Transactional
    public RewardCatalog createCatalogItem(RewardCatalog item) {
        RewardCatalog saved = catalogRepository.save(item);
        Map<String, Object> event = new HashMap<>();
        event.put("rewardId", saved.getId() != null ? saved.getId().toString() : null);
        event.put("rewardName", saved.getName());
        event.put("rewardType", resolveRewardType(saved));
        event.put("costInPoints", saved.getCostInPoints());
        event.put("stockQuantity", saved.getStockQuantity());
        event.put("requiredTier", saved.getRequiredTier());
        event.put("createdAt", LocalDateTime.now().toString());
        kafkaTemplate.send("reward.catalog.created", event);
        return saved;
    }

    private String resolveRewardType(RewardCatalog item) {
        String configuredType = String.valueOf(item.getRewardType() == null ? "" : item.getRewardType()).trim().toUpperCase();
        if ("CASHBACK".equals(configuredType) || "VOUCHER".equals(configuredType)) {
            return configuredType;
        }

        // Fallback to text analysis if type is missing or invalid.
        String nameDesc = (String.valueOf(item.getName()) + " " + String.valueOf(item.getDescription())).toLowerCase();
        if (nameDesc.contains("cashback")) {
            return "CASHBACK";
        }
        return "VOUCHER";
    }

    private boolean canRedeemTier(String userTier, String requiredTier) {
        String required = normalizeTier(requiredTier);
        if ("ALL".equals(required) || "BASIC".equals(required)) {
            return true;
        }
        return tierRank(normalizeTier(userTier)) >= tierRank(required);
    }

    private String normalizeTier(String tier) {
        String normalized = String.valueOf(tier == null ? "" : tier).trim().toUpperCase();
        return normalized.isBlank() ? "BASIC" : normalized;
    }

    private int tierRank(String tier) {
        return switch (tier) {
            case "PLATINUM" -> 3;
            case "GOLD" -> 2;
            case "SILVER" -> 1;
            default -> 0;
        };
    }

    private BigDecimal resolveCashbackAmount(RewardCatalog item) {
        if (item.getCashbackAmount() != null && item.getCashbackAmount().compareTo(BigDecimal.ZERO) > 0) {
            return item.getCashbackAmount();
        }
        String text = (String.valueOf(item.getName()) + " " + String.valueOf(item.getDescription())).toLowerCase();
        // Look for numbers only if it mentions cashback, rupees, value, etc.
        if (!text.contains("cashback") && !text.contains("rs") && !text.contains("inr") && !text.contains("rupee") && !text.contains("off")) {
            return BigDecimal.ZERO;
        }
        Matcher matcher = AMOUNT_PATTERN.matcher(text);
        if (matcher.find()) {
            try {
                BigDecimal val = new BigDecimal(matcher.group(1));
                System.out.println("REWARDS_DEBUG: Resolved cashback amount " + val + " from text: " + text);
                return val;
            } catch (Exception e) {
                log.error("Failed to parse amount from reward: {}", text);
            }
        }
        return BigDecimal.ZERO;
    }

    @SuppressWarnings("unchecked")
    private BigDecimal creditWallet(UUID userId, BigDecimal amount, String rewardName) {
        String healthCheckUrl = buildWalletUrl("/health-check");
        log.info("Testing wallet-service connectivity: {}", healthCheckUrl);
        try {
            ResponseEntity<String> healthResponse = restTemplate.getForEntity(healthCheckUrl, String.class);
            log.info("Wallet health-check response: {} - {}", healthResponse.getStatusCode(), healthResponse.getBody());
        } catch (Exception e) {
            log.warn("Wallet health-check FAILED: {}", e.getMessage());
        }

        String walletCreditUrl = buildWalletUrl("/internal/credit");
        log.info("Attempting to credit wallet at: {} for user: {} amount: {}", walletCreditUrl, userId, amount);
        
        WalletCreditRequest request = new WalletCreditRequest(
                userId,
                amount,
                "REWARD_CASHBACK",
                "Cashback credited for reward redemption: " + rewardName
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(INTERNAL_SECRET_HEADER, INTERNAL_SECRET_VALUE);

        HttpEntity<WalletCreditRequest> entity = new HttpEntity<>(request, headers);
        try {
            Map<String, Object> response = restTemplate.postForObject(walletCreditUrl, entity, Map.class);
            if (response == null || response.get("balance") == null) {
                System.err.println("REWARDS_ERROR: Wallet service returned empty response or missing balance");
                throw new RuntimeException("Unable to credit cashback to wallet right now");
            }
            BigDecimal newBalance = new BigDecimal(response.get("balance").toString());
            System.out.println("REWARDS_DEBUG: Wallet credited successfully. New balance: " + newBalance);
            return newBalance;
        } catch (Exception e) {
            System.err.println("REWARDS_ERROR: Failed to call wallet service: " + e.getMessage());
            throw new RuntimeException("Wallet service unreachable: " + e.getMessage());
        }
    }

    private String buildWalletUrl(String endpointPath) {
        String baseUrl = walletServiceBaseUrl == null ? "" : walletServiceBaseUrl.trim();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        String normalizedEndpoint = endpointPath.startsWith("/") ? endpointPath : "/" + endpointPath;
        if (baseUrl.endsWith(WALLET_API_PREFIX)) {
            return baseUrl + normalizedEndpoint;
        }
        if (baseUrl.endsWith("/api")) {
            return baseUrl + "/wallet" + normalizedEndpoint;
        }
        return baseUrl + WALLET_API_PREFIX + normalizedEndpoint;
    }

    private Map<String, Object> buildRedeemEvent(UUID userId, RewardRedeemResponse response) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventId", UUID.randomUUID().toString());
        event.put("userId", userId.toString());
        event.put("rewardId", response.getRewardId() != null ? response.getRewardId().toString() : null);
        event.put("rewardName", response.getRewardName());
        event.put("rewardType", response.getRewardType());
        event.put("pointsSpent", response.getPointsSpent());
        event.put("remainingPoints", response.getRemainingPoints());
        event.put("cashbackCredited", response.getCashbackCredited() != null ? response.getCashbackCredited().toPlainString() : "0");
        event.put("walletBalance", response.getWalletBalance() != null ? response.getWalletBalance().toPlainString() : null);
        event.put("message", response.getMessage());
        event.put("redeemedAt", response.getRedeemedAt() != null ? response.getRedeemedAt().toString() : LocalDateTime.now().toString());
        return event;
    }

    private void syncRewardNotification(Map<String, Object> event) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(INTERNAL_SECRET_HEADER, INTERNAL_SECRET_VALUE);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(event, headers);
        String baseUrl = notificationServiceBaseUrl == null ? "" : notificationServiceBaseUrl.trim();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        String url = baseUrl + NOTIFICATION_API_PREFIX + "/internal/sync/reward-redeemed";
        try {
            restTemplate.postForObject(url, entity, Map.class);
        } catch (Exception e) {
            log.warn("Reward notification sync failed: {}", e.getMessage());
        }
    }

    private void publishRewardRedeemed(Map<String, Object> event) {
        try {
            kafkaTemplate.send("reward.redeemed", event);
        } catch (Exception e) {
            log.warn("Reward redeemed event publish failed: {}", e.getMessage());
        }
    }

    private RewardEventLog buildEventLog(UUID transactionId, UUID userId, String eventType) {
        RewardEventLog logEntry = new RewardEventLog();
        logEntry.setTransactionId(transactionId);
        logEntry.setUserId(userId);
        logEntry.setEventType(eventType);
        logEntry.setProcessedAt(LocalDateTime.now());
        return logEntry;
    }
}
