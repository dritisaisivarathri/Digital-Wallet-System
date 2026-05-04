package com.wallet.rewards.repository;

import com.wallet.rewards.entity.RewardEventLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RewardEventLogRepository extends JpaRepository<RewardEventLog, UUID> {
}
