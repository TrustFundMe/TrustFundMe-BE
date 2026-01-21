package com.trustfund.service;

import com.trustfund.model.response.CampaignFollowerResponse;

import java.util.List;

public interface CampaignFollowService {

    void follow(Long campaignId, Long userId);

    void unfollow(Long campaignId, Long userId);

    boolean isFollowing(Long campaignId, Long userId);

    long countFollowers(Long campaignId);

    List<Long> getMyFollowedCampaignIds(Long userId);

    List<CampaignFollowerResponse> getFollowersByCampaignId(Long campaignId);
}

