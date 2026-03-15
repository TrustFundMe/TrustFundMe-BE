package com.trustfund.service;

import com.trustfund.model.response.ExpenditureItemResponse;
import com.trustfund.model.response.ExpenditureResponse;
import com.trustfund.model.response.ExpenditureTransactionResponse;
import com.trustfund.model.request.CreateExpenditureItemRequest;
import com.trustfund.model.request.CreateExpenditureRequest;
import com.trustfund.model.request.UpdateExpenditureActualsRequest;

import java.math.BigDecimal;
import java.util.List;

public interface ExpenditureService {
    ExpenditureResponse createExpenditure(CreateExpenditureRequest request);

    List<ExpenditureResponse> getExpendituresByCampaign(Long campaignId);

    List<ExpenditureItemResponse> getExpenditureItemsByCampaign(Long campaignId);

    ExpenditureResponse getExpenditureById(Long id);

    ExpenditureResponse updateExpenditureStatus(Long id, com.trustfund.model.request.ReviewExpenditureRequest request);

    ExpenditureResponse updateExpenditureActuals(Long id, UpdateExpenditureActualsRequest request);

    ExpenditureResponse updateDisbursementProof(Long id, com.trustfund.model.request.UpdateDisbursementProofRequest request);

    ExpenditureResponse requestWithdrawal(Long id, java.time.LocalDateTime evidenceDueAt);

    List<ExpenditureItemResponse> getExpenditureItems(Long expenditureId);

    ExpenditureItemResponse getExpenditureItemById(Long id);

    void updateExpenditureItemQuantity(Long id, Integer amount);

    ExpenditureResponse addItemsToExpenditure(Long expenditureId, List<CreateExpenditureItemRequest> items);

    void deleteExpenditureItem(Long itemId);

    ExpenditureResponse updateEvidenceStatus(Long id, String status);

    ExpenditureTransactionResponse createRefund(Long expenditureId, BigDecimal amount, Long fromUserId, String proofUrl);
}
