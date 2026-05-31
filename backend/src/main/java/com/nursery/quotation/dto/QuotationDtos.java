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
             @Pattern(
                                 regexp = "(^\\d+\\s*[xX]\\s*\\d+$)|(^[Pp]ot$)|(^\\d+\"\\s*[Pp]ot$)|(^\\d+\\s*[Kk][Gg](s)?$)",
                                 message = "Dimension must be a bag size (number x number), a weight size (number kg/kgs), or a pot size (pot or number\" pot)"
             )
             @Size(max = 20)
             String dimension,

            @Size(max = 20, message = "Height must not exceed 20 characters")
            String height,

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
            String notes,

            Boolean heightEnabled,

            @Valid
            List<QuotationItemRequest> items,

            // If true → FINAL, if false/null → DRAFT
            Boolean shouldFinalize
    ) {}

    public record UpdateQuotationRequest(
            String customerName,
            String customerPhone,
            String customerAddress,
            String notes,

            Boolean heightEnabled,

            @Valid
            List<QuotationItemRequest> items,

            Boolean shouldFinalize
    ) {}

    // ─── Response DTOs ───────────────────────────────────────────────

    public record QuotationItemResponse(
            Long id,
            String plantName,
            String dimension,
            String height,
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
            String notes,
            Boolean heightEnabled,
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
