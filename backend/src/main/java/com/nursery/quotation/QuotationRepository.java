package com.nursery.quotation;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QuotationRepository extends JpaRepository<Quotation, Long> {

    // ─── Single fetch ────────────────────────────────────────────────

    @Query("SELECT q FROM Quotation q LEFT JOIN FETCH q.items WHERE q.id = :id AND q.userId = :userId")
    Optional<Quotation> findByIdAndUserId(Long id, UUID userId);

    Optional<Quotation> findByPublicToken(String token);

    // ─── List & search ───────────────────────────────────────────────

    @Query("SELECT q FROM Quotation q WHERE q.userId = :userId " +
           "AND (:search IS NULL OR LOWER(q.customerName) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Quotation> findByUserIdWithSearch(UUID userId, String search, Pageable pageable);

    @Query("SELECT q FROM Quotation q WHERE q.userId = :userId AND q.status = :status " +
           "AND (:search IS NULL OR LOWER(q.customerName) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Quotation> findByUserIdAndStatusWithSearch(UUID userId, QuotationStatus status, String search, Pageable pageable);

    // ─── Numbering ────────────────────────────────────────────────────

    @Query("SELECT COALESCE(MAX(q.sequence), 0) FROM Quotation q WHERE q.userId = :userId AND q.year = :year")
    int findMaxSequenceForUserAndYear(UUID userId, int year);

    boolean existsByQuotationNumber(String quotationNumber);

    // ─── Analytics ───────────────────────────────────────────────────

    long countByUserId(UUID userId);

    @Query("SELECT COALESCE(SUM(q.totalAmount), 0) FROM Quotation q WHERE q.userId = :userId AND q.status = 'FINAL'")
    BigDecimal sumTotalAmountByUserIdAndStatusFinal(UUID userId);

    @Query("SELECT q FROM Quotation q WHERE q.userId = :userId ORDER BY q.createdAt DESC LIMIT :limit")
    List<Quotation> findRecentByUserId(UUID userId, int limit);
}
