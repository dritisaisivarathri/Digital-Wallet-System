package com.wallet.auth.service;

import com.wallet.auth.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenBlacklistServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private TokenBlacklistService tokenBlacklistService;

    @Test
    void isBlacklisted_ReturnsFalseWhenRedisIsUnavailable() {
        when(redisTemplate.hasKey("jwt:blacklist:test-token"))
                .thenThrow(new RedisConnectionFailureException("Redis unavailable"));

        assertFalse(tokenBlacklistService.isBlacklisted("test-token"));
    }
}
