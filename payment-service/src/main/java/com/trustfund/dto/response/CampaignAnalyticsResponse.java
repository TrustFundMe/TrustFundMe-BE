package com.trustfund.dto.response;

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
public class CampaignAnalyticsResponse {
    private Long campaignId;
    private BigDecimal totalReceived;
    private BigDecimal totalSpent;
    private BigDecimal currentBalance;
    private BigDecimal targetAmount;
    private java.time.LocalDateTime approvedAt;
    private List<ChartPoint> chartData;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ChartPoint {
        private String date; // Format: dd/MM
        private BigDecimal balanceGreen;
        private BigDecimal balanceRed;
    }
}
