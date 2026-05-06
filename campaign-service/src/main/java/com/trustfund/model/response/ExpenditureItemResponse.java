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
public class ExpenditureItemResponse {
    private Long id;
    private Long expenditureId;
    private String name;
    private Integer expectedQuantity;
    private Integer actualQuantity;
    private Integer quantityLeft;
    private BigDecimal actualPrice;
    private BigDecimal expectedPrice;
    private String expectedNote;
    private String expectedPurchaseLocation;
    private String actualPurchaseLocation;
    private String expectedBrand;
    private String actualBrand;
    private String expectedUnit;
    private String actualUnit;
    private Long catologyId;
    private String catologyName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
