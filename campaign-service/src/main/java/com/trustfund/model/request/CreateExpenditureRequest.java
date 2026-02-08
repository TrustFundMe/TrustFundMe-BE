package com.trustfund.model.request;

import jakarta.validation.constraints.Future;
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
public class CreateExpenditureRequest {

    @NotNull(message = "campaignId không được để trống")
    private Long campaignId;

    @Future(message = "Hạn nộp minh chứng phải là một ngày trong tương lai")
    private LocalDateTime evidenceDueAt;

    private String evidenceStatus;

    private String plan;

    private List<CreateExpenditureItemRequest> items;
}
