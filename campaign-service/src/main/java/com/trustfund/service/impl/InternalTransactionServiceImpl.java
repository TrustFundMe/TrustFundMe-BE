package com.trustfund.service.impl;

import com.trustfund.model.Campaign;
import com.trustfund.model.InternalTransaction;
import com.trustfund.model.enums.InternalTransactionStatus;
import com.trustfund.model.enums.InternalTransactionType;
import com.trustfund.repository.CampaignRepository;
import com.trustfund.repository.InternalTransactionRepository;
import com.trustfund.service.InternalTransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

                // Validate: nếu nguồn là Quỹ chung (type = GENERAL_FUND hoặc
                // ID=GENERAL_FUND_ID), kiểm tra số dư đủ
                if (fromCampaignId != null) {
                        Campaign sourceCandidate = campaignRepository.findById(fromCampaignId).orElse(null);
                        if (sourceCandidate != null) {
                                boolean isGeneralFund = com.trustfund.model.Campaign.TYPE_GENERAL_FUND
                                                .equals(sourceCandidate.getType())
                                                || GENERAL_FUND_ID.equals(fromCampaignId);

                                if (isGeneralFund && amount.compareTo(sourceCandidate.getBalance()) > 0) {
                                        java.text.NumberFormat format = java.text.NumberFormat
                                                        .getInstance(java.util.Locale.of("vi", "VN"));
                                        String currentBal = format.format(sourceCandidate.getBalance()) + " VNĐ";
                                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                                        "Số dư quỹ chung không đủ (Hiện tại: " + currentBal + ")");
                                }
                        }
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

                // Nếu trạng thái là COMPLETED hoặc APPROVED ngay lập tức, thì thực hiện chuyển
                // tiền
                if (status == InternalTransactionStatus.COMPLETED || status == InternalTransactionStatus.APPROVED) {
                        processFundTransfer(saved);
                }

                return saved;
        }

        @Override
        public List<InternalTransaction> getAll() {
                return transactionRepository.findAllByOrderByCreatedAtDesc();
        }

        @Override
        public InternalTransaction getById(Long id) {
                return transactionRepository.findById(id)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Giao dịch không tồn tại"));
        }

        @Override
        @Transactional
        public void delete(Long id) {
                InternalTransaction tx = transactionRepository.findById(id)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Giao dịch không tồn tại"));
                if (tx.getStatus() == InternalTransactionStatus.COMPLETED) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                        "Không thể xóa giao dịch đã hoàn tất");
                }
                transactionRepository.deleteById(id);
        }

        @Override
        @Transactional
        public InternalTransaction updateTransactionStatus(Long id, InternalTransactionStatus newStatus) {
                InternalTransaction tx = transactionRepository.findById(id)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Giao dịch không tồn tại"));

                if (tx.getStatus() == InternalTransactionStatus.COMPLETED) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                        "Giao dịch đã hoàn tất, không thể thay đổi");
                }

                InternalTransactionStatus oldStatus = tx.getStatus();
                tx.setStatus(newStatus);

                InternalTransaction saved = transactionRepository.save(tx);

                // Chỉ trigger chuyển tiền khi chuyển sang COMPLETED hoặc APPROVED
                boolean willTransfer = (newStatus == InternalTransactionStatus.COMPLETED
                                || newStatus == InternalTransactionStatus.APPROVED);
                boolean hasTransferred = (oldStatus == InternalTransactionStatus.COMPLETED
                                || oldStatus == InternalTransactionStatus.APPROVED);
                if (willTransfer && !hasTransferred) {
                        processFundTransfer(saved);
                }

                return saved;
        }

        private void processFundTransfer(InternalTransaction tx) {
                System.out.println("\n[SYSTEM DIAGNOSTIC] ======= TRACING BALANCE UPDATE =======");
                System.out.println("[DIAGNOSTIC] Transaction ID: " + tx.getId());
                System.out.println("[DIAGNOSTIC] Amount from TX Object: " + tx.getAmount().toPlainString() + " (Scale: "
                                + tx.getAmount().scale() + ")");

                try {
                        if (tx.getFromCampaignId() != null) {
                                Campaign from = campaignRepository.findById(tx.getFromCampaignId())
                                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                                "Không tìm thấy quỹ nguồn ID: "
                                                                                + tx.getFromCampaignId()));

                                System.out.println("[DIAGNOSTIC] Source Campaign (ID: " + from.getId()
                                                + ") Balance Before: "
                                                + from.getBalance().toPlainString());

                                if (from.getBalance().compareTo(tx.getAmount()) < 0) {
                                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                                        "Số dư của Quỹ không đủ để thực hiện giao dịch");
                                }

                                from.setBalance(from.getBalance().subtract(tx.getAmount()));
                                System.out.println("[DIAGNOSTIC] Source Campaign (ID: " + from.getId()
                                                + ") Calculated After: "
                                                + from.getBalance().toPlainString());
                                campaignRepository.save(from);
                        }

                        if (tx.getToCampaignId() != null) {
                                Campaign to = campaignRepository.findById(tx.getToCampaignId())
                                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                                "Không tìm thấy quỹ đích ID: " + tx.getToCampaignId()));

                                System.out.println(
                                                "[DIAGNOSTIC] Dest Campaign (ID: " + to.getId() + ") Balance Before: "
                                                                + to.getBalance().toPlainString());
                                to.setBalance(to.getBalance().add(tx.getAmount()));
                                System.out.println(
                                                "[DIAGNOSTIC] Dest Campaign (ID: " + to.getId() + ") Calculated After: "
                                                                + to.getBalance().toPlainString());
                                campaignRepository.save(to);
                        }
                        System.out.println("[SYSTEM DIAGNOSTIC] ======= TRACE COMPLETED =======\n");
                } catch (Exception e) {
                        System.err.println("[DIAGNOSTIC ERROR] " + e.getMessage());
                        throw e;
                }
        }

        @Override
        public Map<String, BigDecimal> getGeneralFundStats() {
                Campaign generalFund = campaignRepository.findById(GENERAL_FUND_ID)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Không tìm thấy Quỹ chung"));

                // Tính tổng chi tiêu / thu dựa trên các giao dịch hợp lệ
                BigDecimal outcome = transactionRepository.sumOutcomeFromGeneralFund();
                BigDecimal income = transactionRepository.sumIncomeToGeneralFund();
                Long count = transactionRepository.countByStatus(InternalTransactionStatus.APPROVED);

                Map<String, BigDecimal> stats = new HashMap<>();
                stats.put("balance", generalFund.getBalance());
                stats.put("outcome", outcome != null ? outcome : BigDecimal.ZERO);
                stats.put("income", income != null ? income : BigDecimal.ZERO);
                stats.put("transactionCount", count != null ? BigDecimal.valueOf(count) : BigDecimal.ZERO);

                return stats;
        }

        @Override
        public List<InternalTransaction> getGeneralFundHistory() {
                return transactionRepository.findByFromCampaignIdOrToCampaignIdOrderByCreatedAtDesc(GENERAL_FUND_ID,
                                GENERAL_FUND_ID);
        }

        @Override
        public Page<InternalTransaction> getGeneralFundHistoryPaginated(Pageable pageable) {
                return transactionRepository.findByFromCampaignIdOrToCampaignIdOrderByCreatedAtDesc(GENERAL_FUND_ID,
                                GENERAL_FUND_ID, pageable);
        }

        @Override
        @Transactional
        public InternalTransaction updateEvidence(Long id, Long evidenceImageId) {
                InternalTransaction tx = transactionRepository.findById(id)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Giao dịch không tồn tại"));

                tx.setEvidenceImageId(evidenceImageId);
                return transactionRepository.save(tx);
        }

        @Override
        public List<InternalTransaction> getApprovedReceivedByCampaign(Long campaignId) {
                return transactionRepository.findByToCampaignIdAndStatusOrderByCreatedAtDesc(
                                campaignId, InternalTransactionStatus.APPROVED);
        }
}
