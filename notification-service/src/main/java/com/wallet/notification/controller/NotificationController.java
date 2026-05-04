package com.wallet.notification.controller;

import com.wallet.notification.entity.NotificationHistory;
import com.wallet.notification.repository.NotificationRepository;
import com.wallet.common.dto.KycNotificationEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationRepository repository;

    @Autowired
    private com.wallet.notification.service.NotificationService service;

    @GetMapping
    public ResponseEntity<List<NotificationHistory>> getAll() {
        return ResponseEntity.ok(repository.findAllByOrderBySentAtDesc()); // Super admin view
    }

    @GetMapping("/{userId}")
    public ResponseEntity<List<NotificationHistory>> getByUserId(@PathVariable java.util.UUID userId) {
        return ResponseEntity.ok(service.getNotifications(userId));
    }

    @PostMapping("/internal/sync/topup")
    public ResponseEntity<java.util.Map<String, Object>> syncTopUp(@RequestBody java.util.Map<String, Object> event) {
        return ResponseEntity.ok(java.util.Map.of("applied", service.syncTopUp(event)));
    }

    @PostMapping("/internal/sync/transfer")
    public ResponseEntity<java.util.Map<String, Object>> syncTransfer(@RequestBody java.util.Map<String, Object> event) {
        return ResponseEntity.ok(java.util.Map.of("applied", service.syncTransfer(event)));
    }

    @PostMapping("/internal/sync/withdraw")
    public ResponseEntity<java.util.Map<String, Object>> syncWithdraw(@RequestBody java.util.Map<String, Object> event) {
        return ResponseEntity.ok(java.util.Map.of("applied", service.syncWithdraw(event)));
    }

    @PostMapping("/internal/sync/reward-redeemed")
    public ResponseEntity<java.util.Map<String, Object>> syncRewardRedeemed(@RequestBody java.util.Map<String, Object> event) {
        return ResponseEntity.ok(java.util.Map.of("applied", service.syncRewardRedeemed(event)));
    }

    @PostMapping("/internal/sync/kyc-status")
    public ResponseEntity<java.util.Map<String, Object>> syncKycStatus(@RequestBody KycNotificationEvent event) {
        service.processKycStatusUpdate(event);
        return ResponseEntity.ok(java.util.Map.of("applied", true));
    }

    @PostMapping("/send")
    public ResponseEntity<String> sendManual(@jakarta.validation.Valid @RequestBody com.wallet.notification.dto.ManualNotificationRequest request) {
        service.sendManualNotification(request.getUserId(), request.getMessage(), request.getTopic());
        return ResponseEntity.ok("Manual notification triggered and logged successfully");
    }
}
