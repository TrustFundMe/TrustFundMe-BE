package com.trustfund.model.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class CreateCampaignRequest {

    @NotNull(message = "fundOwnerId không được để trống")
    private Long fundOwnerId;

    @NotBlank(message = "Tiêu đề không được để trống")
    @Size(min = 10, max = 255, message = "Tiêu đề phải từ 10 đến 255 ký tự")
    private String title;

    @NotBlank(message = "Mô tả không được để trống")
    @Size(min = 50, max = 10000, message = "Mô tả phải từ 50 đến 10,000 ký tự")
    private String description;

    @NotBlank(message = "Danh mục không được để trống")
    @Size(max = 100)
    private String category;

    private LocalDateTime startDate;
    private LocalDateTime endDate;

    @Size(max = 50)
    private String status;

    @Size(max = 50)
    private String type;

    @Size(max = 2000)
    private String thankMessage;

    @NotNull(message = "Số dư không được để trống")
    @DecimalMin(value = "0.0", message = "Số dư không được nhỏ hơn 0")
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;
}
