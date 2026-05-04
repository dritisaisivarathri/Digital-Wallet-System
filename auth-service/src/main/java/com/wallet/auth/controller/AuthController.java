package com.wallet.auth.controller;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.wallet.auth.dto.*;
import com.wallet.auth.service.AuthService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthService service;

    @Autowired
    private com.wallet.auth.service.OtpService otpService;

    @PostMapping("/signup")
    public ResponseEntity<String> addNewUser(@jakarta.validation.Valid @RequestBody RegisterRequest request) {
        try {
            return ResponseEntity.ok(service.saveUser(request));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> getToken(@jakarta.validation.Valid @RequestBody AuthRequest request) {
        try {
            AuthResponse response = service.login(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(401).body("Invalid Authentication");
        }
    }

    @GetMapping("/validate")
    public String validateToken(@RequestParam("token") String token) {
        return "Token is valid";
    }

    @Hidden
    @GetMapping("/internal/users")
    public ResponseEntity<?> getAllUsersInternal() {
        return ResponseEntity.ok(service.findAllUsers());
    }

    @Hidden
    @GetMapping("/internal/users/{userId}")
    public ResponseEntity<?> getUserInternal(@PathVariable java.util.UUID userId) {
        try {
            return ResponseEntity.ok(service.getProfile(userId));
        } catch (Exception e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

    @Hidden
    @PostMapping("/internal/users/{userId}/status")
    public ResponseEntity<String> updateStatus(@PathVariable java.util.UUID userId, @RequestParam String status) {
        service.updateUserStatus(userId, status);
        return ResponseEntity.ok("User status updated to " + status);
    }

    @Hidden
    @DeleteMapping("/internal/users/{userId}")
    public ResponseEntity<String> deleteUserInternal(@PathVariable java.util.UUID userId) {
        try {
            service.deleteUser(userId);
            return ResponseEntity.ok("User credentials deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

    @GetMapping("/users/{userId}/profile")
    public ResponseEntity<?> getProfile(@PathVariable java.util.UUID userId, Authentication authentication) {
        java.util.UUID effectiveUserId = resolveEffectiveUserId(userId, authentication);
        try {
            return ResponseEntity.ok(service.getProfile(effectiveUserId));
        } catch (Exception e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

    @PutMapping("/updateprofile")
    public ResponseEntity<?> updateProfile(@jakarta.validation.Valid @RequestBody UpdateProfileRequest request,
            Authentication authentication) {
        java.util.UUID effectiveUserId = resolveAuthenticatedUserId(authentication);
        try {
            return ResponseEntity.ok(service.updateProfile(effectiveUserId, request));
        } catch (Exception e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(jakarta.servlet.http.HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            service.logout(authHeader);
            logger.info("User logged out successfully");
        }
        return ResponseEntity.ok("Successfully logged out");
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@jakarta.validation.Valid @RequestBody ChangePasswordRequest request,
                                            Authentication authentication) {
        java.util.UUID userId = resolveAuthenticatedUserId(authentication);
        try {
            service.changePassword(userId, request);
            return ResponseEntity.ok("Password changed successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/send-otp")
    public ResponseEntity<String> sendOtp(@jakarta.validation.Valid @RequestBody OtpRequest request) {
        otpService.generateAndSendOtp(request.getEmail());
        return ResponseEntity.ok("OTP sent to your email.");
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<String> verifyOtp(@jakarta.validation.Valid @RequestBody VerifyOtpRequest request) {
        boolean isValid = otpService.verifyOtp(request.getEmail(), request.getCode());
        if (isValid) {
            return ResponseEntity.ok("OTP verified successfully");
        } else {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).body("Invalid or expired OTP");
        }
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@jakarta.validation.Valid @RequestBody RefreshTokenRequest request) {
        try {
            return ResponseEntity.ok(service.refreshToken(request.getRefreshToken()));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@jakarta.validation.Valid @RequestBody ForgotPasswordRequest request) {
        try {
            service.forgotPassword(request.getEmail());
            return ResponseEntity.ok("If an account exists with that email, an OTP has been sent.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@jakarta.validation.Valid @RequestBody ResetPasswordRequest request) {
        try {
            service.resetPassword(request.getToken(), request.getNewPassword());
            return ResponseEntity.ok("Password has been reset successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/reset-password/otp")
    public ResponseEntity<String> resetPasswordWithOtp(@jakarta.validation.Valid @RequestBody ResetPasswordWithOtpRequest request) {
        try {
            service.resetPasswordWithOtp(request.getEmail(), request.getCode(), request.getNewPassword());
            return ResponseEntity.ok("Password has been reset successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    private java.util.UUID resolveAuthenticatedUserId(Authentication authentication) {
        if (authentication == null) {
            authentication = SecurityContextHolder.getContext().getAuthentication();
        }
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new RuntimeException("Authenticated user not found");
        }
        Object principal = authentication.getPrincipal();
        String principalValue = principal instanceof UserDetails userDetails
                ? userDetails.getUsername()
                : principal.toString();
        try {
            return java.util.UUID.fromString(principalValue);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid authenticated user");
        }
    }

    private java.util.UUID resolveEffectiveUserId(java.util.UUID requestedUserId, Authentication authentication) {
        if (authentication == null) {
            authentication = SecurityContextHolder.getContext().getAuthentication();
        }
        if (authentication == null || authentication.getPrincipal() == null) {
            return requestedUserId;
        }
        boolean isAdmin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
        if (isAdmin) {
            return requestedUserId;
        }
        Object principal = authentication.getPrincipal();
        String principalValue = principal instanceof UserDetails userDetails
                ? userDetails.getUsername()
                : principal.toString();
        try {
            return java.util.UUID.fromString(principalValue);
        } catch (IllegalArgumentException e) {
            return requestedUserId;
        }
    }
}
