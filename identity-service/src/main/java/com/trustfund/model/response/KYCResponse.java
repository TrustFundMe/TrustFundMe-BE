package com.trustfund.model.response;

import com.trustfund.model.enums.KYCStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KYCResponse {
    private Long id;
    private Long userId;
    // ── OCR fields (từ CCCD/hộ chiếu) ──
    private String fullName;          // họ tên trên CCCD
    private String address;           // địa chỉ thường trú
    private String workplace;         // nơi làm việc
    private String taxId;             // mã số thuế cá nhân
    // ── Tài khoản user ──
    private String email;
    private String phoneNumber;
    // ── ID document ──
    private String idType;
    private String idNumber;
    private LocalDate issueDate;
    private LocalDate expiryDate;
    private String issuePlace;
    private String idImageFront;
    private String idImageBack;
    private String selfieImage;
    private KYCStatus status;
    private String rejectionReason;
    // ── Face biometric data ──
    private String faceDescriptor;
    private String livenessMetadata;
    private String faceMeshSample;
    /** Whether this KYC has passed face liveness check */
    private boolean livenessVerified;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
