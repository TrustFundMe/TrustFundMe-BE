package com.trustfund.model.response;

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
public class AggregatedTransactionResponse {
    private String id;
    private String type; // DONATION, EXPENDITURE, REFUND, INTERNAL_TRANSFER
    private String description;
    private BigDecimal amount;
    private String date;
    private BigDecimal balanceAfter;
    private Long relatedCampaignId; // ID campaign đối tác (campaign nhận tiền khi đang gửi)
    private Long expenditureId;     // ID expenditure gốc (dùng cho chi tiêu & hoàn trả)
}
