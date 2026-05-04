package com.wallet.user.repository;

import com.wallet.user.entity.KycHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface KycHistoryRepository extends JpaRepository<KycHistory, UUID> {
    List<KycHistory> findByUserIdOrderByChangedAtDesc(UUID userId);
}
