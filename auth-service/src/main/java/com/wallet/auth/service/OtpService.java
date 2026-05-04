package com.wallet.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

@Service
public class OtpService {
    private static final Logger log = LoggerFactory.getLogger(OtpService.class);

    private final RedisTemplate<String, Object> redisTemplate;
    private final JavaMailSender mailSender;
    private final SecureRandom secureRandom = new SecureRandom();
    private static final String OTP_PREFIX = "otp:";
    private static final String OTP_VERIFIED_PREFIX = "otp_verified:";
    private static final int OTP_EXPIRY_MINUTES = 5;

    @Value("${spring.mail.username:no-reply@digitalwallet.com}")
    private String mailUsername;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    @Value("${spring.mail.from:}")
    private String mailFrom;

    @Autowired
    public OtpService(RedisTemplate<String, Object> redisTemplate, JavaMailSender mailSender) {
        this.redisTemplate = redisTemplate;
        this.mailSender = mailSender;
    }

    public void generateAndSendOtp(String email) {
        if (mailUsername == null || mailUsername.isBlank() || mailPassword == null || mailPassword.isBlank()) {
            throw new RuntimeException("SMTP is not configured. Set SPRING_MAIL_USERNAME and SPRING_MAIL_PASSWORD.");
        }

        String otp = String.format("%06d", secureRandom.nextInt(1000000));
        
        // Store in Redis
        redisTemplate.opsForValue().set(OTP_PREFIX + email, otp, OTP_EXPIRY_MINUTES, TimeUnit.MINUTES);
        // Clear any older verification state whenever a new OTP is issued.
        redisTemplate.delete(OTP_VERIFIED_PREFIX + email);
        
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom((mailFrom != null && !mailFrom.isBlank()) ? mailFrom : mailUsername);
        message.setTo(email);
        message.setSubject("Digital Wallet OTP");
        message.setText(
                "Your Digital Wallet reset OTP is " + otp + ".\n\n" +
                "It will expire in " + OTP_EXPIRY_MINUTES + " minutes.\n" +
                "If you did not request this, please ignore this email."
        );

        try {
            mailSender.send(message);
            log.info("OTP email sent successfully to {}", email);
        } catch (Exception e) {
            log.error("Failed to send OTP email to {}", email, e);
            throw new RuntimeException("Unable to send OTP email right now. Please try again.");
        }
    }

    public boolean verifyOtp(String email, String code) {
        Object storedOtpObj = redisTemplate.opsForValue().get(OTP_PREFIX + email);
        String storedOtp = storedOtpObj != null ? storedOtpObj.toString() : null;
        if (storedOtp == null || !storedOtp.equals(code)) {
            return false;
        }
        redisTemplate.opsForValue().set(OTP_VERIFIED_PREFIX + email, code, OTP_EXPIRY_MINUTES, TimeUnit.MINUTES);
        // Delete OTP after successful verification to prevent reuse
        redisTemplate.delete(OTP_PREFIX + email);
        return true;
    }

    public boolean isOtpVerifiedForReset(String email, String code) {
        Object verifiedOtpObj = redisTemplate.opsForValue().get(OTP_VERIFIED_PREFIX + email);
        String verifiedOtp = verifiedOtpObj != null ? verifiedOtpObj.toString() : null;
        return verifiedOtp != null && verifiedOtp.equals(code);
    }

    public void clearOtpVerification(String email) {
        redisTemplate.delete(OTP_VERIFIED_PREFIX + email);
    }
}
