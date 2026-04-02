package com.trustfund.service;

import com.trustfund.model.InternalTransaction;
import com.trustfund.model.enums.InternalTransactionType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface InternalTransactionService {
    InternalTransaction createTransaction(Long fromCampaignId, Long toCampaignId, BigDecimal amount, InternalTransactionType type, String reason);
    Map<String, BigDecimal> getGeneralFundStats();
    List<InternalTransaction> getGeneralFundHistory();
}
