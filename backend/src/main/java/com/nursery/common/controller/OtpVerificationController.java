package com.nursery.common.controller;

import com.nursery.common.service.OtpVerificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Controller for OTP email verification endpoints
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*", maxAge = 3600)
public class OtpVerificationController {

    private final OtpVerificationService otpVerificationService;

    public OtpVerificationController(OtpVerificationService otpVerificationService) {
        this.otpVerificationService = otpVerificationService;
    }

    /**
     * Send OTP to email during signup
     * POST /api/auth/send-otp
     * Request: { "email": "user@example.com", "user_id": "uuid" }
     */
    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOtp(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String userId = request.get("user_id");

        if (email == null || email.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Email is required"
            ));
        }

        if (userId == null || userId.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "User ID is required"
            ));
        }

        try {
            log.info("Sending OTP for {}", email);
            otpVerificationService.sendOtpForSignup(UUID.fromString(userId), email);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "OTP sent to your email",
                "expiresIn", 600 // 10 minutes in seconds
            ));

        } catch (Exception e) {
            String errorMessage = e.getMessage();

            // Check if it's a validation error (email is temporary/disposable)
            if (errorMessage.contains("Temporary") || errorMessage.contains("Invalid email") || errorMessage.contains("not allowed")) {
                log.warn("Email validation failed: {}", errorMessage);
                return ResponseEntity.status(400).body(Map.of(
                    "success", false,
                    "message", errorMessage,
                    "type", "INVALID_EMAIL"
                ));
            }

            // Other errors
            log.error("Send OTP error", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Failed to send OTP. Please try again later",
                "error", e.getMessage()
            ));
        }
    }

    /**
     * Verify OTP entered by user
     * POST /api/auth/verify-otp
     * Request: { "email": "user@example.com", "otp": "123456" }
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String otp = request.get("otp");

        if (email == null || email.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Email is required"
            ));
        }

        if (otp == null || otp.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "OTP is required"
            ));
        }

        try {
            log.info("Verifying OTP for {}", email);
            boolean isValid = otpVerificationService.verifyOtp(email, otp);

            if (isValid) {
                log.info("OTP verified successfully for {}", email);
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Email verified successfully"
                ));
            } else {
                long expiryTime = otpVerificationService.getOtpExpiryTime(email);
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Invalid OTP or expired",
                    "expiresIn", expiryTime
                ));
            }

        } catch (Exception e) {
            log.error("Verify OTP error for {}", email, e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Error verifying OTP"
            ));
        }
    }

    /**
     * Resend OTP
     * POST /api/auth/resend-otp
     * Request: { "email": "user@example.com", "user_id": "uuid" }
     */
    @PostMapping("/resend-otp")
    public ResponseEntity<?> resendOtp(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String userId = request.get("user_id");

        if (email == null || email.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Email is required"
            ));
        }

        if (userId == null || userId.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "User ID is required"
            ));
        }

        try {
            log.info("Resending OTP for {}", email);
            otpVerificationService.resendOtp(UUID.fromString(userId), email);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "OTP resent to your email",
                "expiresIn", 600
            ));

        } catch (Exception e) {
            log.error("Resend OTP error for {}", email, e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Failed to resend OTP"
            ));
        }
    }

    /**
     * Check if email is verified
     * GET /api/auth/is-verified?email=user@example.com
     */
    @GetMapping("/is-verified")
    public ResponseEntity<?> checkIfVerified(@RequestParam String email) {
        try {
            boolean verified = otpVerificationService.isEmailVerified(email);

            return ResponseEntity.ok(Map.of(
                "verified", verified,
                "message", verified ? "Email is verified" : "Email is not verified"
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Error checking verification status"
            ));
        }
    }
}

