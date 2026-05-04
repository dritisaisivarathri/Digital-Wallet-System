package com.wallet.user.dto;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class DtoTest {

    @Test
    void testUserProfileDto() {
        UUID id = UUID.randomUUID();
        UserProfileDto dto = new UserProfileDto();
        dto.setId(id);
        dto.setEmail("test@test.com");
        dto.setFullName("Full Name");
        dto.setPhoneNumber("1234567890");
        dto.setRole("USER");
        dto.setStatus("ACTIVE");

        assertEquals(id, dto.getId());
        assertEquals("test@test.com", dto.getEmail());
        assertEquals("Full Name", dto.getFullName());
        assertEquals("1234567890", dto.getPhoneNumber());
        assertEquals("USER", dto.getRole());
        assertEquals("ACTIVE", dto.getStatus());

        UserProfileDto dto2 = new UserProfileDto(id, "e", "f", "p", "r", "s");
        assertEquals("f", dto2.getFullName());
    }

    @Test
    void testKycSubmitRequest() {
        KycSubmitRequest dto = new KycSubmitRequest();
        dto.setDocumentType("PASSPORT");
        dto.setDocumentNumber("P123");
        dto.setDocumentUrl("url");

        assertEquals("PASSPORT", dto.getDocumentType());
        assertEquals("P123", dto.getDocumentNumber());
        assertEquals("url", dto.getDocumentUrl());

        KycSubmitRequest dto2 = new KycSubmitRequest("dt", "dn", "du");
        assertEquals("dt", dto2.getDocumentType());
    }
}
