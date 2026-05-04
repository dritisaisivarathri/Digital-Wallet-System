package com.wallet.admin;

import com.wallet.admin.config.OpenApiConfig;
import com.wallet.admin.entity.Campaign;
import com.wallet.admin.exception.GlobalExceptionHandler;
import com.wallet.common.dto.KycNotificationEvent;
import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

class SupportClassesTest {

    @Test
    void openApiConfig_BuildsApiDefinition() {
        OpenAPI openAPI = new OpenApiConfig().customOpenAPI();

        assertEquals("Admin Service API", openAPI.getInfo().getTitle());
        assertEquals("http://localhost:8090", openAPI.getServers().get(0).getUrl());
        assertNotNull(openAPI.getComponents().getSecuritySchemes().get("bearerAuth"));
    }

    @Test
    void globalExceptionHandler_ReturnsValidationAndGeneralErrors() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "campaign");
        bindingResult.addError(new FieldError("campaign", "name", "required"));

        MethodArgumentNotValidException validationException =
                new MethodArgumentNotValidException(mock(MethodParameter.class), bindingResult);
        ResponseEntity<Map<String, String>> validationResponse = handler.handleValidationExceptions(validationException);
        ResponseEntity<String> generalResponse = handler.handleGeneralException(new RuntimeException("boom"));

        assertEquals(HttpStatus.BAD_REQUEST, validationResponse.getStatusCode());
        assertEquals("required", validationResponse.getBody().get("name"));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, generalResponse.getStatusCode());
        assertEquals("boom", generalResponse.getBody());
    }

    @Test
    void campaign_GettersAndConstructor() {
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        Campaign campaign = new Campaign();
        campaign.setId(id);
        campaign.setName("Promo");
        campaign.setTargetTier("GOLD");
        campaign.setStatus("ACTIVE");
        campaign.setCreatedAt(now);

        assertEquals(id, campaign.getId());
        assertEquals("Promo", campaign.getName());
        assertEquals("GOLD", campaign.getTargetTier());
        assertEquals("ACTIVE", campaign.getStatus());
        assertEquals(now, campaign.getCreatedAt());

        Campaign fromCtor = new Campaign(id, "Promo", "PLATINUM", "INACTIVE", now);
        assertEquals("PLATINUM", fromCtor.getTargetTier());
    }

    @Test
    void kycNotificationEvent_GettersAndConstructor() {
        UUID userId = UUID.randomUUID();
        KycNotificationEvent event = new KycNotificationEvent();
        event.setUserId(userId);
        event.setUserEmail("user@test.com");
        event.setStatus("APPROVED");
        event.setReason("ok");
        event.setType("KYC_UPDATE");

        assertEquals(userId, event.getUserId());
        assertEquals("user@test.com", event.getUserEmail());
        assertEquals("APPROVED", event.getStatus());
        assertEquals("ok", event.getReason());
        assertEquals("KYC_UPDATE", event.getType());

        KycNotificationEvent fromCtor = new KycNotificationEvent(userId, "user@test.com", "admin@test.com", "REJECTED", "bad", "KYC_UPDATE");
        assertEquals("REJECTED", fromCtor.getStatus());
    }
}
