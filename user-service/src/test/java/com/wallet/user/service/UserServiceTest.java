package com.wallet.user.service;

import com.wallet.user.dto.KycSubmitRequest;
import com.wallet.user.entity.KycDetails;
import com.wallet.user.repository.KycRepository;
import com.wallet.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private KycRepository kycRepository;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private UserRepository userRepository;

    @Mock
    private com.wallet.user.repository.KycHistoryRepository kycHistoryRepository;

    @InjectMocks
    private UserService userService;

    private UUID userId;
    private KycSubmitRequest kycSubmitRequest;
    private KycDetails kycDetails;

    @BeforeEach
    void setUp() throws IOException {
        userId = UUID.randomUUID();
        kycSubmitRequest = new KycSubmitRequest("PASSPORT", "ABC12345", "http://docs.com/123");
        kycDetails = new KycDetails();
        kycDetails.setUserId(userId);
        kycDetails.setStatus("PENDING");
        Path tempUploadDir = Files.createTempDirectory("user-service-test-uploads");
        ReflectionTestUtils.setField(userService, "uploadDir", tempUploadDir.toString());
    }

    @Test
    void findById_Success() {
        com.wallet.user.entity.User user = new com.wallet.user.entity.User();
        user.setId(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        Optional<com.wallet.user.entity.User> result = userService.findById(userId);

        assertTrue(result.isPresent());
        assertEquals(userId, result.get().getId());
    }

    @Test
    void submitKyc_Success() {
        when(kycRepository.findByUserId(userId)).thenReturn(Optional.empty());
        MockMultipartFile file = new MockMultipartFile("file", "document.pdf", "application/pdf", "pdf".getBytes());

        String result = assertDoesNotThrow(() -> userService.submitKyc(userId, "test@example.com", "PASSPORT", "ABC12345", file));

        assertEquals("KYC details submitted successfully and pending approval", result);
        verify(kycRepository, times(1)).save(any(KycDetails.class));
        verify(kycHistoryRepository, times(1)).save(any());
    }

    @Test
    void submitKyc_AlreadyApproved() {
        kycDetails.setStatus("APPROVED");
        when(kycRepository.findByUserId(userId)).thenReturn(Optional.of(kycDetails));
        MockMultipartFile file = new MockMultipartFile("file", "document.pdf", "application/pdf", "pdf".getBytes());

        assertThrows(RuntimeException.class, () -> userService.submitKyc(userId, "test@example.com", "PASSPORT", "ABC12345", file));
    }

    @Test
    void submitKyc_ExistingPending_Success() {
        kycDetails.setStatus("PENDING");
        kycDetails.setDocumentType("OLD_TYPE");
        when(kycRepository.findByUserId(userId)).thenReturn(Optional.of(kycDetails));
        MockMultipartFile file = new MockMultipartFile("file", "document.pdf", "application/pdf", "pdf".getBytes());

        String result = assertDoesNotThrow(() -> userService.submitKyc(userId, "test@example.com", "PASSPORT", "ABC12345", file));

        assertEquals("KYC details submitted successfully and pending approval", result);
        assertEquals("PASSPORT", kycDetails.getDocumentType()); // Should be updated
        verify(kycRepository, times(1)).save(kycDetails);
        verify(kycHistoryRepository, times(1)).save(any());
    }

    @Test
    void getKycStatus_Success() {
        when(kycRepository.findByUserId(userId)).thenReturn(Optional.of(kycDetails));

        KycDetails result = userService.getKycStatus(userId, "test@example.com", "USER");

        assertNotNull(result);
        assertEquals(userId, result.getUserId());
    }

    @Test
    void getKycStatus_NotFound() {
        when(kycRepository.findByUserId(userId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> userService.getKycStatus(userId, "test@example.com", "USER"));
    }

    @Test
    void getPendingKycs_Success() {
        when(kycRepository.findByStatus("PENDING")).thenReturn(List.of(kycDetails));

        List<KycDetails> result = userService.getPendingKycs();

        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
    }

    @Test
    void updateKycStatus_Approved_Success() {
        when(kycRepository.findByUserId(userId)).thenReturn(Optional.of(kycDetails));

        userService.updateKycStatus(userId, "APPROVED", null);

        assertEquals("APPROVED", kycDetails.getStatus());
        verify(kycRepository, times(1)).save(kycDetails);
        verify(kycHistoryRepository, times(1)).save(any());
        verify(restTemplate, times(1)).postForEntity(contains("ACTIVE"), any(), eq(String.class));
    }

    @Test
    void updateKycStatus_Rejected_Success() {
        when(kycRepository.findByUserId(userId)).thenReturn(Optional.of(kycDetails));

        userService.updateKycStatus(userId, "REJECTED", "Documents unclear");

        assertEquals("REJECTED", kycDetails.getStatus());
        assertEquals("Documents unclear", kycDetails.getRejectionReason());
        verify(kycHistoryRepository, times(1)).save(any());
        verify(restTemplate, times(1)).postForEntity(contains("REJECTED"), any(), eq(String.class));
    }

    @Test
    void updateKycStatus_NotFound() {
        when(kycRepository.findByUserId(userId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> userService.updateKycStatus(userId, "APPROVED", null));
    }

    @Test
    void updateKycStatus_SyncAuthService_Failure() {
        when(kycRepository.findByUserId(userId)).thenReturn(Optional.of(kycDetails));
        when(restTemplate.postForEntity(anyString(), any(), eq(String.class))).thenThrow(new RuntimeException("Service down"));

        // Should not throw exception, just log it
        assertDoesNotThrow(() -> userService.updateKycStatus(userId, "APPROVED", null));
        
        verify(kycRepository).save(kycDetails);
        verify(kycHistoryRepository).save(any());
    }

    @Test
    void updateKycStatus_BackfillsMissingEmail_FromUserRecord() {
        com.wallet.user.entity.User user = new com.wallet.user.entity.User();
        user.setId(userId);
        user.setEmail("registered@gmail.com");
        kycDetails.setEmail(" ");

        when(kycRepository.findByUserId(userId)).thenReturn(Optional.of(kycDetails));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        KycDetails result = userService.updateKycStatus(userId, "APPROVED", null);

        assertEquals("registered@gmail.com", result.getEmail());
        verify(userRepository, times(1)).findById(userId);
        verify(kycHistoryRepository, times(1)).save(any());
    }

    @Test
    void deleteUser_Success() {
        com.wallet.user.entity.User user = new com.wallet.user.entity.User();
        user.setId(userId);
        user.setStatus("ACTIVE");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        userService.deleteUser(userId);

        assertEquals("INACTIVE", user.getStatus());
        verify(userRepository).save(user);
        verify(kycHistoryRepository).save(any());
        verify(restTemplate).postForEntity(contains("status=INACTIVE"), any(), eq(String.class));
    }

    @Test
    void getKycHistory_Success() {
        when(kycHistoryRepository.findByUserIdOrderByChangedAtDesc(userId)).thenReturn(List.of());
        
        List<com.wallet.user.entity.KycHistory> result = userService.getKycHistory(userId);
        
        assertNotNull(result);
        verify(kycHistoryRepository).findByUserIdOrderByChangedAtDesc(userId);
    }

    @Test
    void updateProfile_ProvisionMissingUserAndSavePhoto_Success() throws Exception {
        MockMultipartFile photo = new MockMultipartFile("photo", "avatar.png", "image/png", "png".getBytes());

        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(com.wallet.user.entity.User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        com.wallet.user.dto.UserProfileDto result = userService.updateProfile(
                userId,
                "user@example.com",
                "USER",
                "Token User",
                "9876543210",
                "Updated User",
                "9999999999",
                photo);

        assertEquals(userId, result.getId());
        assertEquals("user@example.com", result.getEmail());
        assertEquals("Updated User", result.getFullName());
        assertEquals("9999999999", result.getPhoneNumber());
        assertNotNull(result.getProfileImageUrl());
        verify(userRepository, atLeastOnce()).save(any(com.wallet.user.entity.User.class));
    }
}

