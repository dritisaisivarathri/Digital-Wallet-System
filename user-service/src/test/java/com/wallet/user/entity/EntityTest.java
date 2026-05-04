package com.wallet.user.entity;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class EntityTest {

    @Test
    void testUserEntity() {
        User user = new User();
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        
        user.setId(id);
        user.setEmail("test@test.com");
        user.setPassword("pass");
        user.setRole("USER");
        user.setStatus("ACTIVE");
        user.setFullName("Full Name");
        user.setPhoneNumber("1234567890");
        user.setCreatedAt(now);

        assertEquals(id, user.getId());
        assertEquals("test@test.com", user.getEmail());
        assertEquals("pass", user.getPassword());
        assertEquals("USER", user.getRole());
        assertEquals("ACTIVE", user.getStatus());
        assertEquals("Full Name", user.getFullName());
        assertEquals("1234567890", user.getPhoneNumber());
        assertEquals(now, user.getCreatedAt());

        User user2 = new User(id, "e", "p", "r", "s", "f", "ph", now);
        assertEquals("f", user2.getFullName());
    }

    @Test
    void testKycDetailsEntity() {
        KycDetails kyc = new KycDetails();
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        kyc.setId(id);
        kyc.setUserId(userId);
        kyc.setEmail("test@test.com");
        kyc.setDocumentType("PASSPORT");
        kyc.setDocumentNumber("P123");
        kyc.setDocumentUrl("url");
        kyc.setStatus("PENDING");
        kyc.setRejectionReason("none");
        kyc.setSubmittedAt(now);
        kyc.setProcessedAt(now);

        assertEquals(id, kyc.getId());
        assertEquals(userId, kyc.getUserId());
        assertEquals("test@test.com", kyc.getEmail());
        assertEquals("PASSPORT", kyc.getDocumentType());
        assertEquals("P123", kyc.getDocumentNumber());
        assertEquals("url", kyc.getDocumentUrl());
        assertEquals("PENDING", kyc.getStatus());
        assertEquals("none", kyc.getRejectionReason());
        assertEquals(now, kyc.getSubmittedAt());
        assertEquals(now, kyc.getProcessedAt());

        KycDetails kyc2 = new KycDetails(id, userId, "e", "dt", "dn", "du", "s", "rr", now, now);
        assertEquals("dt", kyc2.getDocumentType());
    }
}
