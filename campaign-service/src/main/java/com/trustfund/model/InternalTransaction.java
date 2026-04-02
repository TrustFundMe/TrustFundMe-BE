package com.trustfund.model;

import com.trustfund.model.enums.InternalTransactionStatus;
import com.trustfund.model.enums.InternalTransactionType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "internal_transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InternalTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "from_campaign_id")
    private Long fromCampaignId;

    @Column(name = "to_campaign_id")
    private Long toCampaignId;

    @Column(name = "amount", precision = 19, scale = 4, nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 50, nullable = false)
    private InternalTransactionType type;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "created_by_staff_id")
    private Long createdByStaffId;

    @Column(name = "evidence_image_id")
    private Long evidenceImageId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50, nullable = false)

    private InternalTransactionStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
