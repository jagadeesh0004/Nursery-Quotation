package com.nursery.quotation.dto;

import com.nursery.quotation.QuotationStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public class QuotationDtos {

    // ─── Request DTOs ────────────────────────────────────────────────

    public record QuotationItemRequest(
            @NotBlank(message = "Plant name is required")
            @Size(max = 150, message = "Plant name must not exceed 150 characters")
            String plantName,

            @NotBlank(message = "Dimension is required")
            @Pattern(regexp = "^\\d+\\s*[xX]\\s*\\d+$",
                    message = "Dimension must follow format: number x number")
            @Size(max = 20)
            String dimension,

            @NotNull(message = "Quantity is required")
            @Min(value = 1, message = "Quantity must be at least 1")
            Integer quantity,

            @NotNull(message = "Price is required")
            @DecimalMin(value = "0.0", message = "Price must be zero or greater")
            BigDecimal price
    ) {}

    public record CreateQuotationRequest(
            // Customer info - optional for DRAFT
            String customerName,
            String customerPhone,
            String customerAddress,

            @Valid
            List<QuotationItemRequest> items,

            // If true → FINAL, if false/null → DRAFT
            Boolean shouldFinalize
    ) {}

    public record UpdateQuotationRequest(
            String customerName,
            String customerPhone,
            String customerAddress,

            @Valid
            List<QuotationItemRequest> items,

            Boolean shouldFinalize
    ) {}

    // ─── Response DTOs ───────────────────────────────────────────────

    public record QuotationItemResponse(
            Long id,
            String plantName,
            String dimension,
            Integer quantity,
            BigDecimal price,
            BigDecimal itemTotal
    ) {}

    public record QuotationResponse(
            Long id,
            String quotationNumber,
            QuotationStatus status,
            String customerName,
            String customerPhone,
            String customerAddress,
            String nurseryName,
            String nurseryAddress,
            String nurseryPhone,
            String nurseryOwnerName,
            List<QuotationItemResponse> items,
            BigDecimal totalAmount,
            String publicToken,
            OffsetDateTime finalizedAt,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {}

    public record QuotationSummaryResponse(
            Long id,
            String quotationNumber,
            QuotationStatus status,
            String customerName,
            BigDecimal totalAmount,
            OffsetDateTime createdAt
    ) {}
}
