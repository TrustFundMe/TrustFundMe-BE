package com.trustfund.service;

import com.trustfund.model.request.CreateExpenditureRequest;
import com.trustfund.model.request.CreateExpenditureItemRequest;
import com.trustfund.model.request.UpdateExpenditureActualsRequest;
import com.trustfund.model.response.ExpenditureCatologyResponse;
import com.trustfund.model.response.ExpenditureResponse;
import com.trustfund.model.response.ExpenditureTransactionResponse;
import com.trustfund.model.response.ExpenditureItemResponse;
import jakarta.validation.Valid;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface ExpenditureService {
        ExpenditureResponse createExpenditure(@Valid CreateExpenditureRequest request);

        ExpenditureResponse updateExpenditure(Long id, @Valid CreateExpenditureRequest request);

        List<ExpenditureResponse> getExpendituresByCampaign(Long campaignId);

        List<ExpenditureItemResponse> getExpenditureItemsByCampaign(Long campaignId);

        List<ExpenditureItemResponse> getApprovedItemsByCampaign(Long campaignId);

        ExpenditureResponse getExpenditureById(Long id);

        ExpenditureResponse updateExpenditureStatus(Long id,
                        com.trustfund.model.request.ReviewExpenditureRequest request);

        ExpenditureResponse requestWithdrawal(Long id, LocalDateTime evidenceDueAt);

        ExpenditureResponse requestWithdrawal(Long id, LocalDateTime evidenceDueAt, BigDecimal withdrawAmount);

        List<ExpenditureItemResponse> getExpenditureItems(Long expenditureId);

        ExpenditureResponse updateExpenditureActuals(Long id, UpdateExpenditureActualsRequest request);

        ExpenditureResponse updateDisbursementProof(Long id,
                        com.trustfund.model.request.UpdateDisbursementProofRequest request);

        ExpenditureResponse addItemsToExpenditure(Long expenditureId, List<CreateExpenditureItemRequest> itemsRequest);

        ExpenditureItemResponse getExpenditureItemById(Long id);

        void updateExpenditureItemQuantity(Long id, Integer amountToDeduct);

        void deleteExpenditureItem(Long itemId);

        ExpenditureResponse updateEvidenceStatus(Long id, String status);

        ExpenditureTransactionResponse createRefund(Long expenditureId, BigDecimal amount, Long fromUserId,
                        String proofUrl,
                        String fromBankCode, String fromAccountNumber, String fromAccountHolderName,
                        String toBankCode, String toAccountNumber, String toAccountHolderName);

        java.io.ByteArrayInputStream exportItemsToExcel(Long campaignId);

        java.io.ByteArrayInputStream exportItemsToExcelTemplate();

        List<ExpenditureTransactionResponse> getAllTransactions();

        List<ExpenditureResponse> getExpendituresByStatus(String status);

        BigDecimal getTotalDisbursedByFundOwner(Long fundOwnerId);

        List<ExpenditureResponse> getExpendituresByFundOwner(Long fundOwnerId);

        List<ExpenditureCatologyResponse> getExpenditureCategories(Long expenditureId);

        com.trustfund.model.response.AuditResultResponse auditExpenditure(Long id);

        com.trustfund.model.response.AuditResultResponse auditExpenditureItem(Long itemId);

        void createEvidenceRequirement(com.trustfund.model.request.CreateEvidenceRequirementRequest request);

        void submitEvidence(Long evidenceId, String proofUrl);

        List<com.trustfund.model.response.ExpenditureEvidenceResponse> getPendingEvidenceByUser(Long userId);

        void assignEvidenceToPhase(Long evidenceId, Long expenditureId);

        ExpenditureCatologyResponse createCategory(Long expenditureId, String name, String description);

        void deleteCategory(Long categoryId);

        com.trustfund.model.response.ExpenditureEvidenceResponse getEvidenceById(Long id);

        List<com.trustfund.model.response.ExpenditureEvidenceResponse> getOrphanEvidencesByCampaign(Long campaignId);
}
