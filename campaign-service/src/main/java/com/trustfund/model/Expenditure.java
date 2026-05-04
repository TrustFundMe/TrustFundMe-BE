package com.trustfund.model;

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
@Table(name = "expenditures", indexes = {
    @Index(name = "idx_expenditures_campaign_id", columnList = "campaign_id"),
    @Index(name = "idx_expenditures_status", columnList = "status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Expenditure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToMany(mappedBy = "expenditure", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ExpenditureTransaction> transactions = new ArrayList<>();

    @Column(name = "campaign_id", nullable = false)
    private Long campaignId;

    @OneToMany(mappedBy = "expenditure", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ExpenditureEvidence> evidences = new ArrayList<>();

    @Column(name = "evidence_due_at")
    private LocalDateTime evidenceDueAt;

    @Column(name = "evidence_status", length = 50)
    private String evidenceStatus;

    @Column(name = "evidence_submitted_at")
    private LocalDateTime evidenceSubmittedAt;

    @Column(name = "total_amount", precision = 19, scale = 4)
    private BigDecimal totalAmount;

    @Column(name = "total_expected_amount", precision = 19, scale = 4)
    private BigDecimal totalExpectedAmount;

    @Column(name = "variance", precision = 19, scale = 4)
    private BigDecimal variance;

    @Column(name = "total_received_amount", precision = 19, scale = 4)
    private BigDecimal totalReceivedAmount;

    @Column(name = "is_withdrawal_requested", nullable = false)
    @Builder.Default
    private Boolean isWithdrawalRequested = false;

    @Column(name = "plan", length = 2000)
    private String plan;

    @Column(name = "status", length = 50)
    private String status;

    @Column(name = "is_system_generated")
    @Builder.Default
    private Boolean isSystemGenerated = false;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;
    
    @Column(name = "staff_review_id")
    private Long staffReviewId;
    
    @Column(name = "reject_reason", length = 1000)
    private String rejectReason;

    @Column(name = "proof_url", length = 500)
    private String proofUrl;

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
