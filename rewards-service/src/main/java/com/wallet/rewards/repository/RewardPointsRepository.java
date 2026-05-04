package com.wallet.rewards.repository;

import com.wallet.rewards.entity.RewardPoints;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

public interface RewardPointsRepository extends JpaRepository<RewardPoints, UUID> {
    Optional<RewardPoints> findByUserId(UUID userId);

    @Transactional
    @Modifying
    @Query(value = "INSERT INTO reward_points (id, user_id, total_points, tier, last_updated) " +
                   "VALUES (gen_random_uuid(), :userId, 0, 'SILVER', NOW()) " +
                   "ON CONFLICT (user_id) DO NOTHING", nativeQuery = true)
    void ensureUserExists(@Param("userId") UUID userId);

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE reward_points " +
                   "SET total_points = total_points - :points, " +
                   "tier = CASE " +
                   "  WHEN total_points - :points >= 5000 THEN 'PLATINUM' " +
                   "  WHEN total_points - :points >= 1000 THEN 'GOLD' " +
                   "  ELSE 'SILVER' " +
                   "END, " +
                   "last_updated = NOW() " +
                   "WHERE user_id = :userId AND total_points >= :points", nativeQuery = true)
    int deductPointsForUser(@Param("userId") UUID userId, @Param("points") int points);
}
