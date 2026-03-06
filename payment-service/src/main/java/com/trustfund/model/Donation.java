package com.trustfund.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "donations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Donation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "donor_id")
    private Long donorId;

    @Column(name = "campaign_id")
    private Long campaignId;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "payment_id", referencedColumnName = "id")
    private Payment payment;

    @Builder.Default
    @Column(name = "is_anonymous")
    private Boolean isAnonymous = false;

    @Column(name = "donation_amount", precision = 19, scale = 4)
    private BigDecimal donationAmount;

    @Column(name = "tip_amount", precision = 19, scale = 4)
    private BigDecimal tipAmount;

    @Column(name = "total_amount", precision = 19, scale = 4)
    private BigDecimal totalAmount;

    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
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
