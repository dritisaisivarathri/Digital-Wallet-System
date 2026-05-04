package com.wallet.notification.dto;

import java.util.UUID;

public class ManualNotificationRequest {
    private UUID userId;
    private String message;
    private String topic;

    public ManualNotificationRequest() {}

    public ManualNotificationRequest(UUID userId, String message, String topic) {
        this.userId = userId;
        this.message = message;
        this.topic = topic;
    }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }
}
