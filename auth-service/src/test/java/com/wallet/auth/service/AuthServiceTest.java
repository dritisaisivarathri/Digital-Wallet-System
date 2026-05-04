package com.wallet.auth.service;

import com.wallet.auth.dto.AuthRequest;
import com.wallet.auth.dto.AuthResponse;
import com.wallet.auth.dto.ChangePasswordRequest;
import com.wallet.auth.dto.ForgotPasswordRequest;
import com.wallet.auth.dto.RegisterRequest;
import com.wallet.auth.dto.ResetPasswordRequest;
import com.wallet.auth.dto.UpdateProfileRequest;
import com.wallet.auth.entity.RefreshToken;
import com.wallet.auth.entity.UserCredential;
import com.wallet.auth.repository.RefreshTokenRepository;
import com.wallet.auth.repository.UserCredentialRepository;
import com.wallet.auth.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private UserCredentialRepository repository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private org.springframework.data.redis.core.ValueOperations<String, Object> valueOperations;

    @Mock
    private TokenBlacklistService blacklistService;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private OtpService otpService;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private UserCredential userCredential;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest("testuser", "test@example.com", "password", "USER");
        userCredential = new UserCredential(UUID.randomUUID(), "testuser", "test@example.com", "encodedPassword", "USER", "PENDING_KYC", "Test User", "1234567890");
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void saveUser_Success() {
        when(repository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(repository.findByUsernameIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");

        String result = authService.saveUser(registerRequest);

        assertEquals("User registration successful", result);
        verify(repository, times(1)).saveAndFlush(any(UserCredential.class));
    }

    @Test
    void saveUser_UserAlreadyExists_Email() {
        when(repository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.of(userCredential));

        assertThrows(RuntimeException.class, () -> authService.saveUser(registerRequest));
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void saveUser_UserAlreadyExists_Username() {
        when(repository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(repository.findByUsernameIgnoreCase(anyString())).thenReturn(Optional.of(userCredential));

        assertThrows(RuntimeException.class, () -> authService.saveUser(registerRequest));
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void saveUser_DefaultRole() {
        RegisterRequest request = new RegisterRequest("testuser", "test@example.com", "password", null);
        when(repository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(repository.findByUsernameIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");

        authService.saveUser(request);

        verify(repository).saveAndFlush(argThat(user -> "USER".equals(user.getRole())));
    }

    @Test
    void saveUser_AdminUsernameGetsAdminRole() {
        RegisterRequest request = new RegisterRequest("adminuser_admin", "ops@gmail.com", "password", "USER");
        when(repository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(repository.findByUsernameIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");

        authService.saveUser(request);

        verify(repository).saveAndFlush(argThat(user -> "ADMIN".equals(user.getRole())));
    }

    @Test
    void login_Success() {
        AuthRequest authRequest = new AuthRequest("testuser", "password");
        when(repository.findByUsernameIgnoreCase(anyString())).thenReturn(Optional.of(userCredential));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(jwtUtil.generateToken(anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn("testToken");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthResponse response = authService.login(authRequest);

        assertNotNull(response);
        assertEquals("testToken", response.getToken());
        assertEquals(userCredential.getEmail(), response.getEmail());
    }

    @Test
    void login_WithEmailIdentifier_UsesEmailLookup() {
        AuthRequest authRequest = new AuthRequest("test@example.com", "password");
        when(repository.findByEmailIgnoreCase("test@example.com")).thenReturn(Optional.of(userCredential));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(jwtUtil.generateToken(anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn("testToken");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthResponse response = authService.login(authRequest);

        assertNotNull(response);
        assertEquals("test@example.com", response.getEmail());
        verify(repository).findByEmailIgnoreCase("test@example.com");
        verify(repository, never()).findByUsernameIgnoreCase("test@example.com");
    }

    @Test
    void login_AdminUsernamePromotesRoleToAdmin() {
        AuthRequest authRequest = new AuthRequest("ops@gmail.com", "password");
        UserCredential adminCandidate = new UserCredential(UUID.randomUUID(), "ops_admin", "ops@gmail.com", "encodedPassword", "USER",
                "PENDING_KYC", "Ops User", "1234567890");
        when(repository.findByEmailIgnoreCase("ops@gmail.com")).thenReturn(Optional.of(adminCandidate));
        when(repository.save(any(UserCredential.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(jwtUtil.generateToken(anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn("adminToken");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthResponse response = authService.login(authRequest);

        assertEquals("ADMIN", response.getRole());
        verify(repository).save(argThat(user -> "ADMIN".equals(user.getRole())));
    }

    @Test
    void login_InvalidCredentials_UserNotFound() {
        AuthRequest authRequest = new AuthRequest("testuser", "password");
        when(repository.findByUsernameIgnoreCase(anyString())).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> authService.login(authRequest));
    }

    @Test
    void login_InvalidCredentials_WrongPassword() {
        AuthRequest authRequest = new AuthRequest("testuser", "wrongpassword");
        when(repository.findByUsernameIgnoreCase(anyString())).thenReturn(Optional.of(userCredential));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThrows(RuntimeException.class, () -> authService.login(authRequest));
    }

    @Test
    void getProfile_Success() {
        UUID userId = userCredential.getId();
        when(repository.findById(userId)).thenReturn(Optional.of(userCredential));

        UserCredential result = authService.getProfile(userId);

        assertNotNull(result);
        assertEquals(userId, result.getId());
    }

    @Test
    void getProfile_NotFound() {
        UUID userId = UUID.randomUUID();
        when(repository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> authService.getProfile(userId));
    }

    @Test
    void updateUserStatus_Success() {
        UUID userId = userCredential.getId();
        when(repository.findById(userId)).thenReturn(Optional.of(userCredential));

        authService.updateUserStatus(userId, "ACTIVE");

        assertEquals("ACTIVE", userCredential.getStatus());
        verify(repository).save(userCredential);
    }

    @Test
    void updateUserStatus_UserNotFound() {
        UUID userId = UUID.randomUUID();
        when(repository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> authService.updateUserStatus(userId, "ACTIVE"));
    }

    @Test
    void updateProfile_Success() {
        UUID userId = userCredential.getId();
        UpdateProfileRequest request = new UpdateProfileRequest("Updated User", "9999999999");
        when(repository.findById(userId)).thenReturn(Optional.of(userCredential));
        when(repository.save(any(UserCredential.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserCredential result = authService.updateProfile(userId, request);

        assertEquals("Updated User", result.getFullName());
        assertEquals("9999999999", result.getPhoneNumber());
    }

    @Test
    void updateProfile_UserNotFound() {
        UUID userId = UUID.randomUUID();
        UpdateProfileRequest request = new UpdateProfileRequest("Updated User", "9999999999");
        when(repository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> authService.updateProfile(userId, request));
    }

    @Test
    void changePassword_Success() {
        UUID userId = userCredential.getId();
        ChangePasswordRequest request = new ChangePasswordRequest("oldPass", "newPass");
        
        when(repository.findById(userId)).thenReturn(Optional.of(userCredential));
        when(passwordEncoder.matches("oldPass", userCredential.getPassword())).thenReturn(true);
        when(passwordEncoder.encode("newPass")).thenReturn("newEncodedPass");

        authService.changePassword(userId, request);

        assertEquals("newEncodedPass", userCredential.getPassword());
        verify(repository).save(userCredential);
        verify(refreshTokenRepository).deleteByUserCredential(userCredential);
    }

    @Test
    void changePassword_WrongOldPassword() {
        UUID userId = userCredential.getId();
        ChangePasswordRequest request = new ChangePasswordRequest("wrongOldPass", "newPass");

        when(repository.findById(userId)).thenReturn(Optional.of(userCredential));
        when(passwordEncoder.matches("wrongOldPass", userCredential.getPassword())).thenReturn(false);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> authService.changePassword(userId, request));
        assertEquals("Invalid old password", exception.getMessage());
    }

    @Test
    void changePassword_UserNotFound() {
        UUID userId = UUID.randomUUID();
        ChangePasswordRequest request = new ChangePasswordRequest("oldPass", "newPass");

        when(repository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> authService.changePassword(userId, request));
    }

    @Test
    void forgotPassword_Success() {
        when(repository.findByEmailIgnoreCase("test@example.com")).thenReturn(Optional.of(userCredential));

        authService.forgotPassword("test@example.com");

        verify(otpService).generateAndSendOtp("test@example.com");
        verify(repository, never()).save(any(UserCredential.class));
    }

    @Test
    void resetPassword_Success() {
        userCredential.setResetPasswordToken("reset-token");
        userCredential.setResetPasswordTokenExpiry(LocalDateTime.now().plusMinutes(5));
        when(repository.findByResetPasswordToken("reset-token")).thenReturn(Optional.of(userCredential));
        when(passwordEncoder.encode("1234")).thenReturn("encodedNewPassword");
        when(repository.save(any(UserCredential.class))).thenAnswer(invocation -> invocation.getArgument(0));

        authService.resetPassword("reset-token", "1234");

        assertEquals("encodedNewPassword", userCredential.getPassword());
        assertNull(userCredential.getResetPasswordToken());
        assertNull(userCredential.getResetPasswordTokenExpiry());
        verify(refreshTokenRepository).deleteByUserCredential(userCredential);
    }

    @Test
    void resetPassword_InvalidToken() {
        when(repository.findByResetPasswordToken("bad-token")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> authService.resetPassword("bad-token", "1234"));

        assertEquals("Invalid reset token", exception.getMessage());
    }

    @Test
    void resetPassword_ExpiredToken() {
        userCredential.setResetPasswordToken("expired-token");
        userCredential.setResetPasswordTokenExpiry(LocalDateTime.now().minusMinutes(1));
        when(repository.findByResetPasswordToken("expired-token")).thenReturn(Optional.of(userCredential));
        when(repository.save(any(UserCredential.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> authService.resetPassword("expired-token", "1234"));

        assertEquals("Reset token has expired", exception.getMessage());
        assertNull(userCredential.getResetPasswordToken());
        assertNull(userCredential.getResetPasswordTokenExpiry());
    }

    @Test
    void createRefreshToken_ReplacesExistingToken() {
        RefreshToken existing = new RefreshToken();
        when(refreshTokenRepository.findByUserCredential(userCredential)).thenReturn(Optional.of(existing));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RefreshToken result = authService.createRefreshToken(userCredential);

        assertNotNull(result.getToken());
        verify(refreshTokenRepository).delete(existing);
        verify(refreshTokenRepository).flush();
    }

    @Test
    void refreshToken_Success() {
        RefreshToken token = new RefreshToken(1L, "refresh", Instant.now().plusSeconds(60), userCredential);
        when(refreshTokenRepository.findByToken("refresh")).thenReturn(Optional.of(token));
        when(jwtUtil.generateToken(anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn("jwt");

        AuthResponse response = authService.refreshToken("refresh");

        assertEquals("jwt", response.getToken());
        assertEquals("refresh", response.getRefreshToken());
    }

    @Test
    void deleteUser_Success() {
        UUID userId = userCredential.getId();
        when(repository.existsById(userId)).thenReturn(true);

        authService.deleteUser(userId);

        verify(repository).deleteById(userId);
    }

    @Test
    void deleteUser_NotFound() {
        UUID userId = UUID.randomUUID();
        when(repository.existsById(userId)).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.deleteUser(userId));
        assertEquals("User not found: " + userId, ex.getMessage());
    }

    @Test
    void logout_BlacklistsToken() {
        authService.logout("Bearer token");

        verify(blacklistService).blacklistToken("Bearer token");
    }

    @Test
    void forgotPassword_UserNotFound() {
        when(repository.findByEmailIgnoreCase("missing@test.com")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.forgotPassword("missing@test.com"));
        assertEquals("User with email missing@test.com not found", ex.getMessage());
    }

    @Test
    void findAllUsers_ReturnsRepositoryValues() {
        when(repository.findAll()).thenReturn(List.of(userCredential));

        assertEquals(1, authService.findAllUsers().size());
    }

    @Test
    void sendOtp_SavesOtpInRedis() {
        authService.sendOtp("9999999999");

        verify(valueOperations).set(eq("otp_9999999999"), anyString(), eq(5L), eq(TimeUnit.MINUTES));
    }

    @Test
    void verifyOtp_SuccessDeletesOtp() {
        when(valueOperations.get("otp_9999999999")).thenReturn("123456");

        assertTrue(authService.verifyOtp("9999999999", "123456"));
        verify(redisTemplate).delete("otp_9999999999");
    }

    @Test
    void verifyOtp_FailureReturnsFalse() {
        when(valueOperations.get("otp_9999999999")).thenReturn("654321");

        assertFalse(authService.verifyOtp("9999999999", "123456"));
        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    void resetPasswordWithOtp_AllowsVerifiedOtpFlow() {
        when(otpService.verifyOtp("test@example.com", "123456")).thenReturn(false);
        when(otpService.isOtpVerifiedForReset("test@example.com", "123456")).thenReturn(true);
        when(repository.findByEmailIgnoreCase("test@example.com")).thenReturn(Optional.of(userCredential));
        when(passwordEncoder.encode("newPassword!")).thenReturn("encodedNewPassword");
        when(repository.save(any(UserCredential.class))).thenAnswer(invocation -> invocation.getArgument(0));

        authService.resetPasswordWithOtp("test@example.com", "123456", "newPassword!");

        assertEquals("encodedNewPassword", userCredential.getPassword());
        verify(otpService).clearOtpVerification("test@example.com");
        verify(refreshTokenRepository).deleteByUserCredential(userCredential);
    }

    @Test
    void resetPasswordWithOtp_FailsWhenOtpNotVerified() {
        when(otpService.verifyOtp("test@example.com", "123456")).thenReturn(false);
        when(otpService.isOtpVerifiedForReset("test@example.com", "123456")).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.resetPasswordWithOtp("test@example.com", "123456", "newPassword!"));
        assertEquals("Invalid or expired OTP", ex.getMessage());
    }
}
