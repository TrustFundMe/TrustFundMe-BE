package com.trustfund.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditResultResponse {
    private List<AuditItem> items;
    private String summary;
    private boolean requiresAttention;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuditItem {
        private String itemName;
        private Double declaredPrice;
        private Double marketPrice;
        private Double priceRangeMin;
        private Double priceRangeMax;
        private Double priceDifferencePercentage;
        private boolean isSuspicious;
        private boolean productExists;
        private boolean productExistsByBrand;
        private List<String> evidenceUrls;
        private Integer quantity;
        private String statusMessage;
        private Boolean isLinkMatched;
        private String linkType;
    }
}
