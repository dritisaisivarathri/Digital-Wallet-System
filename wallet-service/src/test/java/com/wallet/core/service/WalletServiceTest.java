package com.wallet.core.service;

import com.wallet.core.dto.TopUpRequest;
import com.wallet.core.dto.TransferRequest;
import com.wallet.core.entity.WalletAccount;
import com.wallet.core.repository.WalletAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class WalletServiceTest {

    @Mock
    private WalletAccountRepository repository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private WalletService walletService;

    private UUID userId;
    private WalletAccount walletAccount;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        walletAccount = new WalletAccount();
        walletAccount.setUserId(userId);
        walletAccount.setCachedBalance(new BigDecimal("100.00"));
        walletAccount.setStatus("ACTIVE");

        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        ReflectionTestUtils.setField(walletService, "transactionServiceBaseUrl", "http://transaction-service:8084");
        ReflectionTestUtils.setField(walletService, "rewardsServiceBaseUrl", "http://rewards-service:8085");
        ReflectionTestUtils.setField(walletService, "notificationServiceBaseUrl", "http://notification-service:8086");
    }

    @Test
    void getBalance_FromRedis() {
        when(valueOperations.get(anyString())).thenReturn("150.00");

        BigDecimal balance = walletService.getBalance(userId);

        assertEquals(new BigDecimal("150.00"), balance);
        verify(repository, never()).findByUserId(any());
    }

    @Test
    void getBalance_FromDb() {
        when(valueOperations.get(anyString())).thenReturn(null);
        when(repository.findByUserId(userId)).thenReturn(Optional.of(walletAccount));

        BigDecimal balance = walletService.getBalance(userId);

        assertEquals(new BigDecimal("100.00"), balance);
        verify(valueOperations, times(1)).set(anyString(), anyString());
    }

    @Test
    void topUp_Success() {
        TopUpRequest request = new TopUpRequest(userId, new BigDecimal("50.00"), "UPI");
        when(repository.findByUserId(userId)).thenReturn(Optional.of(walletAccount));
        when(restTemplate.postForObject(contains("/api/transactions/internal/sync/topup"), any(), eq(Map.class)))
                .thenReturn(Map.of("applied", true));
        when(restTemplate.postForObject(contains("/api/rewards/internal/sync/topup"), any(), eq(Map.class)))
                .thenReturn(Map.of("applied", true));
        when(restTemplate.postForObject(contains("/api/notifications/internal/sync/topup"), any(), eq(Map.class)))
                .thenReturn(Map.of("applied", true));

        String result = walletService.topUp(userId, request);

        assertEquals("Top-up successful. Rs 50 added to your wallet using UPI.", result);
        assertEquals(new BigDecimal("150.00"), walletAccount.getCachedBalance());
        verify(repository, times(1)).saveAndFlush(walletAccount);
        verify(restTemplate, times(1)).postForObject(contains("/api/transactions/internal/sync/topup"), any(), eq(Map.class));
        verify(restTemplate, times(1)).postForObject(contains("/api/rewards/internal/sync/topup"), any(), eq(Map.class));
        verify(restTemplate, times(1)).postForObject(contains("/api/notifications/internal/sync/topup"), any(), eq(Map.class));
        verify(valueOperations, times(1)).set(anyString(), eq("150.00"));
        verify(kafkaTemplate, times(1)).send(eq("wallet.topup.success"), anyMap());
    }

    @Test
    void transfer_Success() {
        UUID targetUserId = UUID.randomUUID();
        WalletAccount targetAccount = new WalletAccount();
        targetAccount.setUserId(targetUserId);
        targetAccount.setCachedBalance(BigDecimal.ZERO);

        TransferRequest request = new TransferRequest(targetUserId, new BigDecimal("40.00"), "Dinner");
        
        when(repository.findByUserId(userId)).thenReturn(Optional.of(walletAccount));
        when(repository.findByUserId(targetUserId)).thenReturn(Optional.of(targetAccount));
        when(restTemplate.postForObject(contains("/api/transactions/internal/sync/transfer"), any(), eq(Map.class)))
                .thenReturn(Map.of("applied", true));
        when(restTemplate.postForObject(contains("/api/rewards/internal/sync/transfer"), any(), eq(Map.class)))
                .thenReturn(Map.of("applied", true));
        when(restTemplate.postForObject(contains("/api/notifications/internal/sync/transfer"), any(), eq(Map.class)))
                .thenReturn(Map.of("applied", true));

        String result = walletService.transfer(userId, request);

        assertEquals("Funds sent successfully. Rs 40 was sent to user " + targetUserId + ".", result);
        assertEquals(new BigDecimal("60.00"), walletAccount.getCachedBalance());
        assertEquals(new BigDecimal("40.00"), targetAccount.getCachedBalance());
        verify(repository, times(2)).saveAndFlush(any());
        verify(restTemplate, times(1)).postForObject(contains("/api/transactions/internal/sync/transfer"), any(), eq(Map.class));
        verify(restTemplate, times(1)).postForObject(contains("/api/rewards/internal/sync/transfer"), any(), eq(Map.class));
        verify(restTemplate, times(1)).postForObject(contains("/api/notifications/internal/sync/transfer"), any(), eq(Map.class));
        verify(kafkaTemplate, times(1)).send(eq("wallet.transfer.completed"), anyMap());
    }

    @Test
    void transfer_InsufficientFunds() {
        UUID targetUserId = UUID.randomUUID();
        TransferRequest request = new TransferRequest(targetUserId, new BigDecimal("200.00"), "Rent");
        
        when(repository.findByUserId(userId)).thenReturn(Optional.of(walletAccount));

        assertThrows(RuntimeException.class, () -> walletService.transfer(userId, request));
    }

    @Test
    void transfer_ToSelf() {
        TransferRequest request = new TransferRequest(userId, new BigDecimal("10.00"), "Self");
        assertThrows(RuntimeException.class, () -> walletService.transfer(userId, request));
    }

    @Test
    void withdraw_Success() {
        com.wallet.core.dto.WithdrawRequest request = new com.wallet.core.dto.WithdrawRequest(new BigDecimal("30.00"));
        when(repository.findByUserId(userId)).thenReturn(Optional.of(walletAccount));

        String result = walletService.withdraw(userId, request);

        assertEquals("Withdrawal successful. Rs 30 has been debited from your wallet.", result);
        assertEquals(new BigDecimal("70.00"), walletAccount.getCachedBalance());
        verify(repository, times(1)).saveAndFlush(walletAccount);
        verify(kafkaTemplate, times(1)).send(eq("wallet.withdraw.success"), anyMap());
    }

    @Test
    void withdraw_InsufficientFunds() {
        com.wallet.core.dto.WithdrawRequest request = new com.wallet.core.dto.WithdrawRequest(new BigDecimal("500.00"));
        when(repository.findByUserId(userId)).thenReturn(Optional.of(walletAccount));

        assertThrows(RuntimeException.class, () -> walletService.withdraw(userId, request));
    }

    @Test
    void creditFromReward_Success() {
        when(repository.findByUserId(userId)).thenReturn(Optional.of(walletAccount));
        when(restTemplate.postForObject(contains("/api/transactions/internal/sync/topup"), any(), eq(Map.class)))
                .thenReturn(Map.of("applied", true));
        when(restTemplate.postForObject(contains("/api/notifications/internal/sync/topup"), any(), eq(Map.class)))
                .thenReturn(Map.of("applied", true));

        BigDecimal updatedBalance = walletService.creditFromReward(
                userId,
                new BigDecimal("100.00"),
                "REWARD_CASHBACK",
                "Cashback credited for reward redemption"
        );

        assertEquals(new BigDecimal("200.00"), updatedBalance);
        assertEquals(new BigDecimal("200.00"), walletAccount.getCachedBalance());
        verify(repository, times(1)).saveAndFlush(walletAccount);
        verify(restTemplate, times(1)).postForObject(contains("/api/transactions/internal/sync/topup"), any(), eq(Map.class));
        verify(restTemplate, never()).postForObject(contains("/api/rewards/internal/sync/topup"), any(), eq(Map.class));
        verify(restTemplate, times(1)).postForObject(contains("/api/notifications/internal/sync/topup"), any(), eq(Map.class));
        verify(kafkaTemplate, times(1)).send(eq("wallet.topup.success"), argThat((Map<String, Object> event) ->
                "REWARD_CASHBACK".equals(event.get("subType"))
                        && "Cashback credited for reward redemption".equals(event.get("notes"))
        ));
    }

    @Test
    void creditFromReward_TransactionSyncFailureStillCreditsWallet() {
        when(repository.findByUserId(userId)).thenReturn(Optional.of(walletAccount));
        when(restTemplate.postForObject(contains("/api/transactions/internal/sync/topup"), any(), eq(Map.class)))
                .thenThrow(new RuntimeException("Read timed out"));
        when(restTemplate.postForObject(contains("/api/notifications/internal/sync/topup"), any(), eq(Map.class)))
                .thenReturn(Map.of("applied", true));

        BigDecimal updatedBalance = walletService.creditFromReward(
                userId,
                new BigDecimal("100.00"),
                "REWARD_CASHBACK",
                "Cashback credited for reward redemption"
        );

        assertEquals(new BigDecimal("200.00"), updatedBalance);
        assertEquals(new BigDecimal("200.00"), walletAccount.getCachedBalance());
        verify(repository, times(1)).saveAndFlush(walletAccount);
        verify(restTemplate, never()).postForObject(contains("/api/rewards/internal/sync/topup"), any(), eq(Map.class));
        verify(kafkaTemplate, times(1)).send(eq("wallet.topup.success"), anyMap());
    }
}
