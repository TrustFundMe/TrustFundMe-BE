package com.trustfund.service;

import com.trustfund.model.request.FlagRequest;
import com.trustfund.model.response.FlagResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FlagService {
    FlagResponse submitFlag(Long userId, FlagRequest request);

    FlagResponse getFlagById(Long id);

    Page<FlagResponse> getPendingFlags(Pageable pageable);

    Page<FlagResponse> getFlagsByPostId(Long postId, Pageable pageable);

    Page<FlagResponse> getFlagsByCampaignId(Long campaignId, Pageable pageable);

    Page<FlagResponse> getFlagsByUserId(Long userId, Pageable pageable);

    FlagResponse reviewFlag(Long flagId, Long adminId, String status);
}
