package com.wallet.core;

import com.wallet.core.config.OpenApiConfig;
import com.wallet.core.dto.TopUpRequest;
import com.wallet.core.dto.TransferRequest;
import com.wallet.core.dto.WithdrawRequest;
import com.wallet.core.entity.WalletAccount;
import com.wallet.core.exception.GlobalExceptionHandler;
import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

class SupportClassesTest {

    @Test
    void openApiConfig_BuildsApiDefinition() {
        OpenAPI openAPI = new OpenApiConfig().customOpenAPI();

        assertEquals("Wallet Service API", openAPI.getInfo().getTitle());
        assertEquals("http://localhost:8090", openAPI.getServers().get(0).getUrl());
        assertNotNull(openAPI.getComponents().getSecuritySchemes().get("bearerAuth"));
    }

    @Test
    void globalExceptionHandler_ReturnsValidationAndGeneralErrors() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "wallet");
        bindingResult.addError(new FieldError("wallet", "amount", "required"));

        MethodArgumentNotValidException validationException =
                new MethodArgumentNotValidException(mock(MethodParameter.class), bindingResult);
        ResponseEntity<Map<String, String>> validationResponse = handler.handleValidationExceptions(validationException);
        ResponseEntity<String> generalResponse = handler.handleGeneralException(new RuntimeException("boom"));

        assertEquals(HttpStatus.BAD_REQUEST, validationResponse.getStatusCode());
        assertEquals("required", validationResponse.getBody().get("amount"));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, generalResponse.getStatusCode());
        assertEquals("boom", generalResponse.getBody());
    }

    @Test
    void walletAccount_GettersAndConstructor() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        WalletAccount account = new WalletAccount();
        account.setId(id);
        account.setUserId(userId);
        account.setCachedBalance(new BigDecimal("99.99"));
        account.setStatus("INACTIVE");

        assertEquals(id, account.getId());
        assertEquals(userId, account.getUserId());
        assertEquals(new BigDecimal("99.99"), account.getCachedBalance());
        assertEquals("INACTIVE", account.getStatus());

        WalletAccount fromCtor = new WalletAccount(id, userId, new BigDecimal("10.00"), "ACTIVE");
        assertEquals("ACTIVE", fromCtor.getStatus());
    }

    @Test
    void requestDtos_GettersAndConstructors() {
        UUID userId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();

        TopUpRequest topUpRequest = new TopUpRequest();
        topUpRequest.setUserId(userId);
        topUpRequest.setAmount(new BigDecimal("10.00"));
        topUpRequest.setPaymentMethod("UPI");
        assertEquals(userId, topUpRequest.getUserId());
        assertEquals("UPI", topUpRequest.getPaymentMethod());
        assertEquals(new BigDecimal("10.00"), topUpRequest.getAmount());
        assertEquals("CARD", new TopUpRequest(userId, new BigDecimal("1.00"), "CARD").getPaymentMethod());

        TransferRequest transferRequest = new TransferRequest();
        transferRequest.setTargetUserId(targetUserId);
        transferRequest.setAmount(new BigDecimal("5.00"));
        transferRequest.setNotes("Dinner");
        assertEquals(targetUserId, transferRequest.getTargetUserId());
        assertEquals("Dinner", transferRequest.getNotes());
        assertEquals(new BigDecimal("5.00"), transferRequest.getAmount());
        assertEquals("Rent", new TransferRequest(targetUserId, new BigDecimal("2.00"), "Rent").getNotes());

        WithdrawRequest withdrawRequest = new WithdrawRequest();
        withdrawRequest.setAmount(new BigDecimal("3.00"));
        withdrawRequest.setNotes("ATM");
        assertEquals(new BigDecimal("3.00"), withdrawRequest.getAmount());
        assertEquals("ATM", withdrawRequest.getNotes());
        assertEquals(new BigDecimal("4.00"), new WithdrawRequest(new BigDecimal("4.00")).getAmount());
    }
}
