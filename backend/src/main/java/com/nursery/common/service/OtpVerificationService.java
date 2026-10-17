package com.nursery.common.service;

import com.nursery.common.entity.EmailVerification;
import com.nursery.common.repository.EmailVerificationRepository;
import com.nursery.common.utils.OtpUtil;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for OTP verification logic
 */
@Slf4j
@Service
public class OtpVerificationService {

    private final EmailVerificationRepository repository;
    private final EmailService emailService;
    private final EmailValidatorService emailValidatorService;

    private static final int MAX_ATTEMPTS = 5;
    private static final int OTP_VALIDITY_MINUTES = 10;

    public OtpVerificationService(
        EmailVerificationRepository repository,
        EmailService emailService,
        EmailValidatorService emailValidatorService
    ) {
        this.repository = repository;
        this.emailService = emailService;
        this.emailValidatorService = emailValidatorService;
    }

    /**
     * Send OTP to email during signup
     */
    public void sendOtpForSignup(UUID userId, String email) {
        try {
            // NEW: Validate email is not temporary/disposable BEFORE sending OTP
            if (!emailValidatorService.isValidEmail(email)) {
                String message = emailValidatorService.getValidationMessage(email);
                log.warn("Email validation failed: {} - {}", email, message);
                throw new RuntimeException(message);
            }

            log.info("Email validation passed: {}", email);

            // Delete any existing unverified OTPs for this email
            repository.deleteUnverifiedByEmail(email);

            // Generate new OTP
            String otp = OtpUtil.generateOtp();

            // Save to database
            EmailVerification verification = new EmailVerification();
            verification.setId(UUID.randomUUID());
            verification.setUserId(userId);
            verification.setEmail(email);
            verification.setOtpCode(otp);
            verification.setVerified(false);
            verification.setAttempts(0);
            verification.setCreatedAt(LocalDateTime.now());
            verification.setExpiresAt(LocalDateTime.now().plusMinutes(OTP_VALIDITY_MINUTES));

            repository.save(verification);

            // Send OTP email
            boolean emailSent = emailService.sendOtpEmail(email, otp);

            if (emailSent) {
                log.info("OTP sent successfully to: {}", email);
            } else {
                log.warn("OTP generated but email send failed: {}", email);
            }

        } catch (Exception e) {
            log.error("Error sending OTP: {}", e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * Verify OTP entered by user
     */
    public boolean verifyOtp(String email, String otpCode) {
        try {
            // Validate format
            if (!OtpUtil.isValidOtpFormat(otpCode)) {
                log.warn("Invalid OTP format for email {}", email);
                return false;
            }

            // Find OTP record
            Optional<EmailVerification> record =
                repository.findFirstByEmailAndVerifiedFalseOrderByCreatedAtDesc(email);

            if (record.isEmpty()) {
                log.warn("No OTP record found for {}", email);
                return false;
            }

            EmailVerification verification = record.get();

            // Check if expired
            if (LocalDateTime.now().isAfter(verification.getExpiresAt())) {
                log.warn("OTP expired for {}", email);
                return false;
            }

            // Check attempts
            if (verification.getAttempts() >= MAX_ATTEMPTS) {
                log.warn("Max OTP attempts exceeded for {}", email);
                return false;
            }

            // Check if OTP matches
            if (!verification.getOtpCode().equals(otpCode)) {
                // Increment attempts
                verification.setAttempts(verification.getAttempts() + 1);
                repository.save(verification);
                log.warn(
                    "Wrong OTP for {} (attempt {}/{})",
                    email,
                    verification.getAttempts(),
                    MAX_ATTEMPTS
                );
                return false;
            }

            // OTP is correct! Mark as verified
            verification.setVerified(true);
            repository.save(verification);
            log.info("Email verified successfully for {}", email);
            return true;

        } catch (Exception e) {
            log.error("Error verifying OTP for {}", email, e);
            return false;
        }
    }

    /**
     * Check if email is verified
     */
    public boolean isEmailVerified(String email) {
        Optional<EmailVerification> record =
            repository.findFirstByEmailAndVerifiedTrueOrderByCreatedAtDesc(email);
        return record.isPresent() && record.get().isVerified();
    }

    /**
     * Get OTP remaining time in seconds
     */
    public long getOtpExpiryTime(String email) {
        Optional<EmailVerification> record =
            repository.findFirstByEmailAndVerifiedFalseOrderByCreatedAtDesc(email);

        if (record.isEmpty()) {
            return 0;
        }

        LocalDateTime expiresAt = record.get().getExpiresAt();
        long seconds = java.time.temporal.ChronoUnit.SECONDS.between(LocalDateTime.now(), expiresAt);
        return Math.max(0, seconds);
    }

    /**
     * Resend OTP (deletes old and creates new)
     */
    public void resendOtp(UUID userId, String email) {
        sendOtpForSignup(userId, email);
    }
}
