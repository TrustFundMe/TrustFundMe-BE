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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserKYCServiceImpl implements UserKYCService {

    private final UserKYCRepository userKYCRepository;
    private final UserRepository userRepository;
    private final BankAccountRepository bankAccountRepository;

    @Override
    @Transactional
    public KYCResponse submitKYC(Long userId, SubmitKYCRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (userKYCRepository.existsByUserId(userId)) {
            throw new BadRequestException("KYC already submitted");
        }

        UserKYC userKYC = UserKYC.builder()
                .user(user)
                .idType(request.getIdType())
                .idNumber(request.getIdNumber())
                .issueDate(request.getIssueDate())
                .expiryDate(request.getExpiryDate())
                .issuePlace(request.getIssuePlace())
                .idImageFront(request.getIdImageFront())
                .idImageBack(request.getIdImageBack())
                .selfieImage(request.getSelfieImage())
                .status(KYCStatus.APPROVED) // Auto-approve when STAFF submits
                .build();

        UserKYC savedKYC = userKYCRepository.save(userKYC);

        // Auto-verify user and promote to FUND_OWNER if Bank is also verified
        if (shouldPromoteToFundOwner(userId)) {
            user.setRole(User.Role.FUND_OWNER);
        }
        userRepository.save(user);

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

        // Auto-verify user and promote to FUND_OWNER if Bank is also verified
        User user = userKYC.getUser();
        if (shouldPromoteToFundOwner(user.getId())) {
            user.setRole(User.Role.FUND_OWNER);
        }
        userRepository.save(user);

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

            // Check if user should be promoted to FUND_OWNER
            if (shouldPromoteToFundOwner(user.getId())) {
                user.setRole(User.Role.FUND_OWNER);
            }

            userRepository.save(user);
        }

        UserKYC savedKYC = userKYCRepository.save(userKYC);
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
