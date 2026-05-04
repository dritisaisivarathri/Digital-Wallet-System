package com.wallet.notification.service;

import com.wallet.notification.entity.NotificationHistory;
import com.wallet.notification.repository.NotificationRepository;
import com.wallet.common.dto.KycNotificationEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private NotificationService notificationService;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
    }

    @Test
    void handleTopUp_Success() {
        Map<String, Object> event = new HashMap<>();
        event.put("userId", userId);
        event.put("amount", "100.00");

        notificationService.handleTopUp(event);

        verify(notificationRepository, times(1)).save(any(NotificationHistory.class));
    }

    @Test
    void handleTransfer_Success() {
        UUID fromUserId = UUID.randomUUID();
        UUID toUserId = UUID.randomUUID();
        Map<String, Object> event = new HashMap<>();
        event.put("fromUserId", fromUserId);
        event.put("toUserId", toUserId);
        event.put("amount", "50.00");

        notificationService.handleTransfer(event);

        // Should save two notifications (one for sender, one for receiver)
        verify(notificationRepository, times(2)).save(any(NotificationHistory.class));
    }

    @Test
    void handleTopUp_Exception_Logged() {
        Map<String, Object> event = new HashMap<>();
        // Missing userId will cause exception
        event.put("amount", "100.00");

        notificationService.handleTopUp(event);

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void handleKycStatusUpdate_Approved_SendsEmailAndSavesHistory() {
        KycNotificationEvent event = new KycNotificationEvent(userId, "user@test.com", "admin@test.com", "APPROVED", null, "KYC_UPDATE");

        notificationService.handleKycStatusUpdate(event);

        verify(emailService).sendEmail(eq("user@test.com"), eq("admin@test.com"), eq("KYC Status Update: APPROVED"),
                contains("You can now perform full transactions."));
        verify(notificationRepository).save(any(NotificationHistory.class));
    }

    @Test
    void handleKycStatusUpdate_Rejected_UsesReason() {
        KycNotificationEvent event = new KycNotificationEvent(userId, "user@test.com", "admin@test.com", "REJECTED", "Missing docs", "KYC_UPDATE");

        notificationService.handleKycStatusUpdate(event);

        verify(emailService).sendEmail(eq("user@test.com"), eq("admin@test.com"), eq("KYC Status Update: REJECTED"),
                contains("Reason: Missing docs"));
        verify(notificationRepository).save(any(NotificationHistory.class));
    }

    @Test
    void handleKycStatusUpdate_EmailFailure_Rethrows() {
        KycNotificationEvent event = new KycNotificationEvent(userId, "user@test.com", "admin@test.com", "APPROVED", null, "KYC_UPDATE");
        doThrow(new RuntimeException("mail down")).when(emailService).sendEmail(any(), any(), any(), any());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> notificationService.handleKycStatusUpdate(event));

        assertEquals("mail down", ex.getMessage());
    }

    @Test
    void getNotifications_ReturnsRepositoryValues() {
        when(notificationRepository.findByUserIdOrderBySentAtDesc(userId)).thenReturn(java.util.List.of(new NotificationHistory()));

        assertEquals(1, notificationService.getNotifications(userId).size());
        verify(notificationRepository).findByUserIdOrderBySentAtDesc(userId);
    }

    @Test
    void sendManualNotification_DefaultsTopicWhenNull() {
        notificationService.sendManualNotification(userId, "Hello", null);

        verify(notificationRepository).save(argThat(history ->
                userId.equals(history.getUserId())
                        && "Hello".equals(history.getMessage())
                        && "manual.alert".equals(history.getTopic())
                        && history.getSentAt() != null));
    }
}
