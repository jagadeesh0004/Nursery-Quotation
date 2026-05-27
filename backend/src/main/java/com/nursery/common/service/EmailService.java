package com.nursery.common.service;

import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Service for sending OTP verification emails
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Value("${app.email.sender:noreply@quotonursery.com}")
    private String emailSender;

    /**
     * Send OTP verification email
     * In development: logs OTP to console
     * In production: sends via email service
     */
    public boolean sendOtpEmail(String toEmail, String otp) {
        try {
            log.info("Sending OTP email to {}", toEmail);

            // Log OTP to console for development
            logOtpToConsole(toEmail, otp);

            // TODO: In production, integrate with SendGrid/Mailgun/AWS SES
            // For now, we're just logging to console

            return true;

        } catch (Exception e) {
            log.error("Failed to send OTP email to {}", toEmail, e);
            return false;
        }
    }

    /**
     * Log OTP email content to console (for development)
     */
    private void logOtpToConsole(String email, String otp) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        log.info(
            "OTP email preview [{}] to {} at {} with code {}",
            emailSender,
            email,
            timestamp,
            otp
        );
    }
}

