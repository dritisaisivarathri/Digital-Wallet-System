package com.wallet.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OtpServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private OtpService otpService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        ReflectionTestUtils.setField(otpService, "mailUsername", "noreply@wallet.com");
        ReflectionTestUtils.setField(otpService, "mailPassword", "mock-password");
        ReflectionTestUtils.setField(otpService, "mailFrom", "noreply@wallet.com");
    }

    @Test
    void generateAndSendOtp_Success() {
        String email = "test@example.com";
        
        otpService.generateAndSendOtp(email);

        verify(valueOperations).set(
                eq("otp:" + email),
                anyString(),
                eq(5L),
                eq(TimeUnit.MINUTES)
        );
        verify(redisTemplate).delete("otp_verified:" + email);
        verify(mailSender).send(any(org.springframework.mail.SimpleMailMessage.class));
    }

    @Test
    void verifyOtp_Success() {
        String email = "test@example.com";
        String code = "123456";
        when(valueOperations.get("otp:" + email)).thenReturn(code);

        boolean result = otpService.verifyOtp(email, code);

        assertTrue(result);
        verify(valueOperations).set("otp_verified:" + email, code, 5L, TimeUnit.MINUTES);
        verify(redisTemplate).delete("otp:" + email);
    }

    @Test
    void verifyOtp_IncorrectCode() {
        String email = "test@example.com";
        String code = "123456";
        when(valueOperations.get("otp:" + email)).thenReturn("654321");

        boolean result = otpService.verifyOtp(email, code);

        assertFalse(result);
        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    void verifyOtp_ExpiredOrMissing() {
        String email = "test@example.com";
        String code = "123456";
        when(valueOperations.get("otp:" + email)).thenReturn(null);

        boolean result = otpService.verifyOtp(email, code);

        assertFalse(result);
    }

    @Test
    void isOtpVerifiedForReset_MatchesStoredCode() {
        String email = "test@example.com";
        when(valueOperations.get("otp_verified:" + email)).thenReturn("654321");

        assertTrue(otpService.isOtpVerifiedForReset(email, "654321"));
        assertFalse(otpService.isOtpVerifiedForReset(email, "123456"));
    }

    @Test
    void generateAndSendOtp_FailsWhenSmtpCredentialsMissing() {
        ReflectionTestUtils.setField(otpService, "mailUsername", "");
        ReflectionTestUtils.setField(otpService, "mailPassword", "");

        RuntimeException ex = assertThrows(RuntimeException.class, () -> otpService.generateAndSendOtp("test@example.com"));
        assertEquals("SMTP is not configured. Set SPRING_MAIL_USERNAME and SPRING_MAIL_PASSWORD.", ex.getMessage());
        verify(mailSender, never()).send(any(org.springframework.mail.SimpleMailMessage.class));
    }
}
