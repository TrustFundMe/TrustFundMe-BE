package com.trustfund.service.impl;

import com.trustfund.model.Campaign;
import com.trustfund.model.InternalTransaction;
import com.trustfund.model.enums.InternalTransactionStatus;
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

    private static final Long GENERAL_FUND_ID = 1L;

    @Override
    @Transactional
    public InternalTransaction createTransaction(Long fromCampaignId, Long toCampaignId, BigDecimal amount,
            InternalTransactionType type, String reason, Long createdByStaffId, Long evidenceImageId,
            InternalTransactionStatus status) {

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số tiền phải lớn hơn 0");
        }

        // Validate campaigns exist
        if (fromCampaignId != null) {
            campaignRepository.findById(fromCampaignId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Không tìm thấy nguồn: " + fromCampaignId));
        }
        if (toCampaignId != null) {
            campaignRepository.findById(toCampaignId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Không tìm thấy đích: " + toCampaignId));
        }

        InternalTransaction transaction = InternalTransaction.builder()
                .fromCampaignId(fromCampaignId)
                .toCampaignId(toCampaignId)
                .amount(amount)
                .type(type)
                .reason(reason)
                .createdByStaffId(createdByStaffId)
                .evidenceImageId(evidenceImageId)
                .status(status)
                .build();

        InternalTransaction saved = transactionRepository.save(transaction);

        // Nếu trạng thái là COMPLETED ngay lập tức (ví dụ Admin tạo), thì thực hiện
        // chuyển tiền
        if (status == InternalTransactionStatus.COMPLETED) {
            processFundTransfer(saved);
        }

        return saved;
    }

    @Override
    @Transactional
    public InternalTransaction updateTransactionStatus(Long id, InternalTransactionStatus newStatus) {
        InternalTransaction tx = transactionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Giao dịch không tồn tại"));

        if (tx.getStatus() == InternalTransactionStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Giao dịch đã hoàn tất, không thể thay đổi");
        }

        InternalTransactionStatus oldStatus = tx.getStatus();
        tx.setStatus(newStatus);

        InternalTransaction saved = transactionRepository.save(tx);

        // Chỉ trigger chuyển tiền khi chuyển sang COMPLETED
        if (newStatus == InternalTransactionStatus.COMPLETED && oldStatus != InternalTransactionStatus.COMPLETED) {
            processFundTransfer(saved);
        }

        return saved;
    }

    private void processFundTransfer(InternalTransaction tx) {
        if (tx.getFromCampaignId() != null) {
            Campaign from = campaignRepository.findById(tx.getFromCampaignId()).get();
            if (from.getBalance().compareTo(tx.getAmount()) < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số dư nguồn không đủ");
            }
            campaignRepository.updateBalance(tx.getFromCampaignId(), tx.getAmount().negate());
        }

        if (tx.getToCampaignId() != null) {
            campaignRepository.updateBalance(tx.getToCampaignId(), tx.getAmount());
        }
    }

    @Override
    public Map<String, BigDecimal> getGeneralFundStats() {
        Campaign generalFund = campaignRepository.findById(GENERAL_FUND_ID)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy Quỹ chung"));

        // Chỉ tính các giao dịch COMPLETED cho thống kê
        BigDecimal outcome = transactionRepository.sumAmountByFromCampaignIdAndTypeAndStatus(
                GENERAL_FUND_ID, InternalTransactionType.SUPPORT, InternalTransactionStatus.COMPLETED);

        BigDecimal income = transactionRepository.sumAmountByToCampaignIdAndTypeAndStatus(
                GENERAL_FUND_ID, InternalTransactionType.RECOVERY, InternalTransactionStatus.COMPLETED);

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
