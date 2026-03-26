package com.trustfund.service;

import com.trustfund.model.request.CreateCampaignRequest;
import com.trustfund.model.request.UpdateCampaignRequest;
import com.trustfund.model.response.CampaignResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CampaignService {

    java.util.List<CampaignResponse> getAll();
    Page<CampaignResponse> getAll(Pageable pageable);

    CampaignResponse getById(Long id);

    List<CampaignResponse> getByFundOwnerId(Long fundOwnerId);

    Page<CampaignResponse> getByFundOwnerIdPaginated(Long fundOwnerId, Pageable pageable);

    CampaignResponse create(CreateCampaignRequest request);

    CampaignResponse update(Long id, UpdateCampaignRequest request);

    CampaignResponse markAsDeleted(Long id);

    List<CampaignResponse> getByStatus(String status);

    List<CampaignResponse> getByCategoryId(Long categoryId);

    CampaignResponse reviewCampaign(Long id, Long staffId, String status, String rejectionReason);
}
