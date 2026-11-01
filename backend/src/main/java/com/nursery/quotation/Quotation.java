package com.nursery.quotation;

import com.nursery.common.entity.BaseAuditEntity;
import com.nursery.invoice.DiscountType;
import com.nursery.customer.Customer;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "quotations", uniqueConstraints = @UniqueConstraint(name = "uk_user_quotation_number", columnNames = {"user_id", "quotation_number"}))
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Quotation extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Column(name = "quotation_number", length = 50)
    private String quotationNumber;

    @Column(name = "year", nullable = false)
    private Integer year;

    @Column(name = "sequence")
    private Integer sequence;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private QuotationStatus status;

    @Column(name = "customer_name", length = 150)
    private String customerName;

    @Column(name = "customer_phone", length = 15)
    private String customerPhone;

    @Column(name = "customer_address")
    private String customerAddress;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;

    @Column(name = "height_enabled", nullable = false)
    @Builder.Default
    private boolean heightEnabled = false;

    @Column(name = "loading_charges_enabled", nullable = false)
    @Builder.Default
    private boolean loadingChargesEnabled = false;

    @Column(name = "loading_price_per_ton")
    private BigDecimal loadingPricePerTon;

    @Column(name = "loading_charges_amount", nullable = false)
    @Builder.Default
    private BigDecimal loadingChargesAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false)
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", length = 32)
    private DiscountType discountType;

    @Column(name = "discount_percentage")
    @Builder.Default
    private BigDecimal discountPercentage = BigDecimal.ZERO;

    @Column(name = "discount_amount", nullable = false)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "shipping_charges", nullable = false)
    @Builder.Default
    private BigDecimal shippingCharges = BigDecimal.ZERO;

    @Column(name = "grand_total", nullable = false)
    @Builder.Default
    private BigDecimal grandTotal = BigDecimal.ZERO;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "public_token", unique = true, length = 64)
    private String publicToken;

    @Column(name = "public_token_expires_at")
    private OffsetDateTime publicTokenExpiresAt;

    @Column(name = "finalized_at")
    private OffsetDateTime finalizedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @OneToMany(mappedBy = "quotation", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<QuotationItem> items = new ArrayList<>();
}
