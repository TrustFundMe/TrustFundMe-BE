package com.trustfund.model.request;

import jakarta.validation.constraints.Size;
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
public class UpdateCampaignRequest {

    @Size(max = 255)
    private String title;

    @Size(max = 5000)
    private String description;

    @Size(max = 500)
    private String coverImage;

    private LocalDateTime startDate;
    private LocalDateTime endDate;

    @Size(max = 50)
    private String status;

    @Size(max = 2000)
    private String thankMessage;

    private BigDecimal balance;

    private Boolean approvedByStaff;
    private LocalDateTime approvedAt;
}
