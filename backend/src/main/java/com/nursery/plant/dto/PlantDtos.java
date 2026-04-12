package com.nursery.plant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.List;

public class PlantDtos {

    public record CreatePlantRequest(
            @NotBlank(message = "Plant name is required")
            @Size(max = 100, message = "Plant name must not exceed 100 characters")
            String name
    ) {}

    public record AddDimensionRequest(
            @NotBlank(message = "Dimension is required")
            @Pattern(regexp = "^\\d+\\s*[xX]\\s*\\d+$",
                    message = "Dimension must follow format: number x number (e.g., 5x7, 8 x 10)")
            @Size(max = 20, message = "Dimension must not exceed 20 characters")
            String dimension,

            @NotNull(message = "Price is required")
            @DecimalMin(value = "0.0", message = "Price must be zero or greater")
            java.math.BigDecimal price
    ) {}

    public record DimensionResponse(
            Long id,
            String dimension,
            java.math.BigDecimal price,
            OffsetDateTime createdAt
    ) {}

    public record PlantResponse(
            Long id,
            String name,
            List<DimensionResponse> dimensions,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {}

    public record PlantSummaryResponse(
            Long id,
            String name
    ) {}
}
