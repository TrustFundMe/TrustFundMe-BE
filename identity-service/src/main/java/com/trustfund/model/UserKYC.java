package com.trustfund.model;

import com.trustfund.model.enums.KYCStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_kyc")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserKYC {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "id_type", nullable = false)
    private String idType;

    @Column(name = "id_number", nullable = false)
    private String idNumber;

    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;

    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    @Column(name = "issue_place", nullable = false)
    private String issuePlace;

    @Column(name = "id_image_front", nullable = false)
    private String idImageFront;

    @Column(name = "id_image_back", nullable = false)
    private String idImageBack;

    @Column(name = "selfie_image", nullable = false)
    private String selfieImage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private KYCStatus status = KYCStatus.PENDING;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    // ── OCR-extracted fields ──
    /** Họ tên đầy đủ trên CCCD/hộ chiếu */
    @Column(name = "full_name_ocr", length = 500)
    private String fullNameOcr;

    /** Địa chỉ đăng ký thường trú (quê quán) trên CCCD */
    @Column(name = "address", length = 1000)
    private String address;

    /** Nơi làm việc hiện tại */
    @Column(name = "workplace", length = 500)
    private String workplace;

    /** Mã số thuế cá nhân */
    @Column(name = "tax_id", length = 50)
    private String taxId;

    // ── Face biometric fields ──
    /** Face descriptor vector (128-dim JSON array) for face comparison */
    @Column(name = "face_descriptor", columnDefinition = "JSON")
    private String faceDescriptor;

    /** Liveness proof metadata (turn steps, timestamp, duration) */
    @Column(name = "liveness_metadata", columnDefinition = "JSON")
    private String livenessMetadata;

    /** Sample of key 3D face mesh landmark points */
    @Column(name = "face_mesh_sample", columnDefinition = "JSON")
    private String faceMeshSample;

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
