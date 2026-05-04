package com.wallet.notification.repository;

import com.wallet.notification.entity.NotificationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

import java.util.List;

public interface NotificationRepository extends JpaRepository<NotificationHistory, UUID> {
    List<NotificationHistory> findByUserIdOrderBySentAtDesc(UUID userId);
    List<NotificationHistory> findAllByOrderBySentAtDesc();
    boolean existsByUserIdAndTopicAndReferenceId(UUID userId, String topic, String referenceId);
}
