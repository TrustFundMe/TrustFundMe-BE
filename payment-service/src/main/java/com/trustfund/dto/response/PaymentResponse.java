package com.trustfund.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PaymentResponse {
    private String paymentUrl;
    private String qrCode;
    private String paymentLinkId;
    private Long donationId;
    private Long campaignId;
    private BigDecimal donationAmount;
    private BigDecimal totalAmount;
    private String status;
}
