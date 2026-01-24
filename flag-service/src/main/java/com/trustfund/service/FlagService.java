package com.trustfund.service;

import com.trustfund.model.request.FlagRequest;
import com.trustfund.model.response.FlagResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FlagService {
    FlagResponse submitFlag(Long userId, FlagRequest request);

    Page<FlagResponse> getPendingFlags(Pageable pageable);

    FlagResponse reviewFlag(Long flagId, Long adminId, String status);
}
