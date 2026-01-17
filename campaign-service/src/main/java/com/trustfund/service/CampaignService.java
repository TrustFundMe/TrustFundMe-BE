package com.trustfund.service;

import com.trustfund.model.Campaign;
import com.trustfund.model.request.CreateCampaignRequest;
import com.trustfund.model.request.UpdateCampaignRequest;

import java.util.List;

public interface CampaignService {

    List<Campaign> getAll();

    Campaign getById(Long id);

    List<Campaign> getByFundOwnerId(Long fundOwnerId);

    Campaign create(CreateCampaignRequest request);

    Campaign update(Long id, UpdateCampaignRequest request);

    Campaign markAsDeleted(Long id);
}
