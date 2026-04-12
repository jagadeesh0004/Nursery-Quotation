package com.nursery.plant;

import com.nursery.common.entity.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.time.OffsetDateTime;

@Entity
@Table(name = "dimensions")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Dimension extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plant_id", nullable = false)
    private Plant plant;

    @Column(name = "dimension", nullable = false, length = 20)
    private String dimension;

    @Column(name = "price", precision = 10, scale = 2, nullable = false)
    private java.math.BigDecimal price;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;
}
