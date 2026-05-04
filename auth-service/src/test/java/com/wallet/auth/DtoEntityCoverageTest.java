package com.wallet.auth;

import com.wallet.auth.dto.AuthResponse;
import com.wallet.auth.dto.ForgotPasswordRequest;
import com.wallet.auth.dto.RefreshTokenRequest;
import com.wallet.auth.dto.ResetPasswordRequest;
import com.wallet.auth.entity.RefreshToken;
import com.wallet.auth.entity.UserCredential;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DtoEntityCoverageTest {

    @Test
    void authResponse_GettersAndSetters() {
        AuthResponse response = new AuthResponse();
        response.setToken("token");
        response.setRefreshToken("refresh");
        response.setUserId("1");
        response.setUsername("wallet-user");
        response.setEmail("user@test.com");
        response.setRole("USER");
        response.setFullName("User");
        response.setPhoneNumber("123");

        assertEquals("token", response.getToken());
        assertEquals("refresh", response.getRefreshToken());
        assertEquals("1", response.getUserId());
        assertEquals("wallet-user", response.getUsername());
        assertEquals("user@test.com", response.getEmail());
        assertEquals("USER", response.getRole());
        assertEquals("User", response.getFullName());
        assertEquals("123", response.getPhoneNumber());

        AuthResponse fromCtor = new AuthResponse("t", "r", "2", "admin-user", "e", "ADMIN", "A", "9");
        assertEquals("ADMIN", fromCtor.getRole());
    }

    @Test
    void requestDtos_GettersAndConstructors() {
        ForgotPasswordRequest forgot = new ForgotPasswordRequest();
        forgot.setEmail("user@test.com");
        assertEquals("user@test.com", forgot.getEmail());
        assertEquals("user2@test.com", new ForgotPasswordRequest("user2@test.com").getEmail());

        RefreshTokenRequest refresh = new RefreshTokenRequest();
        refresh.setRefreshToken("refresh");
        assertEquals("refresh", refresh.getRefreshToken());
        assertEquals("r2", new RefreshTokenRequest("r2").getRefreshToken());

        ResetPasswordRequest reset = new ResetPasswordRequest();
        reset.setToken("token");
        reset.setNewPassword("pass");
        assertEquals("token", reset.getToken());
        assertEquals("pass", reset.getNewPassword());
        assertEquals("newpass", new ResetPasswordRequest("token2", "newpass").getNewPassword());
    }

    @Test
    void refreshToken_EntityAndBuilder() {
        UserCredential user = new UserCredential();
        user.setId(UUID.randomUUID());
        Instant expiry = Instant.now().plusSeconds(60);

        RefreshToken token = new RefreshToken();
        token.setId(1L);
        token.setToken("token");
        token.setExpiryDate(expiry);
        token.setUserCredential(user);

        assertEquals(1L, token.getId());
        assertEquals("token", token.getToken());
        assertEquals(expiry, token.getExpiryDate());
        assertEquals(user, token.getUserCredential());

        RefreshToken built = RefreshToken.builder()
                .id(2L)
                .token("built")
                .expiryDate(expiry)
                .userCredential(user)
                .build();
        assertEquals("built", built.getToken());
        assertEquals(2L, built.getId());
    }
}
