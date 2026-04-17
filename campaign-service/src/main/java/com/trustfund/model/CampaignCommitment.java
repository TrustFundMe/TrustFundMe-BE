package com.trustfund.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "campaign_commitments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CampaignCommitment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "campaign_id", nullable = false)
    private Long campaignId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    // ── OCR fields captured at sign time ──
    @Column(name = "address", length = 1000)
    private String address;

    @Column(name = "workplace", length = 500)
    private String workplace;

    @Column(name = "tax_id", length = 50)
    private String taxId;

    @Column(name = "id_number", nullable = false)
    private String idNumber;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "issue_place")
    private String issuePlace;

    @Column(name = "issue_date")
    private LocalDate issueDate;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Lob
    @Column(name = "signature_url", columnDefinition = "LONGTEXT")
    private String signatureUrl;

    @Column(name = "ip_address")
    private String ipAddress;

    @Builder.Default
    private String status = "SIGNED";

    @Column(name = "created_at")
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
