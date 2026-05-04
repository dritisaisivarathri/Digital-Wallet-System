package com.wallet.notification;

import com.wallet.common.dto.KycNotificationEvent;
import com.wallet.notification.config.OpenApiConfig;
import com.wallet.notification.dto.ManualNotificationRequest;
import com.wallet.notification.entity.NotificationHistory;
import com.wallet.notification.exception.GlobalExceptionHandler;
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
    void notificationHistory_GettersAndConstructor() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        NotificationHistory history = new NotificationHistory();
        history.setId(id);
        history.setUserId(userId);
        history.setTopic("topic");
        history.setMessage("message");
        history.setSentAt(now);
        history.setType("SMS");

        assertEquals(id, history.getId());
        assertEquals(userId, history.getUserId());
        assertEquals("topic", history.getTopic());
        assertEquals("message", history.getMessage());
        assertEquals(now, history.getSentAt());
        assertEquals("SMS", history.getType());

        NotificationHistory fromCtor = new NotificationHistory(id, userId, "topic", "message", now, "EMAIL");
        assertEquals("EMAIL", fromCtor.getType());
    }

    @Test
    void manualNotificationRequest_GettersAndConstructor() {
        UUID userId = UUID.randomUUID();
        ManualNotificationRequest request = new ManualNotificationRequest();
        request.setUserId(userId);
        request.setMessage("hello");
        request.setTopic("ops");

        assertEquals(userId, request.getUserId());
        assertEquals("hello", request.getMessage());
        assertEquals("ops", request.getTopic());

        ManualNotificationRequest fromCtor = new ManualNotificationRequest(userId, "hello", "ops");
        assertEquals("hello", fromCtor.getMessage());
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

    @Test
    void openApiConfig_BuildsApiDefinition() {
        OpenAPI openAPI = new OpenApiConfig().customOpenAPI();

        assertEquals("Notification Service API", openAPI.getInfo().getTitle());
        assertEquals("http://localhost:8090", openAPI.getServers().get(0).getUrl());
        assertNotNull(openAPI.getComponents().getSecuritySchemes().get("bearerAuth"));
    }

    @Test
    void globalExceptionHandler_ReturnsValidationErrorsAndGeneralError() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "message", "required"));

        MethodArgumentNotValidException validationException =
                new MethodArgumentNotValidException(mock(MethodParameter.class), bindingResult);
        ResponseEntity<Map<String, String>> validationResponse = handler.handleValidationExceptions(validationException);
        ResponseEntity<String> errorResponse = handler.handleGeneralException(new RuntimeException("boom"));

        assertEquals(HttpStatus.BAD_REQUEST, validationResponse.getStatusCode());
        assertEquals("required", validationResponse.getBody().get("message"));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, errorResponse.getStatusCode());
        assertEquals("boom", errorResponse.getBody());
    }
}
