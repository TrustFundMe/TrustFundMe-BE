package com.trustfund.controller;

import com.trustfund.model.BankAccount;
import com.trustfund.model.User;
import com.trustfund.model.response.UserVerificationStatusResponse;
import com.trustfund.repository.BankAccountRepository;
import com.trustfund.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import com.trustfund.repository.UserKYCRepository; // Added import

@RestController
@RequestMapping("/api/internal/users")
@RequiredArgsConstructor
@Tag(name = "Internal", description = "API nội bộ cho service khác gọi (VD: campaign-service)")
public class InternalUserController {

    private final UserRepository userRepository;
    private final BankAccountRepository bankAccountRepository;
    private final UserKYCRepository userKYCRepository; // Added dependency

    @GetMapping("/{id}/exists")
    @Operation(summary = "Kiểm tra user có tồn tại không (dùng bởi campaign-service khi tạo campaign)")
    public ResponseEntity<Void> exists(@PathVariable Long id) {
        if (userRepository.existsById(id)) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/{id}/verification-status")
    @Operation(summary = "Lấy trạng thái xác thực KYC và Bank Account của user")
    public ResponseEntity<UserVerificationStatusResponse> getVerificationStatus(@PathVariable Long id) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        List<BankAccount> bankAccounts = bankAccountRepository.findByUser_Id(id);
        boolean bankVerified = bankAccounts.stream()
                .anyMatch(acc -> "APPROVED".equals(acc.getStatus()) && Boolean.TRUE.equals(acc.getIsVerified()));

        boolean kycVerified = userKYCRepository.findByUserId(id)
                .map(kyc -> com.trustfund.model.enums.KYCStatus.APPROVED.equals(kyc.getStatus()))
                .orElse(false);

        return ResponseEntity.ok(UserVerificationStatusResponse.builder()
                .kycVerified(kycVerified)
                .bankVerified(bankVerified)
                .build());
    }
}
