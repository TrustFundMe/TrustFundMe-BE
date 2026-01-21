package com.trustfund.service.impl;

import com.trustfund.model.CampaignFollow;
import com.trustfund.model.CampaignFollowId;
import com.trustfund.model.response.CampaignFollowerResponse;
import com.trustfund.repository.CampaignFollowRepository;
import com.trustfund.repository.CampaignRepository;
import com.trustfund.service.CampaignFollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CampaignFollowServiceImpl implements CampaignFollowService {

    private final CampaignFollowRepository campaignFollowRepository;
    private final CampaignRepository campaignRepository;

    @Override
    @Transactional
    public void follow(Long campaignId, Long userId) {
        // verify campaign exists
        campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Campaign not found: " + campaignId));

        CampaignFollowId id = new CampaignFollowId(campaignId, userId);
        if (campaignFollowRepository.existsById(id)) {
            // idempotent
            return;
        }

        CampaignFollow follow = CampaignFollow.builder()
                .id(id)
                .build();
        campaignFollowRepository.save(follow);
    }

    @Override
    @Transactional
    public void unfollow(Long campaignId, Long userId) {
        CampaignFollowId id = new CampaignFollowId(campaignId, userId);
        if (!campaignFollowRepository.existsById(id)) {
            // idempotent
            return;
        }
        campaignFollowRepository.deleteById(id);
    }

    @Override
    public boolean isFollowing(Long campaignId, Long userId) {
        return campaignFollowRepository.existsById_CampaignIdAndId_UserId(campaignId, userId);
    }

    @Override
    public long countFollowers(Long campaignId) {
        return campaignFollowRepository.countById_CampaignId(campaignId);
    }

    @Override
    public List<Long> getMyFollowedCampaignIds(Long userId) {
        return campaignFollowRepository.findCampaignIdsFollowedByUser(userId);
    }

    @Override
    public List<CampaignFollowerResponse> getFollowersByCampaignId(Long campaignId) {
        return campaignFollowRepository.findById_CampaignIdOrderByFollowedAtDesc(campaignId)
                .stream()
                .map(cf -> CampaignFollowerResponse.builder()
                        .userId(cf.getId().getUserId())
                        .followedAt(cf.getFollowedAt())
                        .build())
                .collect(Collectors.toList());
    }
}

