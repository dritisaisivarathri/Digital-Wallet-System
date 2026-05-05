package com.wallet.notification.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    @Test
    void sendEmail_Success() {
        emailService.sendEmail("user@test.com", "admin@test.com", "Subject", "Body");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertEquals("user@test.com", captor.getValue().getTo()[0]);
        assertEquals("no-reply@digitalwallet.com", captor.getValue().getFrom());
        assertEquals("admin@test.com", captor.getValue().getReplyTo());
        assertEquals("Subject", captor.getValue().getSubject());
        assertEquals("Body", captor.getValue().getText());
    }

    @Test
    void sendEmail_Failure() {
        doThrow(new RuntimeException("smtp down")).when(mailSender).send(org.mockito.ArgumentMatchers.any(SimpleMailMessage.class));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> emailService.sendEmail("user@test.com", "admin@test.com", "Subject", "Body"));

        assertEquals("Email delivery failed", ex.getMessage());
    }
}
