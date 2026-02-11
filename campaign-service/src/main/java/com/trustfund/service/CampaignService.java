package com.trustfund.service;

import com.trustfund.model.request.CreateCampaignRequest;
import com.trustfund.model.request.UpdateCampaignRequest;
import com.trustfund.model.response.CampaignResponse;

import java.util.List;

public interface CampaignService {

    List<CampaignResponse> getAll();

    CampaignResponse getById(Long id);

    List<CampaignResponse> getByFundOwnerId(Long fundOwnerId);

    CampaignResponse create(CreateCampaignRequest request);

    CampaignResponse update(Long id, UpdateCampaignRequest request);

    CampaignResponse markAsDeleted(Long id);

    List<CampaignResponse> getByStatus(String status);

    CampaignResponse reviewCampaign(Long id, Long staffId, String status, String rejectionReason);
}
