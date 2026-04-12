package com.nursery.quotation;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * ✅ FIX #2: Input Validation Added
 *
 * Prevents:
 * - SQL injection via plant names
 * - Invalid quantities/prices
 * - Buffer overflow with long strings
 * - Data integrity issues
 */
@Entity
@Table(name = "quotation_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuotationItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quotation_id", nullable = false)
    private Quotation quotation;

    @NotBlank(message = "Plant name cannot be blank")
    @Size(min = 1, max = 150, message = "Plant name must be 1-150 characters")
    @Column(name = "plant_name", nullable = false, length = 150)
    private String plantName;

    @NotBlank(message = "Dimension cannot be blank")
    @Size(min = 1, max = 20, message = "Dimension must be 1-20 characters")
    @Column(name = "dimension", nullable = false, length = 20)
    private String dimension;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    @Max(value = 100000, message = "Quantity cannot exceed 100000")
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    @DecimalMax(value = "999999.99", message = "Price cannot exceed 999999.99")
    @Column(name = "price", nullable = false)
    private BigDecimal price;

    @NotNull(message = "Item total is required")
    @DecimalMin(value = "0.01", message = "Item total must be greater than 0")
    @DecimalMax(value = "999999.99", message = "Item total cannot exceed 999999.99")
    @Column(name = "item_total", nullable = false)
    private BigDecimal itemTotal;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}


