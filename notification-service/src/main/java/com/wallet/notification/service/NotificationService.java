package com.wallet.notification.service;

import com.wallet.common.dto.KycNotificationEvent;
import com.wallet.notification.entity.NotificationHistory;
import com.wallet.notification.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;

@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${services.user-service.internal-base-url:http://localhost:8082}")
    private String userServiceBaseUrl;

    @KafkaListener(topics = "wallet.topup.success", groupId = "notification-group")
    public void handleTopUp(Map<String, Object> event) {
        logger.info("Notification Service received Top-Up: {}", event);
        try {
            syncTopUp(event);
        } catch (Exception e) {
            logger.error("Error processing topup notification", e);
        }
    }

    @KafkaListener(topics = "kyc.status.updated", groupId = "notification-group")
    public void handleKycStatusUpdate(KycNotificationEvent event) {
        logger.info("RECEIVED KYC EVENT - UserID: {}, Status: {}, Email: {}",
                event.getUserId(), event.getStatus(), event.getUserEmail());
        processKycStatusUpdate(event);
    }

    public void processKycStatusUpdate(KycNotificationEvent event) {
        try {
            UUID userId = event.getUserId();
            String status = event.getStatus();
            String reason = event.getReason();
            
            // Exact wording as requested by user
            String message = "";
            if ("APPROVED".equals(status)) {
                message = "Your KYC approved you can perform transactions";
            } else if ("REJECTED".equals(status)) {
                message = "Your KYC rejected upload documents properly";
                if (reason != null && !reason.isBlank()) {
                    message += ". Reason: " + reason;
                }
            } else {
                message = "Your KYC status has been updated to: " + status;
            }

            String userEmail = event.getUserEmail();
            String adminEmail = event.getAdminEmail();
            
            if (userEmail == null || userEmail.isBlank()) {
                logger.error("ABORTING EMAIL: No recipient email for user {}", userId);
                return;
            }

            String referenceId = "kyc-" + userId + "-" + status;

            // Save in-app records before email so the UI still updates if SMTP is unavailable.
            saveNotificationIfMissing(userId, "kyc.status.updated", message, "EMAIL", referenceId);
            saveNotificationIfMissing(null, "kyc.status.updated",
                    "KYC " + status + " for " + userEmail + ". " + message,
                    "IN_APP",
                    referenceId + "-admin");

            logger.info("SENDING KYC EMAIL - To: {}, Subject: KYC Status Update, Msg: {}", userEmail, message);
            try {
                emailService.sendEmail(userEmail, adminEmail, "KYC Status Update: " + status, message);
            } catch (Exception mailError) {
                logger.error("KYC email failed for user {} but in-app notification was saved: {}",
                        userId, mailError.getMessage());
            }

            logger.info("KYC NOTIFICATION SUCCESS for user {}", userId);

        } catch (Exception e) {
             logger.error("KYC NOTIFICATION FAILURE for user {}: {}", event.getUserId(), e.getMessage());
             throw e; // Rethrow to trigger Kafka retry
        }
    }

    @KafkaListener(topics = "wallet.transfer.completed", groupId = "notification-group")
    public void handleTransfer(Map<String, Object> event) {
        logger.info("Notification Service received Transfer: {}", event);
        try {
            syncTransfer(event);
        } catch (Exception e) {
            logger.error("Error processing transfer notification", e);
        }
    }

    @KafkaListener(topics = "wallet.withdraw.success", groupId = "notification-group")
    public void handleWithdraw(Map<String, Object> event) {
        logger.info("Notification Service received Withdrawal: {}", event);
        try {
            syncWithdraw(event);
        } catch (Exception e) {
            logger.error("Error processing withdrawal notification", e);
        }
    }

    @KafkaListener(topics = "reward.redeemed", groupId = "notification-group")
    public void handleRewardRedeemed(Map<String, Object> event) {
        try {
            syncRewardRedeemed(event);
        } catch (Exception e) {
            System.err.println("NOTIFICATION_ERROR: Critical failure in handleRewardRedeemed: " + e.getMessage());
            e.printStackTrace();
            logger.error("Error processing reward redemption notification", e);
        }
    }

    @KafkaListener(topics = "campaign.created", groupId = "notification-group")
    public void handleCampaignCreated(Map<String, Object> event) {
        logger.info("Notification Service received Campaign Created Event: {}", event);
        try {
            String campaignName = String.valueOf(event.getOrDefault("campaignName", "Campaign"));
            String targetTier = String.valueOf(event.getOrDefault("targetTier", "ALL"));
            String status = String.valueOf(event.getOrDefault("status", "ACTIVE"));

            NotificationHistory history = new NotificationHistory();
            history.setUserId(null); // Admin/system notification
            history.setTopic("campaign.created");
            history.setMessage("Campaign created: " + campaignName + " | Tier: " + targetTier + " | Status: " + status);
            history.setType("IN_APP");
            notificationRepository.save(history);
        } catch (Exception e) {
            logger.error("Error processing campaign created notification", e);
        }
    }

    @KafkaListener(topics = "reward.catalog.created", groupId = "notification-group")
    public void handleRewardCatalogCreated(Map<String, Object> event) {
        logger.info("Notification Service received Reward Catalog Created Event: {}", event);
        try {
            String rewardName = String.valueOf(event.getOrDefault("rewardName", "Reward"));
            String rewardType = String.valueOf(event.getOrDefault("rewardType", "VOUCHER"));
            String costInPoints = String.valueOf(event.getOrDefault("costInPoints", "0"));
            String stockQuantity = String.valueOf(event.getOrDefault("stockQuantity", "0"));

            NotificationHistory history = new NotificationHistory();
            history.setUserId(null); // Admin/system notification
            history.setTopic("reward.catalog.created");
            history.setMessage("Reward added: " + rewardName + " | Type: " + rewardType + " | Cost: " + costInPoints + " pts | Stock: " + stockQuantity);
            history.setType("IN_APP");
            notificationRepository.save(history);
        } catch (Exception e) {
            logger.error("Error processing reward catalog created notification", e);
        }
    }

    public java.util.List<NotificationHistory> getNotifications(UUID userId) {
        backfillKycNotificationIfMissing(userId);
        return notificationRepository.findByUserIdOrderBySentAtDesc(userId);
    }

    public boolean syncTopUp(Map<String, Object> event) {
        UUID userId = UUID.fromString(String.valueOf(event.get("userId")));
        String amount = String.valueOf(event.get("amount"));
        String subType = String.valueOf(event.getOrDefault("subType", "TOPUP")).toUpperCase();
        String referenceId = stringOrNull(event.get("transactionId"));
        String message = "REWARD_CASHBACK".equals(subType)
                ? "Reward cashback credited successfully. Rs " + amount + " has been added to your wallet."
                : "Top-up successful. Rs " + amount + " has been added to your wallet.";
        return saveNotificationIfMissing(userId, "wallet.topup.success", message, "IN_APP", referenceId);
    }

    public boolean syncTransfer(Map<String, Object> event) {
        UUID fromUserId = UUID.fromString(String.valueOf(event.get("fromUserId")));
        UUID toUserId = UUID.fromString(String.valueOf(event.get("toUserId")));
        String amount = String.valueOf(event.get("amount"));
        String referenceId = stringOrNull(event.get("transactionId"));
        boolean senderSaved = saveNotificationIfMissing(
                fromUserId,
                "wallet.transfer.completed",
                "Funds sent successfully. Rs " + amount + " was sent to user " + toUserId + ".",
                "IN_APP",
                referenceId
        );
        boolean receiverSaved = saveNotificationIfMissing(
                toUserId,
                "wallet.transfer.completed",
                "Funds received successfully. Rs " + amount + " was received from user " + fromUserId + ".",
                "IN_APP",
                referenceId
        );
        return senderSaved || receiverSaved;
    }

    public boolean syncWithdraw(Map<String, Object> event) {
        UUID userId = UUID.fromString(String.valueOf(event.get("userId")));
        String amount = String.valueOf(event.get("amount"));
        String referenceId = stringOrNull(event.get("transactionId"));
        return saveNotificationIfMissing(
                userId,
                "wallet.withdraw.success",
                "Withdrawal successful. Rs " + amount + " has been debited from your wallet.",
                "IN_APP",
                referenceId
        );
    }

    public boolean syncRewardRedeemed(Map<String, Object> event) {
        UUID userId = UUID.fromString(String.valueOf(event.get("userId")));
        String rewardName = String.valueOf(event.getOrDefault("rewardName", "Reward"));
        String rewardType = String.valueOf(event.getOrDefault("rewardType", "VOUCHER")).toUpperCase();
        String cashback = String.valueOf(event.getOrDefault("cashbackCredited", "0"));
        String referenceId = stringOrNull(event.get("eventId"));

        double cashbackVal = 0;
        if (cashback != null && !"null".equals(cashback)) {
            try {
                cashbackVal = Double.parseDouble(cashback);
            } catch (Exception e) {
                logger.warn("Unable to parse cashback value {}", cashback);
            }
        }

        String message = ("CASHBACK".equals(rewardType) || cashbackVal > 0)
                ? "Reward redeemed successfully. " + rewardName + " cashback of Rs "
                    + (cashbackVal > 0 ? String.valueOf(cashbackVal) : "100")
                    + " has been added to your wallet."
                : "Reward redeemed successfully. " + rewardName + " voucher is now available for you.";

        return saveNotificationIfMissing(userId, "reward.redeemed", message, "IN_APP", referenceId);
    }

    public void sendManualNotification(UUID userId, String message, String topic) {
        NotificationHistory history = new NotificationHistory();
        history.setUserId(userId);
        history.setMessage(message);
        history.setTopic(topic != null ? topic : "manual.alert");
        history.setSentAt(java.time.LocalDateTime.now());
        notificationRepository.save(history);

        logger.info("Manual Notification Saved: UserId={}, Message={}", userId, message);
        // In real app, would also call emailService.sendEmail(...) if user has email preference
    }

    private boolean saveNotificationIfMissing(UUID userId, String topic, String message, String type, String referenceId) {
        if (referenceId != null && notificationRepository.existsByUserIdAndTopicAndReferenceId(userId, topic, referenceId)) {
            logger.info("Skipping duplicate notification for user {} topic {} ref {}", userId, topic, referenceId);
            return false;
        }

        NotificationHistory history = new NotificationHistory();
        history.setUserId(userId);
        history.setTopic(topic);
        history.setMessage(message);
        history.setType(type);
        history.setReferenceId(referenceId);
        notificationRepository.saveAndFlush(history);
        return true;
    }

    private String stringOrNull(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() || "null".equalsIgnoreCase(text) ? null : text;
    }

    private void backfillKycNotificationIfMissing(UUID userId) {
        if (userId == null) {
            return;
        }

        try {
            String approvedReferenceId = "kyc-" + userId + "-APPROVED";
            String rejectedReferenceId = "kyc-" + userId + "-REJECTED";
            if (notificationRepository.existsByUserIdAndTopicAndReferenceId(userId, "kyc.status.updated", approvedReferenceId)
                    || notificationRepository.existsByUserIdAndTopicAndReferenceId(userId, "kyc.status.updated", rejectedReferenceId)) {
                return;
            }

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Internal-Gateway-Token", "DigitalWalletInternalSecret2026");
            java.util.Map<?, ?> kyc = restTemplate.exchange(
                    userServiceBaseUrl + "/api/users/internal/kyc/" + userId,
                    org.springframework.http.HttpMethod.GET,
                    new HttpEntity<>(headers),
                    java.util.Map.class
            ).getBody();

            if (kyc == null) {
                return;
            }

            String status = String.valueOf(kyc.get("status") == null ? "" : kyc.get("status")).trim().toUpperCase();
            if (!"APPROVED".equals(status) && !"REJECTED".equals(status)) {
                return;
            }

            String message = "APPROVED".equals(status)
                    ? "Your KYC approved you can perform transactions"
                    : "Your KYC rejected upload documents properly";
            String reason = String.valueOf(kyc.get("rejectionReason") == null ? "" : kyc.get("rejectionReason")).trim();
            if ("REJECTED".equals(status) && !reason.isBlank() && !"null".equalsIgnoreCase(reason)) {
                message += ". Reason: " + reason;
            }

            saveNotificationIfMissing(userId, "kyc.status.updated", message, "EMAIL", "kyc-" + userId + "-" + status);
        } catch (Exception e) {
            logger.warn("Unable to backfill KYC notification for user {}: {}", userId, e.getMessage());
        }
    }
}
