package com.trustfund.service;

import com.trustfund.model.request.CampaignCategoryRequest;
import com.trustfund.model.response.CampaignCategoryResponse;

import java.util.List;

public interface CampaignCategoryService {
    List<CampaignCategoryResponse> getAll();

    CampaignCategoryResponse getById(Long id);

    CampaignCategoryResponse create(CampaignCategoryRequest request);

    CampaignCategoryResponse update(Long id, CampaignCategoryRequest request);

    void delete(Long id);
}
