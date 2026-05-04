package com.wallet.user.controller;

import com.wallet.user.dto.KycSubmitRequest;
import com.wallet.user.service.UserService;
import com.wallet.user.util.JwtUtil;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @Value("${upload.dir:uploads}")
    private String uploadDir;

    @PostMapping(value = "/kyc", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> submitKyc(@Parameter(hidden = true) @RequestHeader("Authorization") String token,
            @RequestParam("documentType") String documentType,
            @RequestParam("documentNumber") String documentNumber,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        try {
            UUID userId = UUID.fromString(jwtUtil.extractUserId(token));
            String email = jwtUtil.extractEmail(token);
            return ResponseEntity.ok(userService.submitKyc(userId, email, documentType, documentNumber, file));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(@Parameter(hidden = true) @RequestHeader("Authorization") String token) {
        try {
            UUID userId = UUID.fromString(jwtUtil.extractUserId(token));
            String email = jwtUtil.extractEmail(token);
            String role = jwtUtil.extractRole(token);
            String fullName = jwtUtil.extractFullName(token);
            String phoneNumber = jwtUtil.extractPhoneNumber(token);
            return ResponseEntity.ok(userService.getProfile(userId, email, role, fullName, phoneNumber));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    @PostMapping(value = "/profile", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateProfile(@Parameter(hidden = true) @RequestHeader("Authorization") String token,
            @RequestParam(value = "fullName", required = false) String fullName,
            @RequestParam(value = "phoneNumber", required = false) String phoneNumber,
            @RequestParam(value = "photo", required = false) org.springframework.web.multipart.MultipartFile photo) {
        try {
            System.out.println("DEBUG: updateProfile called for " + fullName);
            if (photo != null) {
                System.out.println("DEBUG: Photo received: " + photo.getOriginalFilename() + " (" + photo.getSize() + " bytes)");
            }
            UUID userId = UUID.fromString(jwtUtil.extractUserId(token));
            String email = jwtUtil.extractEmail(token);
            String role = jwtUtil.extractRole(token);
            String tokenFullName = jwtUtil.extractFullName(token);
            String tokenPhoneNumber = jwtUtil.extractPhoneNumber(token);
            return ResponseEntity.ok(userService.updateProfile(
                    userId,
                    email,
                    role,
                    tokenFullName,
                    tokenPhoneNumber,
                    fullName,
                    phoneNumber,
                    photo));
        } catch (Exception e) {
            System.err.println("DEBUG: Error in updateProfile: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/uploads/{subDir}/{filename}")
    public ResponseEntity<org.springframework.core.io.Resource> getFile(@PathVariable String subDir, @PathVariable String filename) {
        try {
            String safeSubDir = java.nio.file.Paths.get(subDir).getFileName().toString();
            String safeFilename = java.nio.file.Paths.get(filename).getFileName().toString();
            java.nio.file.Path basePath = java.nio.file.Paths.get(uploadDir).toAbsolutePath().normalize();
            java.nio.file.Path path = basePath.resolve(safeSubDir).resolve(safeFilename).normalize();
            if (!path.startsWith(basePath) || !java.nio.file.Files.exists(path)) {
                return ResponseEntity.notFound().build();
            }
            org.springframework.core.io.Resource resource = new org.springframework.core.io.UrlResource(path.toUri());
            return ResponseEntity.ok()
                    .contentType(org.springframework.http.MediaTypeFactory.getMediaType(resource)
                            .orElse(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM))
                    .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/kyc/status")
    public ResponseEntity<?> getKycStatus(
            @Parameter(hidden = true) @RequestHeader("Authorization") String token) {
        try {
            String userIdStr = jwtUtil.extractUserId(token);
            if (userIdStr == null) {
                return ResponseEntity.status(401).body("Error: Invalid token or missing User ID");
            }
            UUID userId = UUID.fromString(userIdStr);
            String email = jwtUtil.extractEmail(token);
            String role = jwtUtil.extractRole(token);
            return ResponseEntity.ok(userService.getKycStatus(userId, email, role));
        } catch (RuntimeException e) {
            if (String.valueOf(e.getMessage()).contains("KYC details not found")) {
                return ResponseEntity.ok(Map.of(
                        "status", "NOT_SUBMITTED",
                        "rejectionReason", "",
                        "documentType", "",
                        "documentNumber", "",
                        "documentUrl", ""));
            }
            return ResponseEntity.status(404).body("Error: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("System Error: " + e.getMessage());
        }
    }
    @DeleteMapping("/{userId}")
    public ResponseEntity<?> deleteUser(@PathVariable UUID userId) {
        try {
            userService.deleteUser(userId);
            return ResponseEntity.ok("User deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error deleting user: " + e.getMessage());
        }
    }

    @GetMapping("/{userId}/kyc-history")
    public ResponseEntity<?> getKycHistory(@PathVariable UUID userId) {
        return ResponseEntity.ok(userService.getKycHistory(userId));
    }

    @PutMapping("/{userId}/kyc/update")
    public ResponseEntity<?> updateKyc(@PathVariable UUID userId, @RequestParam String status, @RequestParam(required = false) String reason) {
        try {
            return ResponseEntity.ok(userService.updateKycStatus(userId, status, reason));
        } catch (Exception e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }
}
