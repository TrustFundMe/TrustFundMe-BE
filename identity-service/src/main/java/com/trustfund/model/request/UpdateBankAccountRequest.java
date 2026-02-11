package com.trustfund.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateBankAccountRequest {

    @NotBlank(message = "Mã ngân hàng không được để trống")
    @Size(min = 2, max = 50, message = "Mã ngân hàng phải từ 2 đến 50 ký tự")
    private String bankCode;

    @NotBlank(message = "Số tài khoản không được để trống")
    @Size(min = 6, max = 50, message = "Số tài khoản phải từ 6 đến 50 ký tự")
    @Pattern(regexp = "\\d+", message = "Số tài khoản chỉ được chứa các chữ số")
    private String accountNumber;

    @NotBlank(message = "Tên chủ tài khoản không được để trống")
    @Size(min = 6, max = 255, message = "Tên chủ tài khoản phải từ 6 đến 255 ký tự")
    private String accountHolderName;
}
