package com.trustfund.model.request;

import jakarta.validation.constraints.DecimalMin;
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
public class UpdateCampaignRequest {

    @Size(min = 10, max = 255, message = "Tiêu đề phải từ 10 đến 255 ký tự")
    private String title;

    @Size(min = 50, max = 10000, message = "Mô tả phải từ 50 đến 10,000 ký tự")
    private String description;

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

    @DecimalMin(value = "0.0", message = "Số dư không được nhỏ hơn 0")
    private BigDecimal balance;

    private Long approvedByStaff; // id staff duyệt
    private LocalDateTime approvedAt;
}
