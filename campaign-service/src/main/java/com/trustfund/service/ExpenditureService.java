package com.trustfund.service;

import com.trustfund.model.Expenditure;
import com.trustfund.model.ExpenditureItem;
import com.trustfund.model.request.CreateExpenditureRequest;
import com.trustfund.model.request.UpdateExpenditureActualsRequest; // Added import

import java.util.List;

public interface ExpenditureService {
    Expenditure createExpenditure(CreateExpenditureRequest request);
    List<Expenditure> getExpendituresByCampaign(Long campaignId);
    Expenditure getExpenditureById(Long id);
    Expenditure updateExpenditureStatus(Long id, String status);
    Expenditure updateExpenditureActuals(Long id, UpdateExpenditureActualsRequest request); // Added method
    List<ExpenditureItem> getExpenditureItems(Long expenditureId);
}
