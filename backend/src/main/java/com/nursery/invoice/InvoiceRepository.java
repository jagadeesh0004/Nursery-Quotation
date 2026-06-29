package com.nursery.invoice;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    @Query("SELECT i FROM Invoice i LEFT JOIN FETCH i.items WHERE i.id = :id AND i.userId = :userId AND i.deletedAt IS NULL")
    Optional<Invoice> findByIdAndUserId(Long id, UUID userId);

    @Query("SELECT i FROM Invoice i WHERE i.userId = :userId AND i.deletedAt IS NULL " +
           "AND (:search IS NULL OR LOWER(i.customerName) LIKE LOWER(CONCAT('%', :search, '%')))" )
    Page<Invoice> findByUserIdWithSearch(UUID userId, String search, Pageable pageable);

    @Query("SELECT i FROM Invoice i WHERE i.userId = :userId AND i.deletedAt IS NULL ORDER BY i.createdAt DESC LIMIT :limit")
    List<Invoice> findRecentByUserId(UUID userId, int limit);

    @Query("SELECT COALESCE(MAX(i.sequence), 0) FROM Invoice i WHERE i.userId = :userId AND i.year = :year")
    int findMaxSequenceForUserAndYear(UUID userId, int year);
}
