package com.trustfund.model.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class UpdateExpenditureActualsRequest {

    @NotNull(message = "Danh sách hạng mục không được để trống")
    private List<UpdateItem> items;

    @Data
    public static class UpdateItem {
        @NotNull(message = "ID hạng mục không được để trống")
        private Long id;

        @Min(value = 0, message = "Số lượng thực tế không được nhỏ hơn 0")
        private Integer actualQuantity;

        @DecimalMin(value = "0.0", message = "Giá thực tế không được nhỏ hơn 0")
        private BigDecimal price; // Actual Price
    }
}
