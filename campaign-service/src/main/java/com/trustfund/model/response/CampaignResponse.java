package com.trustfund.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CampaignResponse {
    private Long id;
    private Long fundOwnerId;
    private String title;
    private Long mediaId;
    private String coverImageUrl;
    private String description;
    private Long categoryId;
    private String categoryName;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String type;
    private String status;
    private BigDecimal balance;
    private Long approvedByStaff;
    private LocalDateTime approvedAt;
    private String rejectionReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Verification flags
    private boolean kycVerified;
    private boolean bankVerified;
}
