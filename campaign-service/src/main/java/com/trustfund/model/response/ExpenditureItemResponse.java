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
    private Integer quantity;
    private Integer actualQuantity;
    private Integer quantityLeft;
    private Integer reservations;
    private BigDecimal price;
    private BigDecimal expectedPrice;
    private String note;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
