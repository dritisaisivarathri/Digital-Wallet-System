package com.wallet.core.service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.wallet.core.dto.TopUpRequest;
import com.wallet.core.dto.TransferRequest;
import com.wallet.core.entity.WalletAccount;
import com.wallet.core.repository.WalletAccountRepository;

@Service
public class WalletService {

    private static final Logger log = LoggerFactory.getLogger(WalletService.class);


    @Autowired
    private WalletAccountRepository repository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private RestTemplate restTemplate;

    private static final String REDIS_PREFIX = "wallet_balance_";
    private static final String INTERNAL_SECRET_HEADER = "X-Internal-Gateway-Token";
    private static final String INTERNAL_SECRET_VALUE = "DigitalWalletInternalSecret2026";
    private static final String TRANSACTION_API_PREFIX = "/api/transactions";
    private static final String REWARDS_API_PREFIX = "/api/rewards";
    private static final String NOTIFICATION_API_PREFIX = "/api/notifications";

    @Value("${services.transaction-service.internal-base-url:http://localhost:8084}")
    private String transactionServiceBaseUrl;

    @Value("${services.rewards-service.internal-base-url:http://localhost:8085}")
    private String rewardsServiceBaseUrl;

    @Value("${services.user-service.internal-base-url:http://localhost:8082}")
    private String userServiceBaseUrl;

    @Value("${services.auth-service.internal-base-url:http://localhost:8081}")
    private String authServiceBaseUrl;

    @Value("${services.notification-service.internal-base-url:http://localhost:8086}")
    private String notificationServiceBaseUrl;

    public WalletAccount initializeWallet(UUID userId) {
        WalletAccount account = new WalletAccount();
        account.setUserId(userId);
        account.setCachedBalance(BigDecimal.ZERO);
        account.setStatus("ACTIVE");
        return repository.save(account);
    }

    public BigDecimal getBalance(UUID userId) {
        Object cached = redisTemplate.opsForValue().get(REDIS_PREFIX + userId);
        if (cached != null) {
            return new BigDecimal(cached.toString());
        }
        WalletAccount account = repository.findByUserId(userId).orElseGet(() -> initializeWallet(userId));
        redisTemplate.opsForValue().set(REDIS_PREFIX + userId, account.getCachedBalance().toString());
        return account.getCachedBalance();
    }

    @Transactional
    public String topUp(UUID userId, TopUpRequest request) {
        WalletAccount account = repository.findByUserId(userId).orElseGet(() -> initializeWallet(userId));
        account.setCachedBalance(account.getCachedBalance().add(request.getAmount()));
        repository.saveAndFlush(account);
        Map<String, Object> event = buildTopUpEvent(userId, request.getAmount(), request.getPaymentMethod(), "TOPUP", "Wallet top-up");
        syncTopUpDependents(event);
        runAfterCommit(() -> {
            redisTemplate.opsForValue().set(REDIS_PREFIX + userId, account.getCachedBalance().toString());
            publishWalletEvent("wallet.topup.success", event, "TOPUP");
        });
        
        String method = request.getPaymentMethod() == null || request.getPaymentMethod().isBlank() ? "your selected method" : request.getPaymentMethod();
        return "Top-up successful. Rs " + request.getAmount().stripTrailingZeros().toPlainString() + " added to your wallet using " + method + ".";
    }

    @Transactional
    public String transfer(UUID fromUserId, TransferRequest request) {
        if (fromUserId.equals(request.getTargetUserId())) {
            throw new RuntimeException("Cannot transfer money to yourself. Please use a different target user ID.");
        }
        WalletAccount fromAccount = repository.findByUserId(fromUserId)
                .orElseGet(() -> initializeWallet(fromUserId));
        
        BigDecimal amount = request.getAmount();
        if (fromAccount.getCachedBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Enter correct amount. Your wallet balance is not enough for this transfer.");
        }

        // Auth service is the source of truth for stable registered user IDs.
        verifyUserExists(request.getTargetUserId());

        WalletAccount toAccount = repository.findByUserId(request.getTargetUserId())
                .orElseGet(() -> initializeWallet(request.getTargetUserId()));

        // Deduct and add
        log.info("Transfer: Deducting {} from sender {} (Current balance: {})", amount, fromUserId, fromAccount.getCachedBalance());
        fromAccount.setCachedBalance(fromAccount.getCachedBalance().subtract(amount));

        log.info("Transfer: Adding {} to receiver {} (Current balance: {})", amount, request.getTargetUserId(), toAccount.getCachedBalance());
        toAccount.setCachedBalance(toAccount.getCachedBalance().add(amount));

        repository.saveAndFlush(fromAccount);
        repository.saveAndFlush(toAccount);
        log.info("Transfer: Both accounts saved successfully. New balances: Sender={}, Receiver={}",
                fromAccount.getCachedBalance(), toAccount.getCachedBalance());

        Map<String, Object> event = buildTransferEvent(fromUserId, request.getTargetUserId(), amount, request.getNotes());
        syncTransferDependents(event);
        runAfterCommit(() -> {
            redisTemplate.opsForValue().set(REDIS_PREFIX + fromUserId, fromAccount.getCachedBalance().toString());
            redisTemplate.opsForValue().set(REDIS_PREFIX + request.getTargetUserId(), toAccount.getCachedBalance().toString());
            publishWalletEvent("wallet.transfer.completed", event, "TRANSFER");
        });

        return "Funds sent successfully. Rs " + amount.stripTrailingZeros().toPlainString() + " was sent to user " + request.getTargetUserId() + ".";
    }

    @Transactional
    public String withdraw(UUID userId, com.wallet.core.dto.WithdrawRequest request) {
        WalletAccount account = repository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Wallet not found for user: " + userId));

        BigDecimal amount = request.getAmount();
        if (account.getCachedBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient funds for withdrawal");
        }

        account.setCachedBalance(account.getCachedBalance().subtract(amount));
        repository.saveAndFlush(account);

        Map<String, Object> event = buildWithdrawEvent(userId, amount, request.getNotes());
        syncWithdrawDependents(event);
        runAfterCommit(() -> {
            redisTemplate.opsForValue().set(REDIS_PREFIX + userId, account.getCachedBalance().toString());
            publishWalletEvent("wallet.withdraw.success", event, "WITHDRAWAL");
        });

        return "Withdrawal successful. Rs " + amount.stripTrailingZeros().toPlainString() + " has been debited from your wallet.";
    }

    @Transactional
    public BigDecimal creditFromReward(UUID userId, BigDecimal amount, String source, String note) {
        WalletAccount account = repository.findByUserId(userId).orElseGet(() -> initializeWallet(userId));
        System.out.println("DEBUG: creditFromReward called for User: " + userId + " Amount: " + amount + " Source: " + source);
        account.setCachedBalance(account.getCachedBalance().add(amount));
        repository.saveAndFlush(account);

        String resolvedSource = source != null && !source.isBlank() ? source : "REWARD_CASHBACK";
        String resolvedNote = note != null && !note.isBlank() ? note : "Reward cashback credited";
        Map<String, Object> event = buildTopUpEvent(userId, amount, resolvedSource, "REWARD_CASHBACK", resolvedNote);
        syncRewardCreditDependents(event);
        runAfterCommit(() -> {
            redisTemplate.opsForValue().set(REDIS_PREFIX + userId, account.getCachedBalance().toString());
            publishWalletEvent("wallet.topup.success", event, "TOPUP");
        });
        return account.getCachedBalance();
    }

    private Map<String, Object> buildTopUpEvent(UUID userId, BigDecimal amount, String paymentMethod, String subType, String notes) {
        Map<String, Object> event = new HashMap<>();
        event.put("userId", userId);
        event.put("amount", amount);
        event.put("paymentMethod", paymentMethod);
        event.put("type", "CREDIT");
        event.put("subType", subType);
        event.put("notes", notes);
        event.put("transactionId", UUID.randomUUID());
        return event;
    }

    private Map<String, Object> buildTransferEvent(UUID fromUserId, UUID toUserId, BigDecimal amount, String notes) {
        Map<String, Object> event = new HashMap<>();
        event.put("fromUserId", fromUserId);
        event.put("toUserId", toUserId);
        event.put("amount", amount);
        event.put("notes", notes);
        event.put("type", "TRANSFER");
        event.put("transactionId", UUID.randomUUID());
        return event;
    }

    private Map<String, Object> buildWithdrawEvent(UUID userId, BigDecimal amount, String notes) {
        Map<String, Object> event = new HashMap<>();
        event.put("userId", userId);
        event.put("amount", amount);
        event.put("notes", notes);
        event.put("type", "DEBIT");
        event.put("subType", "WITHDRAWAL");
        event.put("transactionId", UUID.randomUUID());
        return event;
    }

    private void syncTopUpDependents(Map<String, Object> event) {
        callRequiredInternalService(buildServiceUrl(transactionServiceBaseUrl, TRANSACTION_API_PREFIX, "/internal/sync/topup"), event);
        callOptionalInternalService(buildServiceUrl(rewardsServiceBaseUrl, REWARDS_API_PREFIX, "/internal/sync/topup"), event);
        callOptionalInternalService(buildServiceUrl(notificationServiceBaseUrl, NOTIFICATION_API_PREFIX, "/internal/sync/topup"), event);
    }

    private void syncRewardCreditDependents(Map<String, Object> event) {
        callOptionalInternalService(buildServiceUrl(transactionServiceBaseUrl, TRANSACTION_API_PREFIX, "/internal/sync/topup"), event);
        callOptionalInternalService(buildServiceUrl(notificationServiceBaseUrl, NOTIFICATION_API_PREFIX, "/internal/sync/topup"), event);
    }

    private void syncTransferDependents(Map<String, Object> event) {
        callRequiredInternalService(buildServiceUrl(transactionServiceBaseUrl, TRANSACTION_API_PREFIX, "/internal/sync/transfer"), event);
        callOptionalInternalService(buildServiceUrl(rewardsServiceBaseUrl, REWARDS_API_PREFIX, "/internal/sync/transfer"), event);
        callOptionalInternalService(buildServiceUrl(notificationServiceBaseUrl, NOTIFICATION_API_PREFIX, "/internal/sync/transfer"), event);
    }

    private void syncWithdrawDependents(Map<String, Object> event) {
        callRequiredInternalService(buildServiceUrl(transactionServiceBaseUrl, TRANSACTION_API_PREFIX, "/internal/sync/withdraw"), event);
        callOptionalInternalService(buildServiceUrl(notificationServiceBaseUrl, NOTIFICATION_API_PREFIX, "/internal/sync/withdraw"), event);
    }

    private void callRequiredInternalService(String url, Map<String, Object> event) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(INTERNAL_SECRET_HEADER, INTERNAL_SECRET_VALUE);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(event, headers);
        log.info("Initiating internal sync call to: {}", url);
        try {
            Map<?, ?> response = restTemplate.postForObject(url, entity, Map.class);
            log.info("Internal sync call to {} SUCCESS. Response: {}", url, response);
        } catch (Exception e) {
            log.error("Internal sync call to {} FAILED. Error: {}", url, e.getMessage());
            throw new RuntimeException("Dependent service sync failed for " + url + ": " + e.getMessage(), e);
        }
    }

    private void callOptionalInternalService(String url, Map<String, Object> event) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(INTERNAL_SECRET_HEADER, INTERNAL_SECRET_VALUE);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(event, headers);
        try {
            Map<?, ?> response = restTemplate.postForObject(url, entity, Map.class);
            log.info("Optional internal sync call to {} SUCCESS. Response: {}", url, response);
        } catch (Exception e) {
            log.warn("Optional internal sync call to {} FAILED. Kafka event will remain as backup. Error: {}", url, e.getMessage());
        }
    }

    private void publishWalletEvent(String topic, Map<String, Object> event, String operation) {
        try {
            kafkaTemplate.send(topic, event);
            System.out.println("DEBUG: Kafka event sent for " + operation + " " + event);
        } catch (Exception e) {
            System.err.println("Kafka unavailable: " + e.getMessage());
        }
    }

    private void runAfterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
            return;
        }
        action.run();
    }

    private void verifyUserExists(UUID userId) {
        String url = buildServiceUrl(authServiceBaseUrl, "/api/auth/internal/users", "/" + userId);
        HttpHeaders headers = new HttpHeaders();
        headers.set(INTERNAL_SECRET_HEADER, INTERNAL_SECRET_VALUE);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        
        log.info("Verifying existence of target user: {} via {}", userId, url);
        try {
            restTemplate.exchange(url, org.springframework.http.HttpMethod.GET, entity, Map.class);
            log.info("User {} verified successfully.", userId);
        } catch (org.springframework.web.client.HttpClientErrorException.NotFound e) {
            log.error("User {} not found in auth-service.", userId);
            throw new RuntimeException("Enter correct user ID. The recipient user ID does not exist in the system.");
        } catch (Exception e) {
            log.error("Failed to verify user {}: {}", userId, e.getMessage());
            throw new RuntimeException("Unable to verify the recipient user ID right now. Please try again after the services are running.");
        }
    }

    private String buildServiceUrl(String baseUrl, String apiPrefix, String endpointPath) {
        String normalizedBaseUrl = baseUrl == null ? "" : baseUrl.trim();
        if (normalizedBaseUrl.endsWith("/")) {
            normalizedBaseUrl = normalizedBaseUrl.substring(0, normalizedBaseUrl.length() - 1);
        }

        String normalizedApiPrefix = apiPrefix.startsWith("/") ? apiPrefix : "/" + apiPrefix;
        String normalizedEndpoint = endpointPath.startsWith("/") ? endpointPath : "/" + endpointPath;

        if (normalizedBaseUrl.endsWith(normalizedApiPrefix)) {
            return normalizedBaseUrl + normalizedEndpoint;
        }
        if (normalizedBaseUrl.endsWith("/api")) {
            return normalizedBaseUrl + normalizedApiPrefix.substring("/api".length()) + normalizedEndpoint;
        }
        return normalizedBaseUrl + normalizedApiPrefix + normalizedEndpoint;
    }
}
