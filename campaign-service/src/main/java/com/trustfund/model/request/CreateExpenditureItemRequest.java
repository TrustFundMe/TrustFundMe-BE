package com.trustfund.model.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateExpenditureItemRequest {

    @NotBlank(message = "Danh mục không được để trống")
    private String category;

    @NotNull(message = "Số lượng không được để trống")
    private Integer quantity;

    @NotNull(message = "Giá không được để trống")
    @NotNull(message = "Giá không được để trống")
    @DecimalMin(value = "0.0", message = "Giá không được nhỏ hơn 0")
    private BigDecimal price;

    @NotNull(message = "Giá dự kiến không được để trống")
    @DecimalMin(value = "0.0", message = "Giá dự kiến không được nhỏ hơn 0")
    private BigDecimal expectedPrice;

    private String note;
}
