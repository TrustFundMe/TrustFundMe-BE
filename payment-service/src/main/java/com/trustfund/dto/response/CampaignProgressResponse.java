package com.trustfund.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignProgressResponse {
    private Long campaignId;
    private BigDecimal raisedAmount;
    private BigDecimal goalAmount;
    private int progressPercentage;
}
