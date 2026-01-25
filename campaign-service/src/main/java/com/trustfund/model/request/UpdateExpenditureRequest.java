package com.trustfund.model.request;

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
public class UpdateExpenditureRequest {

    private LocalDateTime evidenceDueAt;

    private BigDecimal totalAmount;

    private String plan;

    private LocalDateTime voteStartAt;

    private LocalDateTime voteEndAt;

    private String voteStatus;

    private String status;

    private String evidenceStatus;

    private String voteResult;
}
