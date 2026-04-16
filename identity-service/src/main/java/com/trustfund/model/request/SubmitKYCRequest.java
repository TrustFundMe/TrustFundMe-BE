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
    // ── OCR-extracted fields (from CCCD/passport scan) ──
    /** Họ tên đầy đủ trên CCCD/hộ chiếu */
    @NotBlank(message = "Họ tên không được để trống")
    private String fullName;

    /** Địa chỉ đăng ký thường trú trên CCCD/hộ chiếu */
    @NotBlank(message = "Địa chỉ không được để trống")
    private String address;

    /** Nơi làm việc hiện tại (optional) */
    private String workplace;

    /** Mã số thuế cá nhân (nếu có) */
    private String taxId;

    // ── ID document fields ──
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
