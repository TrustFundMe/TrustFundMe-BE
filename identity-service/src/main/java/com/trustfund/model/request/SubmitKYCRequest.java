package com.trustfund.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmitKYCRequest {
    @NotBlank(message = "Loại định danh không được để trống")
    private String idType;

    @NotBlank(message = "Số định danh không được để trống")
    private String idNumber;

    @NotNull(message = "Vui lòng nhập ngày cấp")
    private LocalDate issueDate;

    @NotNull(message = "Vui lòng nhập ngày hết hạn")
    private LocalDate expiryDate;

    @NotBlank(message = "Vui lòng nhập nơi cấp")
    private String issuePlace;

    @NotBlank(message = "Vui lòng tải ảnh mặt trước")
    private String idImageFront;

    // Không bắt buộc với Passport (không có mặt sau)
    private String idImageBack;

    @NotBlank(message = "Vui lòng tải ảnh chân dung")
    private String selfieImage;
}
