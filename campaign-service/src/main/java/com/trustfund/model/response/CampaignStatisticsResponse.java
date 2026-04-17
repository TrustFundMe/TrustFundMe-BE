package com.trustfund.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CampaignStatisticsResponse {
    private BigDecimal totalReceived;
    private BigDecimal totalSpent;
    private BigDecimal currentBalance;
    private String iconTotalReceived;
    private String iconTotalSpent;
    private String iconCurrentBalance;
    private BigDecimal totalReceivedFromGeneralFund;
    private String iconTotalReceivedFromGeneralFund;
    private List<ExpenditureResponse> expenditures;
    private java.util.Map<Long, String> campaignMap;
}
