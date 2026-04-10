package com.trustfund.model.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateTrustScoreConfigRequest {

    @NotNull(message = "Points cannot be null")
    private Integer points;

    private Boolean isActive;

    private String ruleName;

    private String description;
}
