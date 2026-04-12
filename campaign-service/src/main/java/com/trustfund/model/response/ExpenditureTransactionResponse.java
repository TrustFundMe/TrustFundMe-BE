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
public class ExpenditureTransactionResponse {
    private Long id;
    private Long expenditureId;
    private Long campaignId;
    private Long fromUserId;
    private Long toUserId;
    private BigDecimal amount;
    private String fromBankCode;
    private String fromAccountNumber;
    private String fromAccountHolderName;
    private String toBankCode;
    private String toAccountNumber;
    private String toAccountHolderName;
    private String type;
    private String status;
    private String proofUrl;
    private LocalDateTime createdAt;
}
