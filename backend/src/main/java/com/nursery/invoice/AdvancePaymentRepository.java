package com.nursery.invoice;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface AdvancePaymentRepository extends JpaRepository<AdvancePayment, Long> {

    List<AdvancePayment> findByInvoiceIdOrderByPaymentDateAscIdAsc(Long invoiceId);

    @Query("select coalesce(sum(ap.amount), 0) from AdvancePayment ap where ap.invoice.id = :invoiceId")
    BigDecimal sumAdvanceByInvoiceId(@Param("invoiceId") Long invoiceId);

    void deleteByInvoiceId(Long invoiceId);
}
