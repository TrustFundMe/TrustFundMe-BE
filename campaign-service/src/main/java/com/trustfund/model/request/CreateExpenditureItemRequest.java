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

    @NotBlank(message = "Tên hàng hóa không được để trống")
    private String name;

    private Integer expectedQuantity;

    private Integer actualQuantity;

    @DecimalMin(value = "0.0", message = "Giá thực tế không được nhỏ hơn 0")
    private BigDecimal actualPrice;

    @DecimalMin(value = "0.0", message = "Giá dự kiến không được nhỏ hơn 0")
    private BigDecimal expectedPrice;

    private String expectedNote;

    private String expectedPurchaseLocation;

    private String expectedBrand;

    private String actualBrand;
    private Long catologyId;
    private String expectedUnit;

    private String actualUnit;

    private String actualPurchaseLocation;
}
