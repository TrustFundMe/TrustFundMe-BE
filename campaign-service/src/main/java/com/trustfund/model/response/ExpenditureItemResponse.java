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
    private String category;
    private Integer expectedQuantity;
    private Integer actualQuantity;
    private Integer quantityLeft;
    private BigDecimal actualPrice;
    private BigDecimal expectedPrice;
    private String note;
    private String purchaseLocation;
    private String brand;
    private String unit;
    private Long catologyId;
    private String catologyName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
