package com.trustfund.service.impl;

import com.trustfund.model.Campaign;
import com.trustfund.model.InternalTransaction;
import com.trustfund.model.enums.InternalTransactionType;
import com.trustfund.repository.CampaignRepository;
import com.trustfund.repository.InternalTransactionRepository;
import com.trustfund.service.InternalTransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class InternalTransactionServiceImpl implements InternalTransactionService {

    private final InternalTransactionRepository transactionRepository;
    private final CampaignRepository campaignRepository;

    // Quỹ chung cố định ID = 1 (theo init-all-databases.sql)
    private static final Long GENERAL_FUND_ID = 1L;

    @Override
    @Transactional
    public InternalTransaction createTransaction(Long fromCampaignId, Long toCampaignId, BigDecimal amount,
            InternalTransactionType type, String reason) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số tiền phải lớn hơn 0");
        }

        // Kiểm tra campaign tồn tại
        if (fromCampaignId != null) {
            Campaign from = campaignRepository.findById(fromCampaignId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Không tìm thấy campaign nguồn: " + fromCampaignId));
            if (from.getBalance().compareTo(amount) < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số dư không đủ: " + from.getTitle());
            }
            campaignRepository.updateBalance(fromCampaignId, amount.negate());
        }

        if (toCampaignId != null) {
            campaignRepository.findById(toCampaignId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Không tìm thấy campaign đích: " + toCampaignId));
            campaignRepository.updateBalance(toCampaignId, amount);
        }

        InternalTransaction transaction = InternalTransaction.builder()
                .fromCampaignId(fromCampaignId)
                .toCampaignId(toCampaignId)
                .amount(amount)
                .type(type)
                .reason(reason)
                .build();

        return transactionRepository.save(transaction);
    }

    @Override
    public Map<String, BigDecimal> getGeneralFundStats() {
        Campaign generalFund = campaignRepository.findById(GENERAL_FUND_ID)
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy Quỹ chung (ID=1)"));

        // Outcome: Tổng tiền gửi đi từ Quỹ chung (SUPPORT)
        BigDecimal outcome = transactionRepository.sumAmountByFromCampaignIdAndType(GENERAL_FUND_ID,
                InternalTransactionType.SUPPORT);

        // Income: Tổng tiền thu về Quỹ chung (RECOVERY)
        BigDecimal income = transactionRepository.sumAmountByToCampaignIdAndType(GENERAL_FUND_ID,
                InternalTransactionType.RECOVERY);

        Map<String, BigDecimal> stats = new HashMap<>();
        stats.put("balance", generalFund.getBalance());
        stats.put("outcome", outcome != null ? outcome : BigDecimal.ZERO);
        stats.put("income", income != null ? income : BigDecimal.ZERO);

        return stats;
    }

    @Override
    public List<InternalTransaction> getGeneralFundHistory() {
        return transactionRepository.findByFromCampaignIdOrToCampaignIdOrderByCreatedAtDesc(GENERAL_FUND_ID,
                GENERAL_FUND_ID);
    }
}
