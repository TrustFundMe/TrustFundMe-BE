package com.trustfund.controller;

import com.trustfund.model.BankAccount;
import com.trustfund.model.User;
import com.trustfund.model.response.UserInfoResponse;
import com.trustfund.model.response.UserVerificationStatusResponse;
import com.trustfund.model.response.UserKYCResponse;
import com.trustfund.repository.BankAccountRepository;
import com.trustfund.repository.UserRepository;
import com.trustfund.repository.UserKYCRepository;
import com.trustfund.service.interfaceServices.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/internal/users")
@RequiredArgsConstructor
@Tag(name = "Internal", description = "API nội bộ cho service khác gọi (VD: campaign-service)")
public class InternalUserController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final BankAccountRepository bankAccountRepository;
    private final UserKYCRepository userKYCRepository; // Added dependency

    @GetMapping("/{id}")
    @Operation(summary = "Lấy thông tin user theo ID (dùng nội bộ bởi các service khác)")
    public ResponseEntity<UserInfoResponse> getUserInfo(@PathVariable("id") Long id) {
        return userRepository.findById(id)
                .map(u -> ResponseEntity.ok(UserInfoResponse.builder()
                        .id(u.getId())
                        .fullName(u.getFullName())
                        .avatarUrl(u.getAvatarUrl())
                        .email(u.getEmail())
                        .trustScore(u.getTrustScore())
                        .build()))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/exists")
    @Operation(summary = "Kiểm tra user có tồn tại không (dùng bởi campaign-service khi tạo campaign)")
    public ResponseEntity<Void> exists(@PathVariable("id") Long id) {
        if (userRepository.existsById(id)) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/{id}/verification-status")
    @Operation(summary = "Lấy trạng thái xác thực KYC và Bank Account của user")
    public ResponseEntity<UserVerificationStatusResponse> getVerificationStatus(@PathVariable("id") Long id) {
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

    @PutMapping("/{id}/upgrade-role")
    @Operation(summary = "Nâng cấp role user lên FUND_OWNER (dùng khi duyệt campaign)")
    public ResponseEntity<Void> upgradeRole(@PathVariable("id") Long id) {
        userService.upgradeToFundOwner(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/upgrade-to-fund-donor")
    @Operation(summary = "Nâng cấp role user lên FUND_DONOR (dùng sau khi KYC được duyệt)")
    public ResponseEntity<Void> upgradeToFundDonor(@PathVariable("id") Long id) {
        userService.upgradeToFundDonor(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/name")
    @Operation(summary = "Lấy full name của user theo ID (dùng nội bộ)")
    public ResponseEntity<String> getUserFullName(@PathVariable("id") Long id) {
        return userRepository.findById(id)
                .map(u -> ResponseEntity.ok(u.getFullName()))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/kyc")
    @Operation(summary = "Lấy KYC đầy đủ của user (dùng bởi campaign-service)")
    public ResponseEntity<UserKYCResponse> getUserKYC(@PathVariable("id") Long id) {
        return userKYCRepository.findByUserId(id)
                .map(kyc -> ResponseEntity.ok(UserKYCResponse.builder()
                        .id(kyc.getId())
                        .userId(kyc.getUser().getId())
                        .fullName(kyc.getFullNameOcr())
                        .address(kyc.getAddress())
                        .workplace(kyc.getWorkplace())
                        .taxId(kyc.getTaxId())
                        .email(kyc.getUser().getEmail())
                        .phoneNumber(kyc.getUser().getPhoneNumber())
                        .idType(kyc.getIdType())
                        .idNumber(kyc.getIdNumber())
                        .issueDate(kyc.getIssueDate())
                        .expiryDate(kyc.getExpiryDate())
                        .issuePlace(kyc.getIssuePlace())
                        .status(kyc.getStatus().name())
                        .build()))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/primary-bank")
    @Operation(summary = "Lấy tài khoản ngân hàng chính của user (ưu tiên đã duyệt)")
    public ResponseEntity<com.trustfund.model.response.BankAccountResponse> getPrimaryBankAccount(@PathVariable("id") Long id) {
        java.util.List<com.trustfund.model.BankAccount> accounts = bankAccountRepository.findByUser_Id(id);
        if (accounts.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        // Ưu tiên lấy cái APPROVED, nếu không có thì lấy cái đầu tiên tìm thấy
        com.trustfund.model.BankAccount bestAcc = accounts.stream()
                .filter(acc -> "APPROVED".equals(acc.getStatus()))
                .findFirst()
                .orElse(accounts.get(0));

        return ResponseEntity.ok(com.trustfund.model.response.BankAccountResponse.builder()
                .id(bestAcc.getId())
                .userId(bestAcc.getUser().getId())
                .bankCode(bestAcc.getBankCode())
                .accountNumber(bestAcc.getAccountNumber())
                .accountHolderName(bestAcc.getAccountHolderName())
                .isVerified(bestAcc.getIsVerified())
                .status(bestAcc.getStatus())
                .build());
    }

    @GetMapping("/staff-ids")
    @Operation(summary = "Lấy danh sách ID của tất cả nhân viên đang hoạt động (dùng nội bộ)")
    public ResponseEntity<java.util.List<Long>> getStaffIds() {
        java.util.List<Long> staffIds = userRepository.findAllByRole(User.Role.STAFF).stream()
                .filter(User::getIsActive)
                .map(User::getId)
                .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(staffIds);
    }

    @PutMapping("/{id}/update-trust-score")
    @Operation(summary = "Cập nhật điểm uy tín của user (dùng bởi campaign-service)")
    public ResponseEntity<Void> updateTrustScore(@PathVariable("id") Long id, @org.springframework.web.bind.annotation.RequestParam("delta") int delta) {
        System.out.println("IDENTITY_SERVICE: Updating trust score for user " + id + " with delta " + delta);
        return userRepository.findById(id)
                .map(u -> {
                    int current = u.getTrustScore() != null ? u.getTrustScore() : 0;
                    u.setTrustScore(current + delta);
                    userRepository.save(u);
                    System.out.println("IDENTITY_SERVICE: User " + id + " new score: " + u.getTrustScore());
                    return ResponseEntity.ok().<Void>build();
                })
                .orElseGet(() -> {
                    System.err.println("IDENTITY_SERVICE: User " + id + " not found for score update");
                    return ResponseEntity.notFound().build();
                });
    }

    @GetMapping("/leaderboard")
    @Operation(summary = "Lấy bảng xếp hạng điểm uy tín (dùng bởi campaign-service)")
    public ResponseEntity<List<UserInfoResponse>> getLeaderboard(@org.springframework.web.bind.annotation.RequestParam(value = "page", defaultValue = "0") int page,
                                                                 @org.springframework.web.bind.annotation.RequestParam(value = "size", defaultValue = "10") int size) {
        org.springframework.data.domain.Page<User> userPage = userRepository.findAll(
                org.springframework.data.domain.PageRequest.of(page, size,
                        org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "trustScore")));

        List<UserInfoResponse> list = userPage.getContent().stream()
                .map(u -> UserInfoResponse.builder()
                        .id(u.getId())
                        .fullName(u.getFullName())
                        .avatarUrl(u.getAvatarUrl())
                        .email(u.getEmail())
                        .trustScore(u.getTrustScore())
                        .build())
                .collect(java.util.stream.Collectors.toList());

        return ResponseEntity.ok(list);
    }
}
