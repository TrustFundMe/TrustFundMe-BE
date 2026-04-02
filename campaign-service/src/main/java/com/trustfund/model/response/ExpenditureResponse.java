package com.trustfund.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpenditureResponse {
    private Long id;
    private Long campaignId;
    private LocalDateTime evidenceDueAt;
    private String evidenceStatus;
    private LocalDateTime evidenceSubmittedAt;
    private BigDecimal totalAmount;
    private BigDecimal totalExpectedAmount;
    private BigDecimal variance;
    private Boolean isWithdrawalRequested;
    private String plan;
    private String status;
    private Long staffReviewId;
    private String rejectReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String disbursementProofUrl;
    private List<ExpenditureTransactionResponse> transactions;
}
