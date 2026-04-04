package com.trustfund.model.request;

import com.trustfund.model.enums.InternalTransactionStatus;
import com.trustfund.model.enums.InternalTransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class InternalTransactionRequest {

    private Long fromCampaignId;

    private Long toCampaignId;

    @NotNull(message = "Số tiền không được để trống")
    @DecimalMin(value = "0.01", message = "Số tiền phải lớn hơn 0")
    private BigDecimal amount;

    @NotNull(message = "Loại giao dịch không được để trống")
    private InternalTransactionType type;

    private String reason;

    private Long createdByStaffId;

    private Long evidenceImageId;

    private InternalTransactionStatus status;

}
