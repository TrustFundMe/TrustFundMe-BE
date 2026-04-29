package com.trustfund.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "expenditure_catology")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpenditureCatology {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expenditure_id", nullable = false)
    @JsonIgnore
    private Expenditure expenditure;

    @Column(name = "expenditure_id", insertable = false, updatable = false)
    private Long expenditureId;

    @Column(name = "name", nullable = false, length = 255, columnDefinition = "NVARCHAR(255)")
    private String name;

    @Column(name = "description", length = 1000, columnDefinition = "NVARCHAR(1000)")
    private String description;

    @Column(name = "expected_amount", precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal expectedAmount = BigDecimal.ZERO;

    @Column(name = "actual_amount", precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal actualAmount = BigDecimal.ZERO;

    @Column(name = "balance", precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "withdrawal_condition", length = 1000, columnDefinition = "NVARCHAR(1000)")
    private String withdrawalCondition;

    @OneToMany(mappedBy = "catology", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ExpenditureItem> items = new ArrayList<>();

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
