package com.trustfund.model.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewCampaignRequest {

    @NotBlank(message = "Status is required")
    private String status;

    private String rejectionReason;
}
