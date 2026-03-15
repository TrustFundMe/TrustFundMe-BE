package com.trustfund.service.impl;

import com.trustfund.model.Expenditure;
import com.trustfund.model.ExpenditureItem;
import com.trustfund.model.ExpenditureTransaction;
import com.trustfund.repository.ExpenditureTransactionRepository;
import com.trustfund.model.response.CampaignResponse;
import com.trustfund.model.response.ExpenditureResponse;
import com.trustfund.model.response.ExpenditureTransactionResponse;
import com.trustfund.model.response.ExpenditureItemResponse;
import com.trustfund.model.request.CreateExpenditureRequest;
import com.trustfund.model.request.CreateExpenditureItemRequest;
import com.trustfund.model.request.UpdateExpenditureActualsRequest;
import com.trustfund.service.CampaignService;
import com.trustfund.repository.ExpenditureItemRepository;
import com.trustfund.repository.ExpenditureRepository;
import com.trustfund.service.ExpenditureService;
import com.trustfund.client.IdentityServiceClient;
import com.trustfund.model.response.BankAccountResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExpenditureServiceImpl implements ExpenditureService {

    private final ExpenditureRepository expenditureRepository;
    private final ExpenditureItemRepository expenditureItemRepository;
    private final ExpenditureTransactionRepository transactionRepository;
    private final CampaignService campaignService;
    private final IdentityServiceClient identityServiceClient;

    @Override
    @Transactional
    public ExpenditureResponse createExpenditure(CreateExpenditureRequest request) {
        CampaignResponse campaign = campaignService.getById(request.getCampaignId());

        if ("DISABLED".equalsIgnoreCase(campaign.getStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chiến dịch đã bị vô hiệu hóa, không thể yêu cầu chi tiêu.");
        }

        // === VALIDATION: Kiểm tra điều kiện tạo expenditure mới ===
        List<ExpenditureResponse> existingExps = getExpendituresByCampaign(request.getCampaignId());

        if (!existingExps.isEmpty()) {
            if ("AUTHORIZED".equalsIgnoreCase(campaign.getType())) {
                boolean hasActiveExp = existingExps.stream().anyMatch(e -> !"DISBURSED".equalsIgnoreCase(e.getStatus())
                        && !"REJECTED".equalsIgnoreCase(e.getStatus()));
                
                if (hasActiveExp) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Quỹ ủy quyền chỉ được tạo khoản chi mới khi khoản chi hiện tại đã được giải ngân hoặc bị từ chối.");
                }

                // Kiểm tra bằng chứng giải ngân cho các khoản DISBURSED
                boolean allDisbursedHaveProof = existingExps.stream()
                    .filter(e -> "DISBURSED".equalsIgnoreCase(e.getStatus()))
                    .allMatch(e -> e.getTransactions().stream()
                        .filter(t -> "PAYOUT".equalsIgnoreCase(t.getType()))
                        .anyMatch(t -> t.getProofUrl() != null && !t.getProofUrl().isBlank()));

                if (!allDisbursedHaveProof) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Vui lòng nộp bằng chứng giải ngân cho khoản chi trước đó.");
                }
            } else if ("ITEMIZED".equalsIgnoreCase(campaign.getType())) {
                boolean hasActiveExp = existingExps.stream()
                        .anyMatch(e -> !"DISBURSED".equalsIgnoreCase(e.getStatus()));
                
                if (hasActiveExp) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Quỹ vật phẩm chỉ được tạo khoản chi mới khi khoản chi hiện tại đã được giải ngân.");
                }

                boolean allDisbursedHaveProof = existingExps.stream()
                    .filter(e -> "DISBURSED".equalsIgnoreCase(e.getStatus()))
                    .allMatch(e -> e.getTransactions().stream()
                        .filter(t -> "PAYOUT".equalsIgnoreCase(t.getType()))
                        .anyMatch(t -> t.getProofUrl() != null && !t.getProofUrl().isBlank()));

                if (!allDisbursedHaveProof) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Vui lòng nộp bằng chứng cho khoản chi đã giải ngân trước khi tạo khoản chi mới.");
                }
            }
        }
        // === END VALIDATION ===

        if ("AUTHORIZED".equalsIgnoreCase(campaign.getType()) && request.getEvidenceDueAt() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Hạn nộp minh chứng là bắt buộc đối với loại chiến dịch này");
        }

        String initialStatus = "PENDING_REVIEW";

        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal totalExpectedAmount = BigDecimal.ZERO;

        if (request.getItems() != null) {
            totalAmount = request.getItems().stream()
                    .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            totalExpectedAmount = request.getItems().stream()
                    .map(item -> item.getExpectedPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        BigDecimal variance = totalExpectedAmount.subtract(totalAmount);

        Expenditure expenditure = Expenditure.builder()
                .campaignId(request.getCampaignId())
                .evidenceDueAt(request.getEvidenceDueAt())
                .evidenceStatus(request.getEvidenceStatus() != null ? request.getEvidenceStatus() : "PENDING")
                .totalAmount(totalAmount)
                .totalExpectedAmount(totalExpectedAmount)
                .variance(variance)
                .plan(request.getPlan())
                .status(initialStatus)
                .build();

        final Expenditure savedExpenditure = expenditureRepository.save(expenditure);

        if (request.getItems() != null && !request.getItems().isEmpty()) {
            List<ExpenditureItem> items = request.getItems().stream()
                    .map(itemReq -> ExpenditureItem.builder()
                            .expenditure(savedExpenditure)
                            .category(itemReq.getCategory())
                            .quantity(itemReq.getQuantity())
                            .actualQuantity(0)
                            .quantityLeft(itemReq.getQuantity())
                            .price(BigDecimal.ZERO)
                            .expectedPrice(itemReq.getExpectedPrice())
                            .note(itemReq.getNote())
                            .build())
                    .collect(Collectors.toList());
            expenditureItemRepository.saveAll(items);
        }

        return mapToResponse(savedExpenditure);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExpenditureResponse> getExpendituresByCampaign(Long campaignId) {
        return expenditureRepository.findByCampaignId(campaignId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ExpenditureItemResponse> getExpenditureItemsByCampaign(Long campaignId) {
        return expenditureItemRepository.findByExpenditureCampaignId(campaignId).stream()
                .map(this::mapToItemResponse)
                .collect(Collectors.toList());
    }

    private ExpenditureItemResponse mapToItemResponse(ExpenditureItem item) {
        return ExpenditureItemResponse.builder()
                .id(item.getId())
                .expenditureId(item.getExpenditure().getId())
                .category(item.getCategory())
                .quantity(item.getQuantity())
                .actualQuantity(item.getActualQuantity())
                .quantityLeft(item.getQuantityLeft())
                .price(item.getPrice())
                .expectedPrice(item.getExpectedPrice())
                .note(item.getNote())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ExpenditureResponse getExpenditureById(Long id) {
        Expenditure expenditure = expenditureRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Expenditure not found: " + id));
        return mapToResponse(expenditure);
    }

    private ExpenditureResponse mapToResponse(Expenditure expenditure) {
        return ExpenditureResponse.builder()
                .id(expenditure.getId())
                .campaignId(expenditure.getCampaignId())
                .evidenceDueAt(expenditure.getEvidenceDueAt())
                .evidenceStatus(expenditure.getEvidenceStatus())
                .totalAmount(expenditure.getTotalAmount())
                .totalExpectedAmount(expenditure.getTotalExpectedAmount())
                .variance(expenditure.getVariance())
                .isWithdrawalRequested(expenditure.getIsWithdrawalRequested())
                .plan(expenditure.getPlan())
                .status(expenditure.getStatus())
                .staffReviewId(expenditure.getStaffReviewId())
                .rejectReason(expenditure.getRejectReason())
                .createdAt(expenditure.getCreatedAt())
                .updatedAt(expenditure.getUpdatedAt())
                .transactions(expenditure.getTransactions() != null ? expenditure.getTransactions().stream()
                        .map(this::mapToTransactionResponse)
                        .collect(Collectors.toList()) : java.util.Collections.emptyList())
                .disbursementProofUrl(expenditure.getTransactions() != null ? expenditure.getTransactions().stream()
                        .filter(t -> "PAYOUT".equalsIgnoreCase(t.getType()) && t.getProofUrl() != null && t.getCreatedAt() != null)
                        .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                        .map(ExpenditureTransaction::getProofUrl)
                        .findFirst()
                        .orElse(null) : null)
                .build();
    }

    private ExpenditureTransactionResponse mapToTransactionResponse(ExpenditureTransaction t) {
        return ExpenditureTransactionResponse.builder()
                .id(t.getId())
                .expenditureId(t.getExpenditure().getId())
                .fromUserId(t.getFromUserId())
                .toUserId(t.getToUserId())
                .amount(t.getAmount())
                .fromBankCode(t.getFromBankCode())
                .fromAccountNumber(t.getFromAccountNumber())
                .fromAccountHolderName(t.getFromAccountHolderName())
                .toBankCode(t.getToBankCode())
                .toAccountNumber(t.getToAccountNumber())
                .toAccountHolderName(t.getToAccountHolderName())
                .type(t.getType())
                .status(t.getStatus())
                .proofUrl(t.getProofUrl())
                .createdAt(t.getCreatedAt())
                .build();
    }

    @Override
    @Transactional
    public ExpenditureResponse updateExpenditureStatus(Long id, com.trustfund.model.request.ReviewExpenditureRequest request) {
        Expenditure expenditure = expenditureRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Expenditure not found: " + id));
        String status = request.getStatus();
        expenditure.setStaffReviewId(request.getStaffId());

        if ("REJECTED".equalsIgnoreCase(status)) {
            expenditure.setStatus("REJECTED");
            expenditure.setRejectReason(request.getReasonReject());
        } else if ("APPROVED".equalsIgnoreCase(status)) {
            CampaignResponse campaign = campaignService.getById(expenditure.getCampaignId());
            if ("AUTHORIZED".equalsIgnoreCase(campaign.getType())) {
                expenditure.setStatus("WITHDRAWAL_REQUESTED");
                expenditure.setIsWithdrawalRequested(true);
            } else {
                expenditure.setStatus("APPROVED");
                expenditure.setIsWithdrawalRequested(false);
            }
        } else if ("DISBURSED".equalsIgnoreCase(status)) {
            expenditure.setStatus("DISBURSED");
        } else {
            expenditure.setStatus(status);
        }

        if ("APPROVED".equalsIgnoreCase(status)) {
            CampaignResponse campaign = campaignService.getById(expenditure.getCampaignId());
            
            // Tự động tạo bản ghi giao dịch PENDING cho AUTHORIZED (đã duyệt và tự yêu cầu rút tiền)
            if ("AUTHORIZED".equalsIgnoreCase(campaign.getType())) {
                ExpenditureTransaction transaction = ExpenditureTransaction.builder()
                        .expenditure(expenditure)
                        .amount(expenditure.getTotalExpectedAmount())
                        .fromUserId(1L)
                        .toUserId(campaign.getFundOwnerId())
                        .type("PAYOUT")
                        .status("PENDING")
                        .createdAt(java.time.LocalDateTime.now())
                        .build();

                try {
                    BankAccountResponse fromBank = identityServiceClient.getPrimaryBankAccount(1L);
                    if (fromBank != null) {
                        transaction.setFromBankCode(fromBank.getBankCode());
                        transaction.setFromAccountNumber(fromBank.getAccountNumber());
                        transaction.setFromAccountHolderName(fromBank.getAccountHolderName());
                    }

                    BankAccountResponse toBank = identityServiceClient.getPrimaryBankAccount(campaign.getFundOwnerId());
                    if (toBank != null) {
                        transaction.setToBankCode(toBank.getBankCode());
                        transaction.setToAccountNumber(toBank.getAccountNumber());
                        transaction.setToAccountHolderName(toBank.getAccountHolderName());
                    }
                } catch (Exception e) {
                    log.error("⚠️ Warning: Could not fetch bank details for AUTHORIZED transaction: {}", e.getMessage());
                }
                transactionRepository.save(transaction);
                log.info("➔ Created PENDING transaction for AUTHORIZED expenditure {} after Staff approval", id);

            } else if ("ITEMIZED".equalsIgnoreCase(campaign.getType())) {
                log.info("➔ Expenditure {} (ITEMIZED) approved but requires manual withdrawal request", id);
            }
        }

        // Logic Giải ngân (DISBURSED)
        if ("DISBURSED".equalsIgnoreCase(status)) {
            // Tìm giao dịch PENDING PAYOUT để cập nhật thành COMPLETED
            ExpenditureTransaction transaction = transactionRepository.findByExpenditureIdAndTypeAndStatus(id, "PAYOUT", "PENDING")
                    .stream()
                    .findFirst()
                    .orElse(null);

            if (transaction == null) {
                // Nếu chưa có (trường hợp cũ hoặc lỗi), tạo mới
                transaction = ExpenditureTransaction.builder()
                        .expenditure(expenditure)
                        .type("PAYOUT")
                        .createdAt(java.time.LocalDateTime.now())
                        .build();
            }

            CampaignResponse campaign = campaignService.getById(expenditure.getCampaignId());
            try {
                transaction.setStatus("COMPLETED");
                
                // Luôn coi Admin (ID 1) là người chuyển tiền trong các giao dịch PAYOUT
                transaction.setFromUserId(1L);
                transaction.setToUserId(campaign.getFundOwnerId());
                
                // Fetch bank info cho Admin (ID 1)
                BankAccountResponse fromBank = identityServiceClient.getPrimaryBankAccount(1L);
                if (fromBank != null) {
                    transaction.setFromBankCode(fromBank.getBankCode());
                    transaction.setFromAccountNumber(fromBank.getAccountNumber());
                    transaction.setFromAccountHolderName(fromBank.getAccountHolderName());
                }

                BankAccountResponse toBank = identityServiceClient.getPrimaryBankAccount(campaign.getFundOwnerId());
                if (toBank != null) {
                    transaction.setToBankCode(toBank.getBankCode());
                    transaction.setToAccountNumber(toBank.getAccountNumber());
                    transaction.setToAccountHolderName(toBank.getAccountHolderName());
                }

                if (request.getProofUrl() != null) {
                    transaction.setProofUrl(request.getProofUrl());
                }

                transactionRepository.save(transaction);
                log.info("✅ SUCCESS: Completed PAYOUT transaction for expenditure {}", id);

            } catch (Exception e) {
                log.error("❌ ERROR: Failed to complete transaction for disbursement: {}", e.getMessage());
            }
        }

        return mapToResponse(expenditureRepository.save(expenditure));
    }

    @Override
    @Transactional
    public ExpenditureResponse requestWithdrawal(Long id, java.time.LocalDateTime evidenceDueAt) {
        Expenditure expenditure = expenditureRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Expenditure not found: " + id));

        if (expenditure.getIsWithdrawalRequested()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Yêu cầu rút tiền đã được thực hiện trước đó");
        }

        expenditure.setIsWithdrawalRequested(true);
        if (evidenceDueAt != null) {
            expenditure.setEvidenceDueAt(evidenceDueAt);
        }

        expenditure.setStatus("WITHDRAWAL_REQUESTED");

        // Tạo bản ghi giao dịch PENDING
        ExpenditureTransaction transaction = ExpenditureTransaction.builder()
                .expenditure(expenditure)
                .amount(expenditure.getTotalExpectedAmount())
                .fromUserId(1L)
                .toUserId(campaignService.getById(expenditure.getCampaignId()).getFundOwnerId())
                .type("PAYOUT")
                .status("PENDING")
                .createdAt(java.time.LocalDateTime.now())
                .build();
        
        // Lấy thông tin ngân hàng
        try {
            BankAccountResponse fromBank = identityServiceClient.getPrimaryBankAccount(1L);
            if (fromBank != null) {
                transaction.setFromBankCode(fromBank.getBankCode());
                transaction.setFromAccountNumber(fromBank.getAccountNumber());
                transaction.setFromAccountHolderName(fromBank.getAccountHolderName());
            }

            BankAccountResponse toBank = identityServiceClient.getPrimaryBankAccount(transaction.getToUserId());
            if (toBank != null) {
                transaction.setToBankCode(toBank.getBankCode());
                transaction.setToAccountNumber(toBank.getAccountNumber());
                transaction.setToAccountHolderName(toBank.getAccountHolderName());
            }
        } catch (Exception e) {
            log.error("⚠️ Warning: Could not fetch bank details for manual withdrawal request: {}", e.getMessage());
        }

        transactionRepository.save(transaction);

        return mapToResponse(expenditureRepository.save(expenditure));
    }

    @Override
    public List<ExpenditureItemResponse> getExpenditureItems(Long expenditureId) {
        return expenditureItemRepository.findByExpenditureId(expenditureId).stream()
                .map(this::mapToItemResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ExpenditureResponse updateExpenditureActuals(Long id, UpdateExpenditureActualsRequest request) {
        Expenditure expenditure = expenditureRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Expenditure not found: " + id));

        for (UpdateExpenditureActualsRequest.UpdateItem updateItem : request.getItems()) {
            ExpenditureItem item = expenditureItemRepository.findById(updateItem.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Item not found: " + updateItem.getId()));

            if (!item.getExpenditure().getId().equals(id)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Item does not belong to this expenditure");
            }

            if (updateItem.getActualQuantity() != null) {
                item.setActualQuantity(updateItem.getActualQuantity());
            }
            if (updateItem.getPrice() != null) {
                item.setPrice(updateItem.getPrice());
            }
            expenditureItemRepository.save(item);
        }

        // Recalculate totals
        return mapToResponse(recalculateExpenditureTotals(id));
    }

    @Override
    @Transactional
    public ExpenditureResponse updateDisbursementProof(Long id,
            com.trustfund.model.request.UpdateDisbursementProofRequest request) {
        Expenditure expenditure = expenditureRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Expenditure not found: " + id));
        
        // Cập nhật bằng chứng vào giao dịch PAYOUT mới nhất
        List<ExpenditureTransaction> transactions = transactionRepository.findByExpenditureId(id);
        transactions.stream()
            .filter(t -> "PAYOUT".equalsIgnoreCase(t.getType()))
            .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
            .findFirst()
            .ifPresent(t -> {
                t.setProofUrl(request.getProofUrl());
                transactionRepository.save(t);
            });

        return mapToResponse(expenditure);
    }

    @Override
    @Transactional
    public ExpenditureResponse addItemsToExpenditure(Long expenditureId, List<CreateExpenditureItemRequest> itemsRequest) {
        Expenditure expenditure = expenditureRepository.findById(expenditureId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Expenditure not found: " + expenditureId));

        List<ExpenditureItem> items = itemsRequest.stream()
                .map(itemReq -> ExpenditureItem.builder()
                        .expenditure(expenditure)
                        .category(itemReq.getCategory())
                        .quantity(itemReq.getQuantity())
                        .actualQuantity(0)
                        .quantityLeft(itemReq.getQuantity())
                        .price(BigDecimal.ZERO)
                        .expectedPrice(itemReq.getExpectedPrice())
                        .note(itemReq.getNote())
                        .build())
                .collect(Collectors.toList());

        expenditureItemRepository.saveAll(items);
        return mapToResponse(recalculateExpenditureTotals(expenditureId));
    }

    @Override
    public ExpenditureItemResponse getExpenditureItemById(Long id) {
        return expenditureItemRepository.findById(id)
                .map(this::mapToItemResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found: " + id));
    }

    @Override
    @Transactional
    public void updateExpenditureItemQuantity(Long id, Integer amountToDeduct) {
        log.info("➔ System requested to deduct {} from quantityLeft for ExpenditureItem {}", amountToDeduct, id);

        ExpenditureItem item = expenditureItemRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found: " + id));

        int currentLeft = item.getQuantityLeft() != null ? item.getQuantityLeft() : item.getQuantity();
        item.setQuantityLeft(Math.max(0, currentLeft - amountToDeduct));
        expenditureItemRepository.save(item);
    }

    @Override
    @Transactional
    public void deleteExpenditureItem(Long itemId) {
        ExpenditureItem item = expenditureItemRepository.findById(itemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found: " + itemId));

        Long expenditureId = item.getExpenditure().getId();
        expenditureItemRepository.delete(item);
        recalculateExpenditureTotals(expenditureId);
    }

    private Expenditure recalculateExpenditureTotals(Long expenditureId) {
        Expenditure expenditure = expenditureRepository.findById(expenditureId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Expenditure not found: " + expenditureId));
        List<ExpenditureItem> items = expenditureItemRepository.findByExpenditureId(expenditureId);

        BigDecimal totalExpectedAmount = items.stream()
                .map(item -> item.getExpectedPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalAmount = items.stream()
                .map(item -> {
                    BigDecimal price = item.getPrice() != null ? item.getPrice() : BigDecimal.ZERO;
                    Integer qty = item.getActualQuantity() != null ? item.getActualQuantity() : 0;
                    return price.multiply(BigDecimal.valueOf(qty));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        expenditure.setTotalExpectedAmount(totalExpectedAmount);
        expenditure.setTotalAmount(totalAmount);
        expenditure.setVariance(totalExpectedAmount.subtract(totalAmount));

        return expenditureRepository.save(expenditure);
    }

    @Override
    @Transactional
    public ExpenditureResponse updateEvidenceStatus(Long id, String status) {
        Expenditure expenditure = expenditureRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Expenditure not found: " + id));
        expenditure.setEvidenceStatus(status);
        return mapToResponse(expenditureRepository.save(expenditure));
    }

    @Override
    @Transactional
    public ExpenditureTransactionResponse createRefund(Long expenditureId, BigDecimal amount, Long fromUserId, String proofUrl) {
        Expenditure expenditure = expenditureRepository.findById(expenditureId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Expenditure not found: " + expenditureId));
        
        ExpenditureTransaction transaction = ExpenditureTransaction.builder()
                .expenditure(expenditure)
                .fromUserId(fromUserId)
                .toUserId(1L) // Admin
                .amount(amount)
                .type("REFUND")
                .status("COMPLETED")
                .proofUrl(proofUrl)
                .createdAt(java.time.LocalDateTime.now())
                .build();

        try {
            BankAccountResponse fromBank = identityServiceClient.getPrimaryBankAccount(fromUserId);
            if (fromBank != null) {
                transaction.setFromBankCode(fromBank.getBankCode());
                transaction.setFromAccountNumber(fromBank.getAccountNumber());
                transaction.setFromAccountHolderName(fromBank.getAccountHolderName());
            }

            BankAccountResponse toBank = identityServiceClient.getPrimaryBankAccount(1L);
            if (toBank != null) {
                transaction.setToBankCode(toBank.getBankCode());
                transaction.setToAccountNumber(toBank.getAccountNumber());
                transaction.setToAccountHolderName(toBank.getAccountHolderName());
            }
        } catch (Exception e) {
            log.warn("⚠️ Warning: Could not fetch bank details for refund: {}", e.getMessage());
        }

        transaction = transactionRepository.save(transaction);

        return mapToTransactionResponse(transaction);
    }
}
