package com.trustfund.service;

import com.trustfund.model.response.AggregatedTransactionResponse;
import java.util.List;

public interface CampaignTransactionService {
    List<AggregatedTransactionResponse> getCampaignTransactionHistory(Long campaignId);
}
