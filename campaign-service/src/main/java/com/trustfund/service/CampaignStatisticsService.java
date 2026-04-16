package com.trustfund.service;

import com.trustfund.model.response.CampaignStatisticsResponse;

public interface CampaignStatisticsService {
    CampaignStatisticsResponse getStatisticsByFundOwner(Long fundOwnerId);
}
