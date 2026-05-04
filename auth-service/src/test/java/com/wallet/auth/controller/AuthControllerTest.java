package com.wallet.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wallet.auth.dto.AuthRequest;
import com.wallet.auth.dto.AuthResponse;
import com.wallet.auth.dto.RegisterRequest;
import com.wallet.auth.dto.UpdateProfileRequest;
import com.wallet.auth.entity.UserCredential;
import com.wallet.auth.service.AuthService;
import com.wallet.auth.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false) // Disable security filters for simple controller unit tests
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private com.wallet.auth.service.TokenBlacklistService blacklistService;

    @MockBean
    private com.wallet.auth.service.OtpService otpService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void addNewUser_Success() throws Exception {
        RegisterRequest request = new RegisterRequest("testuser", "test@example.com", "password", "USER");
        when(authService.saveUser(any(RegisterRequest.class))).thenReturn("User registration successful");

        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("User registration successful"));
    }

    @Test
    void addNewUser_Failure() throws Exception {
        RegisterRequest request = new RegisterRequest("testuser", "test@example.com", "password", "USER");
        when(authService.saveUser(any(RegisterRequest.class))).thenThrow(new RuntimeException("User already exists"));

        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("User already exists"));
    }

    @Test
    void getToken_Success() throws Exception {
        AuthRequest request = new AuthRequest("testuser", "password");
        AuthResponse response = new AuthResponse("token", "refreshToken", "123", "testuser", "test@example.com", "USER", "Test User", "1234567890");
        when(authService.login(any(AuthRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("token"))
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    void getToken_Failure() throws Exception {
        AuthRequest request = new AuthRequest("testuser", "password");
        when(authService.login(any(AuthRequest.class))).thenThrow(new RuntimeException("Invalid credentials"));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Invalid Authentication"));
    }

    @Test
    void getProfile_Success() throws Exception {
        UUID userId = UUID.randomUUID();
        UserCredential user = new UserCredential(userId, "testuser", "test@example.com", "pass", "USER", "ACTIVE", "Test User", "123");
        when(authService.getProfile(userId)).thenReturn(user);

        mockMvc.perform(get("/api/auth/users/" + userId + "/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"));
    }

    @Test
    void validateToken_Success() throws Exception {
        mockMvc.perform(get("/api/auth/validate").param("token", "someToken"))
                .andExpect(status().isOk())
                .andExpect(content().string("Token is valid"));
    }

    @Test
    void updateStatus_Success() throws Exception {
        UUID userId = UUID.randomUUID();
        mockMvc.perform(post("/api/auth/internal/users/" + userId + "/status").param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(content().string("User status updated to ACTIVE"));
        
        verify(authService).updateUserStatus(userId, "ACTIVE");
    }

    @Test
    void getProfile_NotFound() throws Exception {
        UUID userId = UUID.randomUUID();
        when(authService.getProfile(userId)).thenThrow(new RuntimeException("User profile not found"));

        mockMvc.perform(get("/api/auth/users/" + userId + "/profile"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("User profile not found"));
    }

    @Test
    @WithMockUser(username = "262e53eb-b506-4a69-8ddf-bcedced51746", roles = "USER")
    void getProfile_UsesAuthenticatedUserIdForNonAdmin() throws Exception {
        UUID requestedUserId = UUID.randomUUID();
        UUID authenticatedUserId = UUID.fromString("262e53eb-b506-4a69-8ddf-bcedced51746");
        UserCredential user = new UserCredential(authenticatedUserId, "testuser", "test@example.com", "pass", "USER",
                "ACTIVE", "Test User", "123");
        when(authService.getProfile(authenticatedUserId)).thenReturn(user);

        mockMvc.perform(get("/api/auth/users/" + requestedUserId + "/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"));

        verify(authService).getProfile(authenticatedUserId);
    }

    @Test
    @WithMockUser(username = "262e53eb-b506-4a69-8ddf-bcedced51746", roles = "USER")
    void updateProfile_UsesAuthenticatedUserId() throws Exception {
        UUID authenticatedUserId = UUID.fromString("262e53eb-b506-4a69-8ddf-bcedced51746");
        UpdateProfileRequest request = new UpdateProfileRequest("Test User", "1234567890");
        UserCredential updatedUser = new UserCredential(authenticatedUserId, "testuser", "test@example.com", "pass",
                "USER", "ACTIVE", "Test User", "1234567890");
        when(authService.updateProfile(any(UUID.class), any(UpdateProfileRequest.class))).thenReturn(updatedUser);

        mockMvc.perform(put("/api/auth/updateprofile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Test User"))
                .andExpect(jsonPath("$.phoneNumber").value("1234567890"));

        verify(authService).updateProfile(org.mockito.ArgumentMatchers.eq(authenticatedUserId),
                argThat(profileRequest -> "Test User".equals(profileRequest.getFullName())
                        && "1234567890".equals(profileRequest.getPhoneNumber())));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getProfile_AdminCanAccessAnyProfile() throws Exception {
        UUID requestedUserId = UUID.randomUUID();
        UserCredential user = new UserCredential(requestedUserId, "otheruser", "other@example.com", "pass", "USER",
                "ACTIVE", "Other User", "123");
        when(authService.getProfile(requestedUserId)).thenReturn(user);

        mockMvc.perform(get("/api/auth/users/" + requestedUserId + "/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("otheruser"));

        verify(authService).getProfile(requestedUserId);
    }

    @Test
    @WithMockUser(username = "not-a-uuid")
    void getProfile_InvalidPrincipalUuid() throws Exception {
        UUID requestedUserId = UUID.randomUUID();
        // Fallback to requestedUserId in catch block
        when(authService.getProfile(requestedUserId)).thenThrow(new RuntimeException("Profile not found"));

        mockMvc.perform(get("/api/auth/users/" + requestedUserId + "/profile"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "262e53eb-b506-4a69-8ddf-bcedced51746", roles = "USER")
    void updateProfile_Failure() throws Exception {
        UpdateProfileRequest request = new UpdateProfileRequest("Test User", "1234567890");
        when(authService.updateProfile(any(UUID.class), any(UpdateProfileRequest.class)))
                .thenThrow(new RuntimeException("Update failed"));

        mockMvc.perform(put("/api/auth/updateprofile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Update failed"));
    }

    @Test
    void logout_Success() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                .header("Authorization", "Bearer mockToken"))
                .andExpect(status().isOk())
                .andExpect(content().string("Successfully logged out"));

        verify(authService).logout("Bearer mockToken");
    }

    @Test
    @WithMockUser(username = "262e53eb-b506-4a69-8ddf-bcedced51746", roles = "USER")
    void changePassword_Success() throws Exception {
        com.wallet.auth.dto.ChangePasswordRequest request = new com.wallet.auth.dto.ChangePasswordRequest("oldPass", "newPass");
        
        mockMvc.perform(post("/api/auth/change-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Password changed successfully"));

        verify(authService).changePassword(any(UUID.class), any());
    }

    @Test
    void sendOtp_Success() throws Exception {
        com.wallet.auth.dto.OtpRequest request = new com.wallet.auth.dto.OtpRequest("test@example.com");
        
        mockMvc.perform(post("/api/auth/send-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("OTP sent to your email."));

        verify(otpService).generateAndSendOtp("test@example.com");
    }

    @Test
    void verifyOtp_Success() throws Exception {
        com.wallet.auth.dto.VerifyOtpRequest request = new com.wallet.auth.dto.VerifyOtpRequest("test@example.com", "123456");
        when(otpService.verifyOtp("test@example.com", "123456")).thenReturn(true);

        mockMvc.perform(post("/api/auth/verify-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("OTP verified successfully"));
    }

    @Test
    void verifyOtp_Failure() throws Exception {
        com.wallet.auth.dto.VerifyOtpRequest request = new com.wallet.auth.dto.VerifyOtpRequest("test@example.com", "wrong");
        when(otpService.verifyOtp("test@example.com", "wrong")).thenReturn(false);

        mockMvc.perform(post("/api/auth/verify-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Invalid or expired OTP"));
    }

    @Test
    void getAllUsersInternal_Success() throws Exception {
        when(authService.findAllUsers()).thenReturn(java.util.List.of(new UserCredential()));

        mockMvc.perform(get("/api/auth/internal/users"))
                .andExpect(status().isOk());
    }

    @Test
    void deleteUserInternal_NotFound() throws Exception {
        UUID userId = UUID.randomUUID();
        doThrow(new RuntimeException("User not found")).when(authService).deleteUser(userId);

        mockMvc.perform(delete("/api/auth/internal/users/" + userId))
                .andExpect(status().isNotFound())
                .andExpect(content().string("User not found"));
    }

    @Test
    void refreshToken_Success() throws Exception {
        AuthResponse response = new AuthResponse("token", "refresh", "1", "wallet-user", "user@test.com", "USER", "User", "123");
        when(authService.refreshToken("refresh")).thenReturn(response);

        mockMvc.perform(post("/api/auth/refresh-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"refresh\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refreshToken").value("refresh"));
    }

    @Test
    void forgotPassword_Failure() throws Exception {
        doThrow(new RuntimeException("missing")).when(authService).forgotPassword("missing@test.com");

        mockMvc.perform(post("/api/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"missing@test.com\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("missing"));
    }

    @Test
    void resetPassword_Failure() throws Exception {
        doThrow(new RuntimeException("invalid token")).when(authService).resetPassword("bad", "1234");

        mockMvc.perform(post("/api/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"bad\",\"newPassword\":\"1234\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("invalid token"));
    }
}
