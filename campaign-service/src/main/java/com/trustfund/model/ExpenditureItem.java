package com.trustfund.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "expenditure_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpenditureItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expenditure_id", nullable = false)
    @JsonIgnore
    private Expenditure expenditure;

    @Column(name = "expenditure_id", insertable = false, updatable = false)
    private Long expenditureId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "catology_id")
    @JsonIgnore
    private ExpenditureCatology catology;

    @Column(name = "catology_id", insertable = false, updatable = false)
    private Long catologyId;

    @Column(name = "name", length = 255)
    private String name;

    @Column(name = "expected_purchase_link", length = 1000)
    private String expectedPurchaseLink;

    @Column(name = "actual_purchase_link", length = 1000)
    private String actualPurchaseLink;

    @Column(name = "expected_quantity")
    private Integer expectedQuantity;

    @Column(name = "actual_quantity")
    private Integer actualQuantity;

    @Column(name = "quantity_left")
    private Integer quantityLeft;

    @Column(name = "actual_price", precision = 19, scale = 4)
    private BigDecimal actualPrice;

    @Column(name = "expected_price", precision = 19, scale = 4)
    private BigDecimal expectedPrice;

    @Column(name = "note", length = 1000)
    private String note;

    @Column(name = "purchase_location", length = 500)
    private String purchaseLocation;

    @Column(name = "brand", length = 255)
    private String brand;

    @Column(name = "unit", length = 50)
    private String unit;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
