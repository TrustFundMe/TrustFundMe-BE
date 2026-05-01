package com.trustfund.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateEvidenceRequirementRequest {
    private Long campaignId;
    private String cassoTransactionId;
    private java.math.BigDecimal amount;
    private String description;
    private java.time.LocalDateTime transactionDate;
}
