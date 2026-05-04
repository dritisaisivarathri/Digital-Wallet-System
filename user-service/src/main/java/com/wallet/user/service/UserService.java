package com.wallet.user.service;

import com.wallet.user.dto.KycSubmitRequest;
import com.wallet.user.dto.UserProfileDto;
import com.wallet.user.entity.KycDetails;
import com.wallet.user.entity.KycHistory;
import com.wallet.user.entity.User;
import com.wallet.user.repository.KycRepository;
import com.wallet.user.repository.UserRepository;
import com.wallet.user.repository.KycHistoryRepository;
import com.wallet.user.filter.InternalSecurityFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private static final long MAX_PROFILE_PHOTO_BYTES = 5L * 1024 * 1024;

    @Autowired
    private KycRepository kycRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private KycHistoryRepository kycHistoryRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${services.auth-service.internal-base-url:http://auth-service:8081}")
    private String authServiceBaseUrl;

    @Value("${upload.dir:uploads}")
    private String uploadDir;

    @jakarta.annotation.PostConstruct
    public void init() {
        try {
            Files.createDirectories(Paths.get(uploadDir));
            System.out.println("DEBUG: Upload directory initialized at " + Paths.get(uploadDir).toAbsolutePath());
        } catch (IOException e) {
            System.err.println("DEBUG: Failed to create upload directory: " + e.getMessage());
        }
    }

    public Optional<com.wallet.user.entity.User> findById(UUID userId) {
        return userRepository.findById(userId);
    }

    public String saveFile(MultipartFile file, String subDir) throws IOException {
        System.out.println("DEBUG: saveFile starting for " + file.getOriginalFilename() + " in " + subDir);
        Path root = Paths.get(uploadDir, subDir);
        if (!Files.exists(root)) {
            System.out.println("DEBUG: creating directory " + root.toAbsolutePath());
            Files.createDirectories(root);
        }
        String originalName = Optional.ofNullable(file.getOriginalFilename()).orElse("upload");
        String safeName = Paths.get(originalName).getFileName().toString().replaceAll("[^a-zA-Z0-9._-]", "_");
        String filename = UUID.randomUUID().toString() + "_" + safeName;
        Path destination = root.resolve(filename);
        System.out.println("DEBUG: copying file to " + destination.toAbsolutePath());
        Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
        System.out.println("DEBUG: saveFile completed. Path: " + "/" + subDir + "/" + filename);
        return "/" + subDir + "/" + filename;
    }

    @Transactional(readOnly = true)
    public UserProfileDto getProfile(UUID userId, String email, String role, String fullName, String phoneNumber) {
        User user = findOrProvisionUser(userId, email, role, fullName, phoneNumber);
        return toProfileDto(user);
    }

    @Transactional
    public UserProfileDto updateProfile(
            UUID userId,
            String email,
            String role,
            String fallbackFullName,
            String fallbackPhoneNumber,
            String fullName,
            String phoneNumber,
            MultipartFile photo) throws IOException {
        User user = findOrProvisionUser(userId, email, role, fallbackFullName, fallbackPhoneNumber);

        if (fullName != null && !fullName.isBlank()) {
            user.setFullName(fullName.trim());
        }
        if (phoneNumber != null && !phoneNumber.isBlank()) {
            user.setPhoneNumber(phoneNumber.trim());
        }

        if (photo != null && !photo.isEmpty()) {
            validateProfilePhoto(photo);
            String photoUrl = saveFile(photo, "profiles");
            user.setProfileImageUrl(photoUrl);
        }

        return toProfileDto(userRepository.save(user));
    }

    @Transactional
    public String submitKyc(UUID userId, String email, String role, KycSubmitRequest request) {
        // Fallback for legacy calls if any, though we should use the file version
        if (request.getDocumentUrl() != null && (request.getDocumentUrl().startsWith("http") || request.getDocumentUrl().startsWith("www"))) {
            throw new RuntimeException("Links are no longer accepted for KYC. Please upload a .pdf or .docx file.");
        }
        return submitKycInternal(userId, email, request.getDocumentType(), request.getDocumentNumber(), request.getDocumentUrl());
    }

    @Transactional
    public String submitKyc(UUID userId, String email, String documentType, String documentNumber, MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename();
        if (filename == null || (!filename.toLowerCase().endsWith(".pdf") && !filename.toLowerCase().endsWith(".docx"))) {
            throw new RuntimeException("Only .pdf and .docx files are allowed");
        }

        String fileUrl = saveFile(file, "kyc");
        return submitKycInternal(userId, email, documentType, documentNumber, fileUrl);
    }

    private String submitKycInternal(UUID userId, String email, String documentType, String documentNumber, String fileUrl) {
        System.out.println("DEBUG: SubmitKYC called for UserID: " + userId + ", Email: " + email);
        
        Optional<KycDetails> existingKyc = kycRepository.findByUserId(userId);
        if (existingKyc.isPresent() && existingKyc.get().getStatus().equals("APPROVED")) {
            throw new RuntimeException("KYC is already approved");
        }

        KycDetails kyc = existingKyc.orElse(new KycDetails());
        String oldStatus = kyc.getStatus();
        kyc.setUserId(userId);
        kyc.setEmail(email); 
        kyc.setDocumentType(documentType);
        kyc.setDocumentNumber(documentNumber);
        kyc.setDocumentUrl(fileUrl);
        kyc.setStatus("PENDING");
        kycRepository.save(kyc);

        // Record History
        kycHistoryRepository.save(new KycHistory(userId, oldStatus, "PENDING", "Submission update"));

        return "KYC details submitted successfully and pending approval";
    }

    public KycDetails getKycStatus(UUID userId, String email, String role) {
        return kycRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("KYC details not found"));
    }

    public List<KycDetails> getPendingKycs() {
        logger.info("Admin requested pending KYC queue. Checking database status...");
        
        // Debug: Log all items in the kyc_details table to see what's actually there
        long totalCount = kycRepository.count();
        List<KycDetails> pending = kycRepository.findByStatus("PENDING");
        
        logger.info("KYC Stats - Total Records: {}, Found Pending: {}", totalCount, pending.size());
        
        if (pending.isEmpty() && totalCount > 0) {
            logger.warn("Pending queue is empty but total records exist. Statuses found in DB: {}", 
                kycRepository.findAll().stream().map(KycDetails::getStatus).distinct().toList());
        }
        
        return pending;
    }

    @Transactional
    public KycDetails updateKycStatus(UUID userId, String status, String rejectionReason) {
        System.out.println("DEBUG: updateKycStatus called for UserID: " + userId + ", NewStatus: " + status + ", Reason: " + rejectionReason);
        KycDetails kyc = kycRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("KYC submission not found for ID: " + userId));

        if (kyc.getEmail() == null || kyc.getEmail().isBlank()) {
            userRepository.findById(userId).ifPresent(user -> kyc.setEmail(user.getEmail()));
        }
        
        String oldStatus = kyc.getStatus();
        kyc.setStatus(status);
        kyc.setRejectionReason(rejectionReason);
        kyc.setProcessedAt(java.time.LocalDateTime.now());
        kycRepository.save(kyc);
        kycRepository.flush();

        // Record History
        kycHistoryRepository.save(new KycHistory(userId, oldStatus, status, rejectionReason));

        // Determine global status
        String newStatus = "PENDING_KYC";
        if ("APPROVED".equals(status)) {
            newStatus = "ACTIVE";
        } else if ("REJECTED".equals(status)) {
            newStatus = "REJECTED";
        }

        // Notify Auth Service (Single Source of Truth)
        try {
            String authServiceUrl = authServiceBaseUrl + "/api/auth/internal/users/" + userId + "/status?status="
                    + newStatus;
            restTemplate.postForEntity(authServiceUrl, new HttpEntity<>(internalHeaders()), String.class);
            System.out.println("Synchronized status " + newStatus + " with Auth Service for UserID: " + userId);
        } catch (Exception e) {
            System.err.println("Failed to sync status with Auth Service: " + e.getMessage());
        }

        return kyc;
    }

    @Transactional
    public void deleteUser(UUID userId) {
        com.wallet.user.entity.User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        
        // Soft delete: update status to INACTIVE
        user.setStatus("INACTIVE");
        userRepository.save(user);
        
        // Record in KYC history or audit log if needed
        kycHistoryRepository.save(new KycHistory(userId, "N/A", "INACTIVE", "Soft deleted by user/admin"));

        // Notify Auth Service to update status to INACTIVE
        try {
            String authServiceUrl = authServiceBaseUrl + "/api/auth/internal/users/" + userId + "/status?status=INACTIVE";
            restTemplate.postForEntity(authServiceUrl, new HttpEntity<>(internalHeaders()), String.class);
            System.out.println("Synchronized soft-delete status INACTIVE with Auth Service for UserID: " + userId);
        } catch (Exception e) {
            System.err.println("Failed to sync soft-delete status with Auth Service: " + e.getMessage());
        }
    }

    public List<com.wallet.user.entity.User> findAll() {
        return userRepository.findAll();
    }

    public List<KycHistory> getKycHistory(UUID userId) {
        return kycHistoryRepository.findByUserIdOrderByChangedAtDesc(userId);
    }

    private HttpHeaders internalHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(InternalSecurityFilter.INTERNAL_SECRET_HEADER, InternalSecurityFilter.INTERNAL_SECRET_VALUE);
        return headers;
    }

    private User findOrProvisionUser(
            UUID userId,
            String email,
            String role,
            String fallbackFullName,
            String fallbackPhoneNumber) {
        Optional<User> existingUser = userRepository.findById(userId);
        if (existingUser.isPresent()) {
            return existingUser.get();
        }

        if (email != null && !email.isBlank()) {
            Optional<User> userByEmail = userRepository.findByEmail(email.trim());
            if (userByEmail.isPresent()) {
                return userByEmail.get();
            }
        }

        User user = new User();
        user.setId(userId);
        user.setEmail(email == null ? null : email.trim());
        user.setRole(normalizeRole(role));
        user.setStatus("PENDING_KYC");
        user.setFullName(blankToNull(fallbackFullName));
        user.setPhoneNumber(blankToNull(fallbackPhoneNumber));
        return userRepository.save(user);
    }

    private void validateProfilePhoto(MultipartFile photo) {
        String contentType = photo.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new RuntimeException("Only image files are allowed for profile photos");
        }
        if (photo.getSize() > MAX_PROFILE_PHOTO_BYTES) {
            throw new RuntimeException("Profile photo must be 5 MB or smaller");
        }
    }

    private UserProfileDto toProfileDto(User user) {
        return new UserProfileDto(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getPhoneNumber(),
                user.getRole(),
                user.getStatus(),
                user.getProfileImageUrl());
    }

    private String normalizeRole(String role) {
        return blankToNull(role) == null ? "USER" : role.trim().toUpperCase(Locale.ROOT);
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
