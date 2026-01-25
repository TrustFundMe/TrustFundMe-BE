package com.trustfund.model.request;

import jakarta.validation.constraints.NotNull;
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
public class CreateExpenditureRequest {

    @NotNull(message = "Campaign ID is required")
    private Long campaignId;

    @NotNull(message = "Vote created by is required")
    private Long voteCreatedBy;

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
