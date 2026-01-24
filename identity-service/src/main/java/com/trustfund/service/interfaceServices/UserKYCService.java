package com.trustfund.service.interfaceServices;

import com.trustfund.model.enums.KYCStatus;
import com.trustfund.model.request.SubmitKYCRequest;
import com.trustfund.model.response.KYCResponse;

public interface UserKYCService {
    KYCResponse submitKYC(Long userId, SubmitKYCRequest request);

    KYCResponse resubmitKYC(Long userId, SubmitKYCRequest request);

    KYCResponse getMyKYC(Long userId);

    KYCResponse getKYCByUserId(Long userId);

    org.springframework.data.domain.Page<KYCResponse> getPendingKYCRequests(
            org.springframework.data.domain.Pageable pageable);

    KYCResponse updateKYCStatus(Long kycId, KYCStatus status, String rejectionReason);
}
