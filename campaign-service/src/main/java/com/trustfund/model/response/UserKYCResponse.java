package com.trustfund.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserKYCResponse {
    private Long id;
    private Long userId;
    // ── OCR fields (từ CCCD/hộ chiếu) ──
    private String fullName;     // họ tên trên CCCD
    private String address;      // địa chỉ thường trú
    private String workplace;    // nơi làm việc
    private String taxId;        // mã số thuế cá nhân
    // ── Tài khoản user ──
    private String email;
    private String phoneNumber;
    // ── ID document ──
    private String idType;
    private String idNumber;
    private LocalDate issueDate;
    private LocalDate expiryDate;
    private String issuePlace;
    private String status;
}