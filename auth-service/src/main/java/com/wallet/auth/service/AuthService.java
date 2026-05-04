package com.wallet.auth.service;

import com.wallet.auth.dto.*;
import com.wallet.auth.entity.RefreshToken;
import com.wallet.auth.entity.UserCredential;
import com.wallet.auth.repository.RefreshTokenRepository;
import com.wallet.auth.repository.UserCredentialRepository;
import com.wallet.auth.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final String ADMIN_USERNAME_SUFFIX = "_admin";

    @Autowired
    private UserCredentialRepository repository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private TokenBlacklistService blacklistService;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private OtpService otpService;

    public String saveUser(RegisterRequest request) {
        Optional<UserCredential> existing = repository.findByEmailIgnoreCase(request.getEmail());
        if(existing.isPresent()){
            throw new RuntimeException("User already exists with email " + request.getEmail());
        }
        if(repository.findByUsernameIgnoreCase(request.getUsername()).isPresent()){
            throw new RuntimeException("User already exists with username " + request.getUsername());
        }
        UserCredential credential = new UserCredential();
        credential.setUsername(request.getUsername());
        credential.setEmail(request.getEmail());
        credential.setPassword(passwordEncoder.encode(request.getPassword()));
        credential.setRole(deriveRoleFromUsername(request.getUsername()));
        credential.setStatus("PENDING_KYC");
        
        repository.saveAndFlush(credential);

        return "User registration successful";
    }

    public AuthResponse login(AuthRequest request) {
        UserCredential user = findUserForLogin(request.getUsername());

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        String effectiveRole = deriveRoleFromUsername(user.getUsername());
        if (!effectiveRole.equalsIgnoreCase(user.getRole())) {
            user.setRole(effectiveRole);
            user = repository.save(user);
        }
        
        String token = jwtUtil.generateToken(user.getEmail(), effectiveRole, user.getId().toString(), user.getFullName(), user.getPhoneNumber());
        
        // Generate and save Refresh Token
        RefreshToken refreshToken = createRefreshToken(user);
        
        return new AuthResponse(token, refreshToken.getToken(), user.getId().toString(), user.getUsername(), user.getEmail(), effectiveRole, user.getFullName(), user.getPhoneNumber());
    }

    public RefreshToken createRefreshToken(UserCredential user) {
        refreshTokenRepository.findByUserCredential(user).ifPresent(existingToken -> {
            refreshTokenRepository.delete(existingToken);
            refreshTokenRepository.flush();
        });
        RefreshToken refreshToken = RefreshToken.builder()
                .userCredential(user)
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plusMillis(86400000)) // 24 hours
                .build();
        return refreshTokenRepository.save(refreshToken);
    }

    public AuthResponse refreshToken(String requestToken) {
        return refreshTokenRepository.findByToken(requestToken)
                .map(this::verifyExpiration)
                .map(RefreshToken::getUserCredential)
                .map(user -> {
                    UserCredential currentUser = user;
                    String effectiveRole = deriveRoleFromUsername(user.getUsername());
                    if (!effectiveRole.equalsIgnoreCase(user.getRole())) {
                        currentUser.setRole(effectiveRole);
                        currentUser = repository.save(currentUser);
                    }
                    String token = jwtUtil.generateToken(currentUser.getEmail(), effectiveRole, currentUser.getId().toString(), currentUser.getFullName(), currentUser.getPhoneNumber());
                    return new AuthResponse(token, requestToken, currentUser.getId().toString(), currentUser.getUsername(), currentUser.getEmail(), effectiveRole, currentUser.getFullName(), currentUser.getPhoneNumber());
                })
                .orElseThrow(() -> new RuntimeException("Refresh token is not in database!"));
    }

    private RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().compareTo(Instant.now()) < 0) {
            refreshTokenRepository.delete(token);
            throw new RuntimeException("Refresh token was expired. Please make a new signin request");
        }
        return token;
    }

    public UserCredential getProfile(UUID userId) {
        return repository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
    }

    public List<UserCredential> findAllUsers() {
        return repository.findAll();
    }

    public UserCredential updateProfile(UUID userId, UpdateProfileRequest request) {
        UserCredential user = repository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        if (request.getFullName() != null) user.setFullName(request.getFullName());
        if (request.getPhoneNumber() != null) user.setPhoneNumber(request.getPhoneNumber());

        log.info("Updating profile for user: {}", userId);
        return repository.save(user);
    }

    public void updateUserStatus(UUID userId, String status) {
        UserCredential user = repository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        user.setStatus(status);
        repository.save(user);
        log.info("Updated status for user {}: {}", userId, status);
    }

    public void changePassword(UUID userId, ChangePasswordRequest request) {
        UserCredential user = repository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid old password");
        }
        
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        repository.save(user);
        refreshTokenRepository.deleteByUserCredential(user);
        log.info("Password changed successfully for user: {}", userId);
    }


    public void deleteUser(java.util.UUID userId) {
        if (!repository.existsById(userId)) {
            throw new RuntimeException("User not found: " + userId);
        }
        repository.deleteById(userId);
    }

    public void logout(String token) {
        blacklistService.blacklistToken(token);
        // Also invalidate refresh token if needed
    }

    public void forgotPassword(String email) {
        repository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new RuntimeException("User with email " + email + " not found"));
        otpService.generateAndSendOtp(email);
        log.info("Forgot-password OTP dispatched for {}", email);
    }

    public void resetPassword(String resetToken, String newPassword) {
        UserCredential user = repository.findByResetPasswordToken(resetToken)
                .orElseThrow(() -> new RuntimeException("Invalid reset token"));

        if (user.getResetPasswordTokenExpiry() == null || user.getResetPasswordTokenExpiry().isBefore(LocalDateTime.now())) {
            user.setResetPasswordToken(null);
            user.setResetPasswordTokenExpiry(null);
            repository.save(user);
            throw new RuntimeException("Reset token has expired");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetPasswordToken(null);
        user.setResetPasswordTokenExpiry(null);
        repository.save(user);
        refreshTokenRepository.deleteByUserCredential(user);
        log.info("Password reset successfully for user: {}", user.getEmail());
    }

    public void resetPasswordWithOtp(String email, String otpCode, String newPassword) {
        boolean validOtp = otpService.verifyOtp(email, otpCode);
        if (!validOtp && !otpService.isOtpVerifiedForReset(email, otpCode)) {
            throw new RuntimeException("Invalid or expired OTP");
        }

        UserCredential user = repository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new RuntimeException("User with email " + email + " not found"));

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetPasswordToken(null);
        user.setResetPasswordTokenExpiry(null);
        repository.save(user);
        otpService.clearOtpVerification(email);
        refreshTokenRepository.deleteByUserCredential(user);
        log.info("Password reset successfully using OTP for user: {}", user.getEmail());
    }

    public void sendOtp(String phoneNumber) {
        String otp = String.format("%06d", new java.util.Random().nextInt(999999));
        System.out.println("MOCK SMS: OTP for " + phoneNumber + " is " + otp);
        // Save to Redis or DB with short expiry
        redisTemplate.opsForValue().set("otp_" + phoneNumber, otp, 5, TimeUnit.MINUTES);
    }

    public boolean verifyOtp(String phoneNumber, String code) {
        Object cachedOtp = redisTemplate.opsForValue().get("otp_" + phoneNumber);
        if (cachedOtp != null && cachedOtp.toString().equals(code)) {
            redisTemplate.delete("otp_" + phoneNumber);
            return true;
        }
        return false;
    }

    private UserCredential findUserForLogin(String identifier) {
        String normalizedIdentifier = identifier == null ? "" : identifier.trim();
        Optional<UserCredential> user = normalizedIdentifier.contains("@")
                ? repository.findByEmailIgnoreCase(normalizedIdentifier)
                : repository.findByUsernameIgnoreCase(normalizedIdentifier);

        return user.orElseThrow(() -> new RuntimeException("Invalid credentials"));
    }

    private String deriveRoleFromUsername(String username) {
        String normalizedUsername = username == null ? "" : username.trim().toLowerCase();
        return normalizedUsername.endsWith(ADMIN_USERNAME_SUFFIX) ? "ADMIN" : "USER";
    }
}
