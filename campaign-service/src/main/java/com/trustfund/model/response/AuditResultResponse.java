package com.trustfund.model.response;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
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
    @JsonProperty("detectedItems")
    @JsonAlias({ "items", "detectedItems" })
    private List<AuditItem> detectedItems;

    private String summary;
    private String recommendation;
    private String riskLevel;
    private Double riskScore;
    private List<String> redFlags;
    private List<String> spendingAnalysis;
    private VendorInfo vendorInfo;
    private boolean requiresAttention;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuditItem {
        @JsonProperty("name")
        @JsonAlias({ "name", "itemName" })
        private String name;

        private String plannedCategory;
        private Integer quantity;

        @JsonProperty("unitPrice")
        @JsonAlias({ "unitPrice", "declaredPrice" })
        private Double unitPrice; // Phân bổ từ expectedUnitPrice

        @JsonProperty("marketUnitPrice")
        @JsonAlias({ "marketUnitPrice", "marketPrice" })
        private Double marketUnitPrice;

        private Double total;
        private String priceStatus; // MATCHED | OVERPRICED | UNDERPRICED

        @JsonProperty("deviationPercentage")
        @JsonAlias({ "deviationPercentage", "priceDifferencePercentage" })
        private Double deviationPercentage;

        private List<String> evidenceUrls;

        // Diagnostic fields preserved from previous logic
        private String marketPriceRange;
        @JsonProperty("marketPriceMin")
        @JsonAlias({ "marketPriceMin", "priceRangeMin" })
        private Double marketPriceMin;

        @JsonProperty("marketPriceMax")
        @JsonAlias({ "marketPriceMax", "priceRangeMax" })
        private Double marketPriceMax;
        private String statusMessage;
        private String unit;
        private Boolean productExists;
        private Boolean productExistsByBrand;
        private String linkType;

        // New Geographic Defense Fields
        private String geographicEvidenceUrl; // Link chứng minh giá khu vực (báo chí, báo cáo giá địa phương)
        private Double logisticsScore; // Chỉ số rủi ro logistics (0-1: 0 là bình thường, 1 là cô lập/khó vận chuyển)
        private Double vendorTrustScore; // Điểm tin cậy của điểm bán (xác thực MST, uy tín cửa hàng)
        private String geographicContextSummary; // Giải thích ngắn gọn về yếu tố vùng miền cho item này
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VendorInfo {
        private String name;
        private String address;
        private String phone;
    }
}
