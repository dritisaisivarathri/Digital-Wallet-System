package com.wallet.rewards;

import com.wallet.rewards.config.OpenApiConfig;
import com.wallet.rewards.entity.RewardCatalog;
import com.wallet.rewards.entity.RewardPoints;
import com.wallet.rewards.exception.GlobalExceptionHandler;
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

        assertEquals("Rewards Service API", openAPI.getInfo().getTitle());
        assertEquals("http://localhost:8090", openAPI.getServers().get(0).getUrl());
        assertNotNull(openAPI.getComponents().getSecuritySchemes().get("bearerAuth"));
    }

    @Test
    void globalExceptionHandler_ReturnsValidationAndGeneralErrors() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "reward");
        bindingResult.addError(new FieldError("reward", "name", "required"));

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
    void rewardCatalog_GettersAndConstructor() {
        UUID id = UUID.randomUUID();
        RewardCatalog catalog = new RewardCatalog();
        catalog.setId(id);
        catalog.setName("Gift Card");
        catalog.setDescription("Coffee");
        catalog.setCostInPoints(100);
        catalog.setStockQuantity(5);
        catalog.setRequiredTier("ALL");

        assertEquals(id, catalog.getId());
        assertEquals("Gift Card", catalog.getName());
        assertEquals("Coffee", catalog.getDescription());
        assertEquals(100, catalog.getCostInPoints());
        assertEquals(5, catalog.getStockQuantity());
        assertEquals("ALL", catalog.getRequiredTier());

        RewardCatalog fromCtor = new RewardCatalog(id, "Voucher", "Desc", 200, 2, "GOLD");
        assertEquals("Voucher", fromCtor.getName());
    }

    @Test
    void rewardPoints_GettersConstructorsAndTierTransitions() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        RewardPoints points = new RewardPoints(id, userId, 900, "SILVER", now);
        assertEquals(id, points.getId());
        assertEquals(userId, points.getUserId());
        assertEquals(900, points.getTotalPoints());
        assertEquals("SILVER", points.getTier());
        assertEquals(now, points.getLastUpdated());

        points.addPoints(200);
        assertEquals("GOLD", points.getTier());

        points.addPoints(4000);
        assertEquals("PLATINUM", points.getTier());

        points.deductPoints(5000);
        assertEquals("SILVER", points.getTier());
    }
}
