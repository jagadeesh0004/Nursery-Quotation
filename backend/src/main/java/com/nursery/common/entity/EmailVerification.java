package com.nursery.common.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity for storing email verification OTP records
 */
@Entity
@Table(name = "email_verifications")
public class EmailVerification {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "otp_code", nullable = false)
    private String otpCode;

    @Column(name = "is_verified", nullable = false)
    private Boolean verified = false;

    @Column(name = "attempts", nullable = false)
    private Integer attempts = 0;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    // Constructors
    public EmailVerification() {}

    // Getters & Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getOtpCode() { return otpCode; }
    public void setOtpCode(String otpCode) { this.otpCode = otpCode; }

    public Boolean isVerified() { return verified; }
    public void setVerified(Boolean verified) { this.verified = verified; }

    public Integer getAttempts() { return attempts; }
    public void setAttempts(Integer attempts) { this.attempts = attempts; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    @Override
    public String toString() {
        return "EmailVerification{" +
            "id=" + id +
            ", email='" + email + '\'' +
            ", verified=" + verified +
            ", attempts=" + attempts +
            ", createdAt=" + createdAt +
            ", expiresAt=" + expiresAt +
            '}';
    }
}

