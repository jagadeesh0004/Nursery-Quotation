package com.nursery.invoice.dto;

import com.nursery.invoice.InvoiceStatus;
import com.nursery.invoice.DiscountType;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public class InvoiceDtos {

    @Value
    public static class CreateInvoiceRequest {
        String customerName;
        String customerPhone;
        String customerAddress;
        String notes;
        Boolean heightEnabled;
        BigDecimal shippingCharges;
        DiscountRequest discount;
        List<InvoiceItemRequest> items;
        Boolean showAdvanceListInPdf;
        Boolean shouldFinalize;
    }

    @Value
    public static class UpdateInvoiceRequest {
        String customerName;
        String customerPhone;
        String customerAddress;
        String notes;
        Boolean heightEnabled;
        BigDecimal shippingCharges;
        DiscountRequest discount;
        List<InvoiceItemRequest> items;
        Boolean showAdvanceListInPdf;
        Boolean shouldFinalize;
    }

    @Value
    public static class DiscountRequest {
        DiscountType type;
        BigDecimal value;
    }

    @Value
    public static class InvoiceItemRequest {
        String plantName;
        String dimension;
        String height;
        Integer quantity;
        BigDecimal price;
    }

    @Value
    public static class InvoiceItemResponse {
        String plantName;
        String dimension;
        String height;
        Integer quantity;
        BigDecimal price;
        BigDecimal itemTotal;
    }

    @Value
    public static class InvoiceResponse {
        Long id;
        String invoiceNumber;
        InvoiceStatus status;
        String customerName;
        String customerPhone;
        String customerAddress;
        String notes;
        Boolean heightEnabled;
        String nurseryName;
        String nurseryAddress;
        String nurseryPhone;
        String nurseryOwnerName;
        List<InvoiceItemResponse> items;
        BigDecimal totalAmount;
        DiscountType discountType;
        BigDecimal discountPercentage;
        BigDecimal discountAmount;
        BigDecimal shippingCharges;
        BigDecimal grandTotal;
        Boolean showAdvanceListInPdf;
        List<AdvancePaymentResponse> advancePayments;
        BigDecimal totalAdvance;
        BigDecimal remainingAmount;
        OffsetDateTime finalizedAt;
        OffsetDateTime createdAt;
    }

    @Value
    public static class InvoiceSummaryResponse {
        Long id;
        String invoiceNumber;
        InvoiceStatus status;
        String customerName;
        BigDecimal grandTotal;
        OffsetDateTime createdAt;
    }

    @Value
    @Builder
    public static class AdvancePaymentRequest {
        LocalDate paymentDate;
        BigDecimal amount;
        String paymentMode;
        String remarks;
    }

    @Value
    public static class AdvancePaymentResponse {
        Long id;
        LocalDate paymentDate;
        BigDecimal amount;
        String paymentMode;
        String remarks;
        OffsetDateTime createdAt;
        OffsetDateTime updatedAt;
    }
}
