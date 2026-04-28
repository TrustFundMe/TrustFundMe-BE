package com.trustfund.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpenditureCatologyResponse {
    private Long id;
    private Long expenditureId;
    private String name;
    private String description;
    private BigDecimal expectedAmount;
    private BigDecimal actualAmount;
    private BigDecimal balance;
    private String withdrawalCondition;
    private List<ExpenditureItemResponse> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
