package com.wallet.tx;

import com.wallet.tx.config.OpenApiConfig;
import com.wallet.tx.entity.LedgerEntry;
import com.wallet.tx.entity.Transaction;
import com.wallet.tx.exception.GlobalExceptionHandler;
import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.math.BigDecimal;
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

        assertEquals("Transaction Service API", openAPI.getInfo().getTitle());
        assertEquals("http://localhost:8090", openAPI.getServers().get(0).getUrl());
        assertNotNull(openAPI.getComponents().getSecuritySchemes().get("bearerAuth"));
    }

    @Test
    void globalExceptionHandler_ReturnsValidationAndGeneralErrors() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "tx");
        bindingResult.addError(new FieldError("tx", "amount", "required"));

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
    void transaction_GettersAndConstructor() {
        UUID id = UUID.randomUUID();
        UUID fromUserId = UUID.randomUUID();
        UUID toUserId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        Transaction transaction = new Transaction();
        transaction.setId(id);
        transaction.setFromUserId(fromUserId);
        transaction.setToUserId(toUserId);
        transaction.setAmount(new BigDecimal("12.34"));
        transaction.setType("TRANSFER");
        transaction.setStatus("COMPLETED");
        transaction.setTimestamp(now);
        transaction.setReferenceNotes("Lunch");

        assertEquals(id, transaction.getId());
        assertEquals(fromUserId, transaction.getFromUserId());
        assertEquals(toUserId, transaction.getToUserId());
        assertEquals(new BigDecimal("12.34"), transaction.getAmount());
        assertEquals("TRANSFER", transaction.getType());
        assertEquals("COMPLETED", transaction.getStatus());
        assertEquals(now, transaction.getTimestamp());
        assertEquals("Lunch", transaction.getReferenceNotes());

        Transaction fromCtor = new Transaction(id, fromUserId, toUserId, new BigDecimal("1.00"), "TOPUP", "PENDING", now, "n");
        assertEquals("TOPUP", fromCtor.getType());
    }

    @Test
    void ledgerEntry_GettersAndConstructor() {
        UUID id = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        LedgerEntry entry = new LedgerEntry();
        entry.setId(id);
        entry.setAccountId(accountId);
        entry.setTransactionId(transactionId);
        entry.setType("CREDIT");
        entry.setAmount(new BigDecimal("55.00"));
        entry.setCreatedAt(now);
        entry.setDescription("Topup");

        assertEquals(id, entry.getId());
        assertEquals(accountId, entry.getAccountId());
        assertEquals(transactionId, entry.getTransactionId());
        assertEquals("CREDIT", entry.getType());
        assertEquals(new BigDecimal("55.00"), entry.getAmount());
        assertEquals(now, entry.getCreatedAt());
        assertEquals("Topup", entry.getDescription());

        LedgerEntry fromCtor = new LedgerEntry(id, accountId, transactionId, "DEBIT", new BigDecimal("1.00"), now, "d");
        assertEquals("DEBIT", fromCtor.getType());
    }
}
