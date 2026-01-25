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
public class FlagRequest {
    private Long postId;
    private Long campaignId;
    @NotBlank(message = "Reason is required")
    private String reason;
}
