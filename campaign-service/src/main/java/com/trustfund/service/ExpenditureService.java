package com.trustfund.service;

import com.trustfund.model.Expenditure;
import com.trustfund.model.ExpenditureItem;
import com.trustfund.model.request.CreateExpenditureItemRequest;
import com.trustfund.model.request.CreateExpenditureRequest;
import com.trustfund.model.request.UpdateExpenditureActualsRequest;

import java.util.List;

public interface ExpenditureService {
    Expenditure createExpenditure(CreateExpenditureRequest request);

    List<Expenditure> getExpendituresByCampaign(Long campaignId);

    Expenditure getExpenditureById(Long id);

    Expenditure updateExpenditureStatus(Long id, com.trustfund.model.request.ReviewExpenditureRequest request);

    Expenditure updateExpenditureActuals(Long id, UpdateExpenditureActualsRequest request); // Added method

    Expenditure updateDisbursementProof(Long id, com.trustfund.model.request.UpdateDisbursementProofRequest request); // Updated method

    Expenditure requestWithdrawal(Long id, java.time.LocalDateTime evidenceDueAt); // Updated method

    List<ExpenditureItem> getExpenditureItems(Long expenditureId);

    Expenditure addItemsToExpenditure(Long expenditureId, List<CreateExpenditureItemRequest> items);

    void deleteExpenditureItem(Long itemId);

    Expenditure updateEvidenceStatus(Long id, String status);
}
