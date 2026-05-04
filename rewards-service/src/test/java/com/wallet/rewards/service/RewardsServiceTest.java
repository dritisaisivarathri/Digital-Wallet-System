package com.wallet.rewards.service;

import com.wallet.rewards.entity.RewardCatalog;
import com.wallet.rewards.entity.RewardEventLog;
import com.wallet.rewards.entity.RewardPoints;
import com.wallet.rewards.dto.RewardRedeemResponse;
import com.wallet.rewards.repository.RewardCatalogRepository;
import com.wallet.rewards.repository.RewardEventLogRepository;
import com.wallet.rewards.repository.RewardPointsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RewardsServiceTest {

    @Mock
    private RewardPointsRepository pointsRepository;

    @Mock
    private RewardCatalogRepository catalogRepository;

    @Mock
    private RewardEventLogRepository rewardEventLogRepository;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private RewardsService rewardsService;

    private UUID userId;
    private RewardPoints rewardPoints;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        rewardPoints = new RewardPoints();
        rewardPoints.setUserId(userId);
        rewardPoints.setTotalPoints(100);
        rewardPoints.setTier("BRONZE");
        ReflectionTestUtils.setField(rewardsService, "walletServiceBaseUrl", "http://wallet-service:8083");
    }

    @Test
    void handleTopUp_AwardPoints() {
        Map<String, Object> event = new HashMap<>();
        event.put("userId", userId);
        event.put("amount", new BigDecimal("500.00")); // Should award 5 points
        event.put("transactionId", UUID.randomUUID());

        doNothing().when(pointsRepository).ensureUserExists(userId);
        when(pointsRepository.findByUserId(userId)).thenReturn(Optional.of(rewardPoints));
        when(rewardEventLogRepository.existsById(any(UUID.class))).thenReturn(false);

        rewardsService.handleTopUp(event);

        assertEquals(105, rewardPoints.getTotalPoints());
        verify(pointsRepository, times(1)).ensureUserExists(userId);
        verify(pointsRepository, times(1)).saveAndFlush(rewardPoints);
        verify(rewardEventLogRepository, times(1)).save(any(RewardEventLog.class));
    }

    @Test
    void handleTransfer_AwardPointsToSender() {
        Map<String, Object> event = new HashMap<>();
        event.put("fromUserId", userId);
        event.put("amount", new BigDecimal("1000.00")); // Should award 10 points
        event.put("transactionId", UUID.randomUUID());

        doNothing().when(pointsRepository).ensureUserExists(userId);
        when(pointsRepository.findByUserId(userId)).thenReturn(Optional.of(rewardPoints));
        when(rewardEventLogRepository.existsById(any(UUID.class))).thenReturn(false);

        rewardsService.handleTransfer(event);

        assertEquals(110, rewardPoints.getTotalPoints());
        verify(pointsRepository, times(1)).ensureUserExists(userId);
        verify(pointsRepository, times(1)).saveAndFlush(rewardPoints);
        verify(rewardEventLogRepository, times(1)).save(any(RewardEventLog.class));
    }

    @Test
    void syncTopUp_DuplicateTransactionIsIgnored() {
        UUID transactionId = UUID.randomUUID();
        Map<String, Object> event = new HashMap<>();
        event.put("userId", userId);
        event.put("amount", new BigDecimal("500.00"));
        event.put("transactionId", transactionId);

        when(rewardEventLogRepository.existsById(transactionId)).thenReturn(true);

        boolean applied = rewardsService.syncTopUp(event);

        assertFalse(applied);
        verify(pointsRepository, never()).saveAndFlush(any());
        verify(rewardEventLogRepository, never()).save(any());
    }

    @Test
    void redeemItem_Success() {
        UUID catalogId = UUID.randomUUID();
        RewardCatalog item = new RewardCatalog();
        item.setId(catalogId);
        item.setName("Gift Card");
        item.setCostInPoints(50);
        item.setStockQuantity(10);
        item.setRequiredTier("ALL");

        doNothing().when(pointsRepository).ensureUserExists(userId);
        when(pointsRepository.findByUserId(userId)).thenReturn(Optional.of(rewardPoints));
        when(catalogRepository.findById(catalogId)).thenReturn(Optional.of(item));
        when(pointsRepository.saveAndFlush(rewardPoints)).thenReturn(rewardPoints);

        RewardRedeemResponse result = rewardsService.redeemItem(userId, catalogId);

        assertEquals("Gift Card", result.getRewardName());
        assertEquals("VOUCHER", result.getRewardType());
        assertEquals(50, result.getRemainingPoints());
        assertEquals(50, rewardPoints.getTotalPoints());
        assertEquals(9, item.getStockQuantity());
        verify(pointsRepository, times(1)).saveAndFlush(rewardPoints);
        verify(catalogRepository, times(1)).save(item);
        verify(kafkaTemplate, times(1)).send(eq("reward.redeemed"), anyMap());
    }

    @Test
    void redeemItem_SilverUserCanRedeemBasicReward() {
        UUID catalogId = UUID.randomUUID();
        rewardPoints.setTier("SILVER");
        rewardPoints.setTotalPoints(151);
        RewardCatalog item = new RewardCatalog();
        item.setId(catalogId);
        item.setName("Flipkart Voucher");
        item.setDescription("$5 Flipkart Gift Card");
        item.setCostInPoints(30);
        item.setStockQuantity(100);
        item.setRequiredTier("BASIC");
        item.setRewardType("VOUCHER");

        doNothing().when(pointsRepository).ensureUserExists(userId);
        when(pointsRepository.findByUserId(userId)).thenReturn(Optional.of(rewardPoints));
        when(catalogRepository.findById(catalogId)).thenReturn(Optional.of(item));
        when(pointsRepository.saveAndFlush(rewardPoints)).thenReturn(rewardPoints);

        RewardRedeemResponse result = rewardsService.redeemItem(userId, catalogId);

        assertEquals(121, result.getRemainingPoints());
        assertEquals(99, result.getRemainingStock());
        verify(pointsRepository, times(1)).saveAndFlush(rewardPoints);
    }

    @Test
    void redeemItem_VoucherStillSucceedsWhenKafkaPublishFails() {
        UUID catalogId = UUID.randomUUID();
        rewardPoints.setTotalPoints(151);
        RewardCatalog item = new RewardCatalog();
        item.setId(catalogId);
        item.setName("Flipkart Voucher");
        item.setDescription("$5 Flipkart Gift Card");
        item.setCostInPoints(30);
        item.setStockQuantity(100);
        item.setRequiredTier("ALL");
        item.setRewardType("VOUCHER");

        doNothing().when(pointsRepository).ensureUserExists(userId);
        when(pointsRepository.findByUserId(userId)).thenReturn(Optional.of(rewardPoints));
        when(catalogRepository.findById(catalogId)).thenReturn(Optional.of(item));
        when(pointsRepository.saveAndFlush(rewardPoints)).thenReturn(rewardPoints);
        when(kafkaTemplate.send(eq("reward.redeemed"), anyMap())).thenThrow(new RuntimeException("Send failed"));

        RewardRedeemResponse result = rewardsService.redeemItem(userId, catalogId);

        assertEquals(121, rewardPoints.getTotalPoints());
        assertEquals(121, result.getRemainingPoints());
        assertEquals(99, result.getRemainingStock());
        verify(pointsRepository, times(1)).saveAndFlush(rewardPoints);
        verify(catalogRepository, times(1)).save(item);
    }

    @Test
    void redeemItem_InsufficientPoints() {
        UUID catalogId = UUID.randomUUID();
        RewardCatalog item = new RewardCatalog();
        item.setId(catalogId);
        item.setCostInPoints(200);
        item.setRequiredTier("ALL");

        doNothing().when(pointsRepository).ensureUserExists(userId);
        when(pointsRepository.findByUserId(userId)).thenReturn(Optional.of(rewardPoints));
        when(catalogRepository.findById(catalogId)).thenReturn(Optional.of(item));

        assertThrows(RuntimeException.class, () -> rewardsService.redeemItem(userId, catalogId));
    }

    @Test
    void getSummary_Success() {
        doNothing().when(pointsRepository).ensureUserExists(userId);
        when(pointsRepository.findByUserId(userId)).thenReturn(Optional.of(rewardPoints));

        RewardPoints result = rewardsService.getSummary(userId);

        assertEquals(userId, result.getUserId());
    }

    @Test
    void getCatalog_ReturnsItems() {
        when(catalogRepository.findAll()).thenReturn(java.util.List.of(new RewardCatalog()));

        assertEquals(1, rewardsService.getCatalog().size());
    }

    @Test
    void redeemItem_TierTooLow() {
        UUID catalogId = UUID.randomUUID();
        rewardPoints.setTier("SILVER");
        rewardPoints.setTotalPoints(5000);
        RewardCatalog item = new RewardCatalog();
        item.setRequiredTier("PLATINUM");
        item.setCostInPoints(100);
        item.setStockQuantity(1);

        doNothing().when(pointsRepository).ensureUserExists(userId);
        when(pointsRepository.findByUserId(userId)).thenReturn(Optional.of(rewardPoints));
        when(catalogRepository.findById(catalogId)).thenReturn(Optional.of(item));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> rewardsService.redeemItem(userId, catalogId));
        assertEquals("Tier too low to redeem this item", ex.getMessage());
    }

    @Test
    void redeemItem_OutOfStock() {
        UUID catalogId = UUID.randomUUID();
        rewardPoints.setTier("GOLD");
        rewardPoints.setTotalPoints(500);
        RewardCatalog item = new RewardCatalog();
        item.setRequiredTier("ALL");
        item.setCostInPoints(100);
        item.setStockQuantity(0);

        doNothing().when(pointsRepository).ensureUserExists(userId);
        when(pointsRepository.findByUserId(userId)).thenReturn(Optional.of(rewardPoints));
        when(catalogRepository.findById(catalogId)).thenReturn(Optional.of(item));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> rewardsService.redeemItem(userId, catalogId));
        assertEquals("Item out of stock", ex.getMessage());
    }

    @Test
    void createCatalogItem_Success() {
        RewardCatalog item = new RewardCatalog();
        item.setName("Coffee");
        when(catalogRepository.save(item)).thenReturn(item);

        assertEquals(item, rewardsService.createCatalogItem(item));
    }

    @Test
    void redeemItem_CashbackCreditsWalletAndReturnsBalance() {
        UUID catalogId = UUID.randomUUID();
        RewardCatalog item = new RewardCatalog();
        item.setId(catalogId);
        item.setName("Cashback Rs 100");
        item.setDescription("Direct wallet cashback");
        item.setRewardType("CASHBACK");
        item.setCashbackAmount(new BigDecimal("100.00"));
        item.setCostInPoints(50);
        item.setStockQuantity(10);
        item.setRequiredTier("ALL");

        Map<String, Object> walletResponse = new HashMap<>();
        walletResponse.put("balance", new BigDecimal("450.00"));

        doNothing().when(pointsRepository).ensureUserExists(userId);
        when(pointsRepository.findByUserId(userId)).thenReturn(Optional.of(rewardPoints));
        when(catalogRepository.findById(catalogId)).thenReturn(Optional.of(item));
        when(pointsRepository.saveAndFlush(rewardPoints)).thenReturn(rewardPoints);
        when(restTemplate.getForEntity("http://wallet-service:8083/api/wallet/health-check", String.class))
                .thenReturn(ResponseEntity.ok("Wallet Controller is reachable"));
        when(restTemplate.postForObject(eq("http://wallet-service:8083/api/wallet/internal/credit"), any(), eq(Map.class)))
                .thenReturn(walletResponse);

        RewardRedeemResponse result = rewardsService.redeemItem(userId, catalogId);

        assertEquals("CASHBACK", result.getRewardType());
        assertEquals(new BigDecimal("100.00"), result.getCashbackCredited());
        assertEquals(new BigDecimal("450.00"), result.getWalletBalance());
        assertEquals(50, result.getRemainingPoints());
        assertEquals(9, result.getRemainingStock());
        verify(pointsRepository, times(1)).saveAndFlush(rewardPoints);
        verify(catalogRepository, times(1)).save(item);
        verify(kafkaTemplate, times(1)).send(eq("reward.redeemed"), anyMap());
    }

    @Test
    void redeemItem_CashbackUsesNormalizedWalletBaseUrl() {
        UUID catalogId = UUID.randomUUID();
        RewardCatalog item = new RewardCatalog();
        item.setId(catalogId);
        item.setName("Cashback Rs 100");
        item.setDescription("Direct wallet cashback");
        item.setRewardType("CASHBACK");
        item.setCashbackAmount(new BigDecimal("100.00"));
        item.setCostInPoints(50);
        item.setStockQuantity(10);
        item.setRequiredTier("ALL");

        Map<String, Object> walletResponse = new HashMap<>();
        walletResponse.put("balance", new BigDecimal("450.00"));

        ReflectionTestUtils.setField(rewardsService, "walletServiceBaseUrl", "http://wallet-service:8083/api/wallet");

        doNothing().when(pointsRepository).ensureUserExists(userId);
        when(pointsRepository.findByUserId(userId)).thenReturn(Optional.of(rewardPoints));
        when(catalogRepository.findById(catalogId)).thenReturn(Optional.of(item));
        when(pointsRepository.saveAndFlush(rewardPoints)).thenReturn(rewardPoints);
        when(restTemplate.getForEntity("http://wallet-service:8083/api/wallet/health-check", String.class))
                .thenReturn(ResponseEntity.ok("Wallet Controller is reachable"));
        when(restTemplate.postForObject(eq("http://wallet-service:8083/api/wallet/internal/credit"), any(), eq(Map.class)))
                .thenReturn(walletResponse);

        RewardRedeemResponse result = rewardsService.redeemItem(userId, catalogId);

        assertEquals(new BigDecimal("450.00"), result.getWalletBalance());
        verify(restTemplate).postForObject(eq("http://wallet-service:8083/api/wallet/internal/credit"), any(), eq(Map.class));
    }
}
