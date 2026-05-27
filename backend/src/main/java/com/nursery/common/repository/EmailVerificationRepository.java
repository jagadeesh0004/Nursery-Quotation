package com.nursery.common.repository;

import com.nursery.common.entity.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for EmailVerification entity
 */
public interface EmailVerificationRepository extends JpaRepository<EmailVerification, UUID> {

    /**
     * Find the latest unverified OTP record for an email
     */
    Optional<EmailVerification> findFirstByEmailAndVerifiedFalseOrderByCreatedAtDesc(String email);

    /**
     * Find a verified OTP record for an email
     */
    Optional<EmailVerification> findFirstByEmailAndVerifiedTrueOrderByCreatedAtDesc(String email);

    /**
     * Delete all unverified OTP records for an email
     */
    @Modifying
    @Transactional
    void deleteUnverifiedByEmail(String email);
}

