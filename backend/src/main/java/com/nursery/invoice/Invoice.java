package com.nursery.invoice;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "invoices")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private UUID userId;

    @Enumerated(EnumType.STRING)
    private InvoiceStatus status;

    private Integer sequence;

    private Integer year;

    @Column(name = "invoice_number", unique = true, length = 128)
    private String invoiceNumber;

    private String customerName;
    private String customerPhone;
    private String customerAddress;
    private String notes;
    private boolean heightEnabled;

    @Column(name = "loading_charges_enabled", nullable = false)
    @Builder.Default
    private boolean loadingChargesEnabled = false;

    @Column(name = "loading_price_per_ton")
    private BigDecimal loadingPricePerTon;

    @Column(name = "loading_charges_amount", nullable = false)
    @Builder.Default
    private BigDecimal loadingChargesAmount = BigDecimal.ZERO;

    private BigDecimal totalAmount;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<InvoiceItem> items = new ArrayList<>();

    @Column(name = "invoice_date", nullable = false)
    private OffsetDateTime invoiceDate;

    private OffsetDateTime createdAt;
    private OffsetDateTime finalizedAt;
    private OffsetDateTime deletedAt;

    @PrePersist
    public void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (invoiceDate == null) {
            invoiceDate = now;
        }
    }
}
