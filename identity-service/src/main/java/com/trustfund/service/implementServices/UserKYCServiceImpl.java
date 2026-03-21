package com.trustfund.service.implementServices;

import com.trustfund.model.User;
import com.trustfund.model.UserKYC;
import com.trustfund.model.enums.KYCStatus;
import com.trustfund.model.request.SubmitKYCRequest;
import com.trustfund.model.response.KYCResponse;
import com.trustfund.repository.UserKYCRepository;
import com.trustfund.repository.UserRepository;
import com.trustfund.repository.BankAccountRepository;
import com.trustfund.service.interfaceServices.UserKYCService;
import com.trustfund.exception.exceptions.NotFoundException;
import com.trustfund.exception.exceptions.BadRequestException;
import com.trustfund.client.NotificationServiceClient;
import com.trustfund.model.request.NotificationRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserKYCServiceImpl implements UserKYCService {

    private static final Logger log = LoggerFactory.getLogger(UserKYCServiceImpl.class);

    private final UserKYCRepository userKYCRepository;
    private final UserRepository userRepository;
    private final BankAccountRepository bankAccountRepository;
    private final NotificationServiceClient notificationServiceClient;

    @Override
    @Transactional
    public KYCResponse submitKYC(Long userId, SubmitKYCRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (userKYCRepository.existsByUserId(userId)) {
            throw new BadRequestException("KYC already submitted");
        }

        UserKYC userKYC = new UserKYC();
        userKYC.setUser(user);
        userKYC.setIdType(request.getIdType());
        userKYC.setIdNumber(request.getIdNumber());
        userKYC.setIssueDate(request.getIssueDate());
        userKYC.setExpiryDate(request.getExpiryDate());
        userKYC.setIssuePlace(request.getIssuePlace());
        userKYC.setIdImageFront(request.getIdImageFront());
        userKYC.setIdImageBack(request.getIdImageBack());
        userKYC.setSelfieImage(request.getSelfieImage());
        userKYC.setStatus(KYCStatus.APPROVED);

        UserKYC savedKYC = userKYCRepository.save(userKYC);

        if (User.Role.USER.equals(user.getRole())) {
            user.setRole(User.Role.FUND_OWNER);
            userRepository.save(user);
        }

        // Send notification for auto-approval
        sendKYCNotification(savedKYC, KYCStatus.APPROVED, null);

        return mapToResponse(savedKYC);
    }

    @Override
    public KYCResponse getMyKYC(Long userId) {
        UserKYC userKYC = userKYCRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("KYC record not found"));
        return mapToResponse(userKYC);
    }

    @Override
    public KYCResponse getKYCByUserId(Long userId) {
        UserKYC userKYC = userKYCRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("KYC record not found"));
        return mapToResponse(userKYC);
    }

    @Override
    @Transactional
    public KYCResponse resubmitKYC(Long userId, SubmitKYCRequest request) {
        UserKYC userKYC = userKYCRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("KYC record not found"));

        if (userKYC.getStatus() == KYCStatus.APPROVED) {
            throw new BadRequestException("Cannot resubmit APPROVED KYC");
        }

        // Check for duplicate ID number (only if ID number changed)
        if (!userKYC.getIdNumber().equals(request.getIdNumber())) {
            if (userKYCRepository.existsByIdNumber(request.getIdNumber())) {
                throw new BadRequestException("CCCD/ID number already exists in system");
            }
        }

        userKYC.setIdType(request.getIdType());
        userKYC.setIdNumber(request.getIdNumber());
        userKYC.setIssueDate(request.getIssueDate());
        userKYC.setExpiryDate(request.getExpiryDate());
        userKYC.setIssuePlace(request.getIssuePlace());
        userKYC.setIdImageFront(request.getIdImageFront());
        userKYC.setIdImageBack(request.getIdImageBack());
        userKYC.setSelfieImage(request.getSelfieImage());
        userKYC.setStatus(KYCStatus.APPROVED); // Auto-approve when STAFF resubmits
        userKYC.setRejectionReason(null);

        UserKYC savedKYC = userKYCRepository.save(userKYC);

        // Then promote to FUND_OWNER after KYC is updated
        User user = userKYC.getUser();
        if (User.Role.USER.equals(user.getRole())) {
            user.setRole(User.Role.FUND_OWNER);
            userRepository.save(user);
        }

        // Send notification for auto-approval
        sendKYCNotification(savedKYC, KYCStatus.APPROVED, null);

        return mapToResponse(savedKYC);
    }

    @Override
    public org.springframework.data.domain.Page<KYCResponse> getPendingKYCRequests(
            org.springframework.data.domain.Pageable pageable) {
        return userKYCRepository.findByStatus(KYCStatus.PENDING, pageable)
                .map(this::mapToResponse);
    }

    @Override
    public org.springframework.data.domain.Page<KYCResponse> getAllKYCRequests(
            org.springframework.data.domain.Pageable pageable) {
        return userKYCRepository.findAll(pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional
    public KYCResponse updateKYCStatus(Long kycId, KYCStatus status, String rejectionReason) {
        UserKYC userKYC = userKYCRepository.findById(kycId)
                .orElseThrow(() -> new NotFoundException("KYC record not found"));

        if (status == KYCStatus.REJECTED && (rejectionReason == null || rejectionReason.isBlank())) {
            throw new BadRequestException("Rejection reason is required when rejecting KYC");
        }

        userKYC.setStatus(status);
        userKYC.setRejectionReason(rejectionReason);

        if (status == KYCStatus.APPROVED) {
            User user = userKYC.getUser();
            user.setVerified(true);

            // Promote to FUND_OWNER for users with role USER
            if (User.Role.USER.equals(user.getRole())) {
                user.setRole(User.Role.FUND_OWNER);
            }

            userRepository.save(user);
        }

        UserKYC savedKYC = userKYCRepository.save(userKYC);

        // Send notification
        sendKYCNotification(savedKYC, status, rejectionReason);

        return mapToResponse(savedKYC);
    }

    /**
     * Check if user should be promoted to FUND_OWNER role.
     * User is promoted when BOTH KYC and Bank Account are verified.
     */
    private boolean shouldPromoteToFundOwner(Long userId) {
        // Check if user has at least one approved bank account
        boolean hasBankVerified = bankAccountRepository.findByUser_Id(userId).stream()
                .anyMatch(bank -> bank.getIsVerified() != null && bank.getIsVerified()
                        && "APPROVED".equals(bank.getStatus()));

        // KYC is already approved (we're in the approval flow)
        // So we only need to check if bank is also verified
        return hasBankVerified;
    }

    private void sendKYCNotification(UserKYC kyc, KYCStatus status, String rejectionReason) {
        try {
            boolean isApproved = status == KYCStatus.APPROVED;
            String title = isApproved ? "Xác thực danh tính (KYC) thành công" : "Xác thực danh tính (KYC) bị từ chối";
            String content = isApproved
                    ? "Chúc mừng! Hồ sơ KYC của bạn đã được duyệt thành công. Bây giờ bạn có thể tạo chiến dịch gây quỹ."
                    : "Rất tiếc, hồ sơ KYC của bạn đã bị từ chối. Lý do: " + rejectionReason;

            java.util.Map<String, Object> notificationData = new java.util.HashMap<>();
            notificationData.put("kycId", kyc.getId());
            notificationData.put("status", status.name());

            NotificationRequest notificationRequest = NotificationRequest.builder()
                    .userId(kyc.getUser().getId())
                    .type(isApproved ? "KYC_APPROVED" : "KYC_REJECTED")
                    .targetId(kyc.getId())
                    .targetType("KYC")
                    .title(title)
                    .content(content)
                    .data(notificationData)
                    .build();

            log.info("[UserKYCService] Sending KYC notification to user {} for KYC {}",
                    kyc.getUser().getId(), kyc.getId());
            notificationServiceClient.sendNotification(notificationRequest);
        } catch (Exception e) {
            log.error("Error sending KYC notification for user {}: {}", kyc.getUser().getId(), e.getMessage());
        }
    }

    private KYCResponse mapToResponse(UserKYC kyc) {
        return KYCResponse.builder()
                .id(kyc.getId())
                .userId(kyc.getUser().getId())
                .fullName(kyc.getUser().getFullName())
                .email(kyc.getUser().getEmail())
                .phoneNumber(kyc.getUser().getPhoneNumber())
                .idType(kyc.getIdType())
                .idNumber(kyc.getIdNumber())
                .issueDate(kyc.getIssueDate())
                .expiryDate(kyc.getExpiryDate())
                .issuePlace(kyc.getIssuePlace())
                .idImageFront(kyc.getIdImageFront())
                .idImageBack(kyc.getIdImageBack())
                .selfieImage(kyc.getSelfieImage())
                .status(kyc.getStatus())
                .rejectionReason(kyc.getRejectionReason())
                .createdAt(kyc.getCreatedAt())
                .updatedAt(kyc.getUpdatedAt())
                .build();
    }
}
