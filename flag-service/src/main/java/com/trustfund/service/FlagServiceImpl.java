package com.trustfund.service;

import com.trustfund.model.Flag;
import com.trustfund.model.request.FlagRequest;
import com.trustfund.model.response.FlagResponse;
import com.trustfund.repository.FlagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FlagServiceImpl implements FlagService {

    private final FlagRepository flagRepository;

    @Override
    @Transactional
    public FlagResponse submitFlag(Long userId, FlagRequest request) {
        if (request.getPostId() == null && request.getCampaignId() == null) {
            throw new RuntimeException("Either postId or campaignId must be provided");
        }

        Flag flag = Flag.builder()
                .userId(userId)
                .postId(request.getPostId())
                .campaignId(request.getCampaignId())
                .reason(request.getReason())
                .status("PENDING")
                .build();

        Flag savedFlag = flagRepository.save(flag);
        return mapToResponse(savedFlag);
    }

    @Override
    public FlagResponse getFlagById(Long id) {
        Flag flag = flagRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Flag not found with id: " + id));
        return mapToResponse(flag);
    }

    @Override
    public Page<FlagResponse> getPendingFlags(Pageable pageable) {
        return flagRepository.findByStatus("PENDING", pageable)
                .map(this::mapToResponse);
    }

    @Override
    public Page<FlagResponse> getFlagsByPostId(Long postId, Pageable pageable) {
        return flagRepository.findByPostId(postId, pageable)
                .map(this::mapToResponse);
    }

    @Override
    public Page<FlagResponse> getFlagsByCampaignId(Long campaignId, Pageable pageable) {
        return flagRepository.findByCampaignId(campaignId, pageable)
                .map(this::mapToResponse);
    }

    @Override
    public Page<FlagResponse> getFlagsByUserId(Long userId, Pageable pageable) {
        return flagRepository.findByUserId(userId, pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional
    public FlagResponse reviewFlag(Long flagId, Long adminId, String status) {
        Flag flag = flagRepository.findById(flagId)
                .orElseThrow(() -> new RuntimeException("Flag not found with id: " + flagId));

        flag.setReviewedBy(adminId);
        flag.setStatus(status);

        Flag updatedFlag = flagRepository.save(flag);
        return mapToResponse(updatedFlag);
    }

    private FlagResponse mapToResponse(Flag flag) {
        return FlagResponse.builder()
                .id(flag.getId())
                .postId(flag.getPostId())
                .campaignId(flag.getCampaignId())
                .userId(flag.getUserId())
                .reviewedBy(flag.getReviewedBy())
                .reason(flag.getReason())
                .status(flag.getStatus())
                .createdAt(flag.getCreatedAt())
                .build();
    }
}
