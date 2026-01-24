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
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
