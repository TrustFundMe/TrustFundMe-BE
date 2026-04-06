package com.trustfund.service;

import com.trustfund.model.InternalTransaction;
import com.trustfund.model.enums.InternalTransactionStatus;
import com.trustfund.model.enums.InternalTransactionType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface InternalTransactionService {
    InternalTransaction createTransaction(Long fromCampaignId, Long toCampaignId, BigDecimal amount,
            InternalTransactionType type, String reason, Long createdByStaffId, Long evidenceImageId,
            InternalTransactionStatus status);

    List<InternalTransaction> getAll();

    InternalTransaction getById(Long id);

    InternalTransaction updateTransactionStatus(Long id, InternalTransactionStatus status);

    void delete(Long id);

    Map<String, BigDecimal> getGeneralFundStats();

    List<InternalTransaction> getGeneralFundHistory();

    InternalTransaction updateEvidence(Long id, Long evidenceImageId);
}
