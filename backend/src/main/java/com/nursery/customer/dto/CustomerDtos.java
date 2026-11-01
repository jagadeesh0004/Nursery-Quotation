package com.nursery.customer.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.OffsetDateTime;

public class CustomerDtos {
    public record CustomerRequest(@NotBlank String name, String phone, String address) {}

    public record CustomerSummary(Long id, String name, String phone, String address,
                                  long quotationCount, long invoiceCount, OffsetDateTime updatedAt) {}

    public record CustomerDocument(Long id, String number, String status, String customerName,
                                   java.math.BigDecimal total, OffsetDateTime createdAt) {}

    public record CustomerDetail(Long id, String name, String phone, String address,
                                 OffsetDateTime createdAt, OffsetDateTime updatedAt,
                                 java.util.List<CustomerDocument> quotations,
                                 java.util.List<CustomerDocument> invoices) {}
}
