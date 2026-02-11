package com.trustfund.model.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateFundraisingGoalRequest {

    @DecimalMin(value = "10000.0", message = "Số tiền mục tiêu tối thiểu phải là 10,000")
    private BigDecimal targetAmount;

    @Size(min = 10, max = 1000, message = "Mô tả phải từ 10 đến 1000 ký tự")
    private String description;

    private Boolean isActive;
}
