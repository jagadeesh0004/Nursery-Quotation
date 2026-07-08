package com.nursery.invoice.dto;

import com.nursery.invoice.InvoiceStatus;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
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
        Boolean loadingChargesEnabled;
        BigDecimal loadingPricePerTon;
        List<InvoiceItemRequest> items;
        Boolean shouldFinalize;
    }

    @Value
    public static class UpdateInvoiceRequest {
        String customerName;
        String customerPhone;
        String customerAddress;
        String notes;
        Boolean heightEnabled;
        Boolean loadingChargesEnabled;
        BigDecimal loadingPricePerTon;
        List<InvoiceItemRequest> items;
        Boolean shouldFinalize;
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
        Boolean loadingChargesEnabled;
        BigDecimal loadingPricePerTon;
        BigDecimal loadingChargesAmount;
        BigDecimal totalAmount;
        OffsetDateTime finalizedAt;
        OffsetDateTime createdAt;
    }

    @Value
    public static class InvoiceSummaryResponse {
        Long id;
        String invoiceNumber;
        InvoiceStatus status;
        String customerName;
        BigDecimal totalAmount;
        OffsetDateTime createdAt;
    }
}
