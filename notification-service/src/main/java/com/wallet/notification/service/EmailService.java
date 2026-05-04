package com.wallet.notification.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.from:}")
    private String configuredFromAddress;

    @Value("${spring.mail.username:}")
    private String smtpUsername;

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    public void sendEmail(String to, String from, String subject, String body) {
        try {
            if (!isValidEmail(to)) {
                throw new IllegalArgumentException("Recipient email is invalid: " + to);
            }

            String sender = isValidEmail(configuredFromAddress)
                    ? configuredFromAddress.trim()
                    : (isValidEmail(smtpUsername) ? smtpUsername.trim() : "no-reply@digitalwallet.com");

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(sender);
            if (isValidEmail(from) && !sender.equalsIgnoreCase(from.trim())) {
                message.setReplyTo(from.trim());
            }
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            
            mailSender.send(message);
            logger.info("Email sent successfully to {}", to);
        } catch (Exception e) {
            logger.error("Failed to send email to {}", to, e);
            throw new RuntimeException("Email delivery failed", e);
        }
    }

    private boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email.trim()).matches();
    }
}
