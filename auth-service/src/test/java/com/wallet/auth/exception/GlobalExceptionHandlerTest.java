package com.wallet.auth.exception;

import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

    @Test
    void handleValidationExceptions_ReturnsFieldErrors() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "object");
        bindingResult.addError(new FieldError("object", "email", "must be a valid email"));
        bindingResult.addError(new FieldError("object", "username", "cannot be empty"));
        
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(
                mock(MethodParameter.class), bindingResult);
                
        ResponseEntity<Map<String, String>> response = handler.handleValidationExceptions(ex);
        
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, String> body = response.getBody();
        assertEquals(2, body.size());
        assertEquals("must be a valid email", body.get("email"));
        assertEquals("cannot be empty", body.get("username"));
    }
    
    private MethodParameter mock(Class<?> clazz) {
        return org.mockito.Mockito.mock(MethodParameter.class);
    }
}
