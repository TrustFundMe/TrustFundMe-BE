package com.trustfund.dto.response;

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
public class MyDonationImpactResponse {
    private Long donationId;
    private Long donorId;
    private Long campaignId;
    private String campaignTitle;
    private BigDecimal donationAmount;
    private BigDecimal tipAmount;
    private BigDecimal totalAmount;
    private String status;
    private Boolean anonymous;
    private LocalDateTime createdAt;
}
