package com.trustfund.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request body to create a refund transaction (return excess funds)")
public class CreateRefundRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @Schema(description = "Amount to refund", example = "500000.00")
    private BigDecimal amount;

    @NotBlank(message = "Proof URL is required")
    @Schema(description = "URL of the transfer proof (screenshot)", example = "https://cloudinary.com/...)")
    private String proofUrl;

    // Người gửi (fund owner) - bank info
    @Schema(description = "Mã ngân hàng người gửi", example = "VCB")
    private String fromBankCode;

    @Schema(description = "Số tài khoản người gửi", example = "1234567890")
    private String fromAccountNumber;

    @Schema(description = "Tên chủ tài khoản người gửi", example = "Nguyen Van A")
    private String fromAccountHolderName;

    // Người nhận (admin) - bank info
    @Schema(description = "Mã ngân hàng người nhận", example = "TPB")
    private String toBankCode;

    @Schema(description = "Số tài khoản người nhận", example = "0987654321")
    private String toAccountNumber;

    @Schema(description = "Tên chủ tài khoản người nhận", example = "TrustFund Admin")
    private String toAccountHolderName;
}
