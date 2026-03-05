package com.trustfund.dto.request;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class CreatePaymentRequest {
    private Long donorId;
    private Long campaignId;
    private BigDecimal donationAmount;
    private BigDecimal tipAmount;
    private String description;
    private List<DonationItemRequest> items;

    @Data
    public static class DonationItemRequest {
        private Long expenditureItemId;
        private Integer quantity;
        private BigDecimal amount;
    }
}
