package com.trustfund.service.implementServices;

import com.trustfund.model.User;
import com.trustfund.model.UserKYC;
import com.trustfund.model.enums.KYCStatus;
import com.trustfund.model.request.SubmitKYCRequest;
import com.trustfund.model.response.KYCResponse;
import com.trustfund.repository.UserKYCRepository;
import com.trustfund.repository.UserRepository;
import com.trustfund.service.interfaceServices.UserKYCService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserKYCServiceImpl implements UserKYCService {

    private final UserKYCRepository userKYCRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public KYCResponse submitKYC(Long userId, SubmitKYCRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (userKYCRepository.existsByUserId(userId)) {
            throw new RuntimeException("KYC already submitted");
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
                .status(KYCStatus.PENDING)
                .build();

        UserKYC savedKYC = userKYCRepository.save(userKYC);
        return mapToResponse(savedKYC);
    }

    @Override
    public KYCResponse getMyKYC(Long userId) {
        UserKYC userKYC = userKYCRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("KYC record not found"));
        return mapToResponse(userKYC);
    }

    @Override
    public KYCResponse getKYCByUserId(Long userId) {
        UserKYC userKYC = userKYCRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("KYC record not found"));
        return mapToResponse(userKYC);
    }

    @Override
    @Transactional
    public KYCResponse resubmitKYC(Long userId, SubmitKYCRequest request) {
        UserKYC userKYC = userKYCRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("KYC record not found"));

        if (userKYC.getStatus() == KYCStatus.APPROVED) {
            throw new RuntimeException("Cannot resubmit APPROVED KYC");
        }

        userKYC.setIdType(request.getIdType());
        userKYC.setIdNumber(request.getIdNumber());
        userKYC.setIssueDate(request.getIssueDate());
        userKYC.setExpiryDate(request.getExpiryDate());
        userKYC.setIssuePlace(request.getIssuePlace());
        userKYC.setIdImageFront(request.getIdImageFront());
        userKYC.setIdImageBack(request.getIdImageBack());
        userKYC.setSelfieImage(request.getSelfieImage());
        userKYC.setStatus(KYCStatus.PENDING);
        userKYC.setRejectionReason(null);

        UserKYC savedKYC = userKYCRepository.save(userKYC);
        return mapToResponse(savedKYC);
    }

    @Override
    public org.springframework.data.domain.Page<KYCResponse> getPendingKYCRequests(
            org.springframework.data.domain.Pageable pageable) {
        return userKYCRepository.findByStatus(KYCStatus.PENDING, pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional
    public KYCResponse updateKYCStatus(Long kycId, KYCStatus status, String rejectionReason) {
        UserKYC userKYC = userKYCRepository.findById(kycId)
                .orElseThrow(() -> new RuntimeException("KYC record not found"));

        if (status == KYCStatus.REJECTED && (rejectionReason == null || rejectionReason.isBlank())) {
            throw new RuntimeException("Rejection reason is required when rejecting KYC");
        }

        userKYC.setStatus(status);
        userKYC.setRejectionReason(rejectionReason);

        if (status == KYCStatus.APPROVED) {
            User user = userKYC.getUser();
            user.setVerified(true);
            userRepository.save(user);
        }

        UserKYC savedKYC = userKYCRepository.save(userKYC);
        return mapToResponse(savedKYC);
    }

    private KYCResponse mapToResponse(UserKYC kyc) {
        return KYCResponse.builder()
                .id(kyc.getId())
                .userId(kyc.getUser().getId())
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
