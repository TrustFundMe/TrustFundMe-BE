package com.trustfund.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "expenditures")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Expenditure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "campaign_id", nullable = false)
    private Long campaignId;

    @Column(name = "vote_created_by", nullable = false)
    private Long voteCreatedBy;

    @Column(name = "evidence_due_at")
    private LocalDateTime evidenceDueAt;

    @Column(name = "total_amount", precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(columnDefinition = "NVARCHAR(2000)")
    private String plan;

    @Column(name = "vote_start_at")
    private LocalDateTime voteStartAt;

    @Column(name = "vote_end_at")
    private LocalDateTime voteEndAt;

    @Column(name = "vote_status", length = 50)
    private String voteStatus;

    @Column(length = 50)
    private String status;

    @Column(name = "evidence_status", columnDefinition = "NVARCHAR(50)")
    private String evidenceStatus;

    @Column(name = "vote_result", columnDefinition = "NVARCHAR(2000)")
    private String voteResult;

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
