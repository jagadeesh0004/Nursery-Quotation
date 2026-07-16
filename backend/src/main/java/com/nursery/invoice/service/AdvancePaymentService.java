package com.nursery.invoice.service;

import com.nursery.common.exception.BusinessException;
import com.nursery.common.exception.ResourceNotFoundException;
import com.nursery.invoice.AdvancePayment;
import com.nursery.invoice.AdvancePaymentRepository;
import com.nursery.invoice.Invoice;
import com.nursery.invoice.InvoiceRepository;
import com.nursery.invoice.dto.InvoiceDtos.AdvancePaymentRequest;
import com.nursery.invoice.dto.InvoiceDtos.AdvancePaymentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdvancePaymentService {

    private final AdvancePaymentRepository advancePaymentRepository;
    private final InvoiceRepository invoiceRepository;

    @Transactional
    public AdvancePaymentResponse addAdvance(UUID userId, Long invoiceId, AdvancePaymentRequest request) {
        Invoice invoice = findOwnedInvoice(userId, invoiceId);
        AdvancePayment payment = AdvancePayment.builder()
                .invoice(invoice)
                .paymentDate(request.getPaymentDate())
                .amount(normalizeAmount(request.getAmount()))
                .paymentMode(requireText(request.getPaymentMode(), "Payment mode is required."))
                .remarks(blankToNull(request.getRemarks()))
                .build();
        validateAdvance(invoice, payment.getAmount());
        return toResponse(advancePaymentRepository.save(payment));
    }

    @Transactional(readOnly = true)
    public List<AdvancePaymentResponse> getAllAdvances(UUID userId, Long invoiceId) {
        findOwnedInvoice(userId, invoiceId);
        return advancePaymentRepository.findByInvoiceIdOrderByPaymentDateAscIdAsc(invoiceId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public AdvancePaymentResponse updateAdvance(UUID userId, Long id, AdvancePaymentRequest request) {
        AdvancePayment payment = findOwnedAdvance(userId, id);
        BigDecimal previousAmount = payment.getAmount();
        payment.setPaymentDate(request.getPaymentDate());
        payment.setAmount(normalizeAmount(request.getAmount()));
        payment.setPaymentMode(requireText(request.getPaymentMode(), "Payment mode is required."));
        payment.setRemarks(blankToNull(request.getRemarks()));
        BigDecimal totalExcludingCurrent = calculateTotalAdvance(payment.getInvoice().getId()).subtract(previousAmount);
        validateAdvance(payment.getInvoice(), payment.getAmount(), totalExcludingCurrent);
        return toResponse(advancePaymentRepository.save(payment));
    }

    @Transactional
    public void deleteAdvance(UUID userId, Long id) {
        AdvancePayment payment = findOwnedAdvance(userId, id);
        advancePaymentRepository.delete(payment);
    }

    @Transactional(readOnly = true)
    public BigDecimal calculateTotalAdvance(Long invoiceId) {
        return safe(advancePaymentRepository.sumAdvanceByInvoiceId(invoiceId));
    }

    @Transactional(readOnly = true)
    public BigDecimal calculateRemainingAmount(Invoice invoice) {
        return safe(invoice.getGrandTotal()).subtract(calculateTotalAdvance(invoice.getId()));
    }

    private void validateAdvance(Invoice invoice, BigDecimal amount) {
        validateAdvance(invoice, amount, calculateTotalAdvance(invoice.getId()));
    }

    private void validateAdvance(Invoice invoice, BigDecimal amount, BigDecimal existingAdvanceTotal) {
        if (amount == null || amount.signum() <= 0) {
            throw new BusinessException("Advance amount must be greater than zero.");
        }
        BigDecimal nextTotal = safe(existingAdvanceTotal).add(amount);
        if (nextTotal.compareTo(safe(invoice.getGrandTotal())) > 0) {
            throw new BusinessException("Total advance cannot exceed the invoice grand total.");
        }
    }

    private Invoice findOwnedInvoice(UUID userId, Long invoiceId) {
        return invoiceRepository.findByIdAndUserId(invoiceId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", invoiceId));
    }

    private AdvancePayment findOwnedAdvance(UUID userId, Long id) {
        AdvancePayment payment = advancePaymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Advance payment", id));
        if (!payment.getInvoice().getUserId().equals(userId) || payment.getInvoice().getDeletedAt() != null) {
            throw new ResourceNotFoundException("Advance payment", id);
        }
        return payment;
    }

    private AdvancePaymentResponse toResponse(AdvancePayment payment) {
        return new AdvancePaymentResponse(
                payment.getId(),
                payment.getPaymentDate(),
                payment.getAmount(),
                payment.getPaymentMode(),
                payment.getRemarks(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }

    private BigDecimal normalizeAmount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(message);
        }
        return value.trim();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
