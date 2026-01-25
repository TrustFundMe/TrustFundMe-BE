package com.trustfund.service;

import com.trustfund.model.Expenditure;
import com.trustfund.model.request.CreateExpenditureRequest;
import com.trustfund.model.request.UpdateExpenditureRequest;

import java.util.List;

public interface ExpenditureService {
    List<Expenditure> getAll();
    Expenditure getById(Long id);
    List<Expenditure> getByCampaignId(Long campaignId);
    Expenditure create(CreateExpenditureRequest request);
    Expenditure update(Long id, UpdateExpenditureRequest request);
    void delete(Long id);
}
