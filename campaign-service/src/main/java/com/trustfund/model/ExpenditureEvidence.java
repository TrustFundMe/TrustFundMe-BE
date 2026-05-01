package com.trustfund.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "expenditure_evidences")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpenditureEvidence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expenditure_id", nullable = true)
    private Expenditure expenditure;

    @Column(name = "campaign_id", nullable = false)
    private Long campaignId;

    @Column(name = "casso_transaction_id", unique = true)
    private String cassoTransactionId;

    @Column(name = "amount", precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "proof_url", length = 1000)
    private String proofUrl;

    @Column(name = "status", length = 50)
    private String status; // PENDING, SUBMITTED, APPROVED, REJECTED

    @Column(name = "due_at")
    private LocalDateTime dueAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = "PENDING";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
