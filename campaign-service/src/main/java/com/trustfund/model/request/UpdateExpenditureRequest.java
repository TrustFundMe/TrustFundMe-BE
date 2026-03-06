package com.trustfund.model.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateExpenditureRequest {

    @NotNull(message = "campaignId không được để trống")
    private Long campaignId;

    private LocalDateTime evidenceDueAt;

    private String evidenceStatus;

    private String plan;

    private List<CreateExpenditureItemRequest> items;
}
