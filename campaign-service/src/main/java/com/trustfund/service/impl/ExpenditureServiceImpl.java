package com.trustfund.service.impl;

import com.trustfund.model.Expenditure;
import com.trustfund.model.ExpenditureItem;
import com.trustfund.model.response.CampaignResponse;
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
    private final CampaignService campaignService;
    private final IdentityServiceClient identityServiceClient;

    @Override
    @Transactional
    public Expenditure createExpenditure(CreateExpenditureRequest request) {
        CampaignResponse campaign = campaignService.getById(request.getCampaignId());

        if ("DISABLED".equalsIgnoreCase(campaign.getStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chiến dịch đã bị vô hiệu hóa, không thể yêu cầu chi tiêu.");
        }

        // === VALIDATION: Kiểm tra điều kiện tạo expenditure mới ===
        List<Expenditure> existingExps = expenditureRepository.findByCampaignId(request.getCampaignId());

        if (!existingExps.isEmpty()) {
            if ("AUTHORIZED".equalsIgnoreCase(campaign.getType())) {
                // Quỹ ủy quyền: chỉ được tạo mới khi expenditure hiện tại đã DISBURSED+bằng chứng HOẶC REJECTED
                boolean canCreate = existingExps.stream().anyMatch(e ->
                    ("DISBURSED".equalsIgnoreCase(e.getStatus()) && e.getDisbursementProofUrl() != null && !e.getDisbursementProofUrl().isBlank())
                    || "REJECTED".equalsIgnoreCase(e.getStatus())
                );
                // Nếu không có expenditure nào đủ điều kiện, kiểm tra tất cả còn đang active
                boolean hasActiveExp = existingExps.stream().anyMatch(e ->
                    !"DISBURSED".equalsIgnoreCase(e.getStatus()) && !"REJECTED".equalsIgnoreCase(e.getStatus())
                );
                if (hasActiveExp) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Quỹ ủy quyền chỉ được tạo khoản chi mới khi khoản chi hiện tại đã được giải ngân và có bằng chứng, hoặc bị từ chối.");
                }
            } else if ("ITEMIZED".equalsIgnoreCase(campaign.getType())) {
                // Quỹ vật phẩm: chỉ được tạo mới khi expenditure hiện tại đã DISBURSED+bằng chứng
                boolean hasActiveExp = existingExps.stream().anyMatch(e ->
                    !"DISBURSED".equalsIgnoreCase(e.getStatus())
                );
                boolean lastHasProof = existingExps.stream()
                    .filter(e -> "DISBURSED".equalsIgnoreCase(e.getStatus()))
                    .allMatch(e -> e.getDisbursementProofUrl() != null && !e.getDisbursementProofUrl().isBlank());
                if (hasActiveExp) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Quỹ vật phẩm chỉ được tạo khoản chi mới khi khoản chi hiện tại đã được giải ngân và có bằng chứng.");
                }
                if (!lastHasProof) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Vui lòng nộp bằng chứng cho khoản chi đã giải ngân trước khi tạo khoản chi mới.");
                }
            }
        }
        // === END VALIDATION ===

        // Ràng buộc: Đối với chiến dịch AUTHORIZED (Quỹ Ủy quyền), evidenceDueAt là bắt
        // buộc
        if ("AUTHORIZED".equalsIgnoreCase(campaign.getType()) && request.getEvidenceDueAt() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Hạn nộp minh chứng là bắt buộc đối với loại chiến dịch này");
        }

        // Logic: AUTHORIZED -> PENDING_REVIEW, ITEMIZED -> APPROVED
        String initialStatus = "APPROVED";
        if ("AUTHORIZED".equalsIgnoreCase(campaign.getType())) {
            initialStatus = "PENDING_REVIEW";
        }


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

        // Tự động lấy và lưu thông tin ngân hàng của chủ quỹ tại thời điểm tạo expenditure
        log.info("Fetching bank details for campaign owner: {}", campaign.getFundOwnerId());
        try {
            BankAccountResponse bankRes = identityServiceClient.getPrimaryBankAccount(campaign.getFundOwnerId());
            if (bankRes != null) {
                expenditure.setBankCode(bankRes.getBankCode());
                expenditure.setAccountNumber(bankRes.getAccountNumber());
                expenditure.setAccountHolderName(bankRes.getAccountHolderName());
                log.info("Recorded bank details for expenditure of campaign {}: {}", campaign.getId(), bankRes.getAccountNumber());
            } else {
                log.warn("No bank details found for campaign owner: {}", campaign.getFundOwnerId());
            }
        } catch (Exception e) {
            log.error("Failed to fetch bank details for expenditure of campaign {}: {}", campaign.getId(), e.getMessage());
        }

        final Expenditure savedExpenditure = expenditureRepository.save(expenditure);

        if (request.getItems() != null && !request.getItems().isEmpty()) {
            List<ExpenditureItem> items = request.getItems().stream()
                    .map(itemReq -> ExpenditureItem.builder()
                            .expenditure(savedExpenditure)
                            .category(itemReq.getCategory())
                            .quantity(itemReq.getQuantity())
                            .actualQuantity(0) // Default to 0 initially
                            .price(BigDecimal.ZERO) // Default Actual Price to 0 initially
                            .expectedPrice(itemReq.getExpectedPrice())
                            .note(itemReq.getNote())
                            .build())
                    .collect(Collectors.toList());
            expenditureItemRepository.saveAll(items);
        }

        return savedExpenditure;
    }

    @Override
    public List<Expenditure> getExpendituresByCampaign(Long campaignId) {
        return expenditureRepository.findByCampaignId(campaignId);
    }

    @Override
    public Expenditure getExpenditureById(Long id) {
        return expenditureRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Expenditure not found: " + id));
    }

    @Override
    @Transactional
    public Expenditure updateExpenditureStatus(Long id, com.trustfund.model.request.ReviewExpenditureRequest request) {
        Expenditure expenditure = getExpenditureById(id);
        String status = request.getStatus();
        expenditure.setStatus(status);
        expenditure.setStaffReviewId(request.getStaffId());
        
        if ("REJECTED".equalsIgnoreCase(status)) {
            expenditure.setRejectReason(request.getReasonReject());
        }

        // Logic: Nếu là chi tiêu AUTHORIZED và được approved -> Tự động yêu cầu rút
        // tiền
        if ("APPROVED".equalsIgnoreCase(status)) {
            CampaignResponse campaign = campaignService.getById(expenditure.getCampaignId());
            if ("AUTHORIZED".equalsIgnoreCase(campaign.getType())) {
                expenditure.setIsWithdrawalRequested(true);
                expenditure.setStatus("WITHDRAWAL_REQUESTED"); // Move to withdrawal request state for admin
                
                // Tự động lấy và lưu thông tin ngân hàng cho quỹ ủy quyền
                try {
                    BankAccountResponse bankRes = identityServiceClient.getPrimaryBankAccount(campaign.getFundOwnerId());
                    if (bankRes != null) {
                        expenditure.setBankCode(bankRes.getBankCode());
                        expenditure.setAccountNumber(bankRes.getAccountNumber());
                        expenditure.setAccountHolderName(bankRes.getAccountHolderName());
                        log.info("Recorded bank details for authorized expenditure {} upgrade: {}", id, bankRes.getAccountNumber());
                    }
                } catch (Exception e) {
                    log.error("Failed to fetch bank details for authorized expenditure {}: {}", id, e.getMessage());
                }
            }
        }

        if ("DISBURSED".equalsIgnoreCase(status)) {
            expenditure.setDisbursedAt(java.time.LocalDateTime.now());
        }

        return expenditureRepository.save(expenditure);
    }

    @Override
    @Transactional
    public Expenditure requestWithdrawal(Long id, java.time.LocalDateTime evidenceDueAt) {
        Expenditure expenditure = getExpenditureById(id);

        if (expenditure.getIsWithdrawalRequested()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Yêu cầu rút tiền đã được thực hiện trước đó");
        }

        CampaignResponse campaign = campaignService.getById(expenditure.getCampaignId());

        expenditure.setIsWithdrawalRequested(true);
        if (evidenceDueAt != null) {
            expenditure.setEvidenceDueAt(evidenceDueAt);
        }

        
        expenditure.setStatus("WITHDRAWAL_REQUESTED");

        // Khi yêu cầu rút tiền, cập nhật/ghi đè thông tin ngân hàng mới nhất của chủ quỹ
        log.info("Refreshing bank details for withdrawal request: {}", id);
        try {
            BankAccountResponse bankRes = identityServiceClient.getPrimaryBankAccount(campaign.getFundOwnerId());
            if (bankRes != null) {
                expenditure.setBankCode(bankRes.getBankCode());
                expenditure.setAccountNumber(bankRes.getAccountNumber());
                expenditure.setAccountHolderName(bankRes.getAccountHolderName());
                log.info("Updated bank details for withdrawal request of expenditure {}: {}", id, bankRes.getAccountNumber());
            } else {
                log.warn("No bank details found for withdrawal request: {}", id);
            }
        } catch (Exception e) {
            log.error("Failed to update bank details for withdrawal request of expenditure {}: {}", id, e.getMessage());
        }

        return expenditureRepository.save(expenditure);
    }

    @Override
    public List<ExpenditureItem> getExpenditureItems(Long expenditureId) {
        return expenditureItemRepository.findByExpenditureId(expenditureId);
    }

    @Override
    @Transactional
    public Expenditure updateExpenditureActuals(Long id, UpdateExpenditureActualsRequest request) {
        Expenditure expenditure = getExpenditureById(id);

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
        List<ExpenditureItem> allItems = expenditureItemRepository.findByExpenditureId(id);

        BigDecimal totalAmount = allItems.stream()
                .map(item -> item.getPrice()
                        .multiply(BigDecimal.valueOf(item.getActualQuantity() != null ? item.getActualQuantity() : 0)))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalExpectedAmount = expenditure.getTotalExpectedAmount(); // Keep original expected amount
        BigDecimal variance = totalExpectedAmount.subtract(totalAmount);

        expenditure.setTotalAmount(totalAmount);
        expenditure.setVariance(variance);

        return expenditureRepository.save(expenditure);
    }

    @Override
    @Transactional
    public Expenditure updateDisbursementProof(Long id, com.trustfund.model.request.UpdateDisbursementProofRequest request) {
        Expenditure expenditure = getExpenditureById(id);
        expenditure.setDisbursementProofUrl(request.getProofUrl());
        return expenditureRepository.save(expenditure);
    }

    @Override
    @Transactional
    public Expenditure addItemsToExpenditure(Long expenditureId, List<CreateExpenditureItemRequest> itemsRequest) {
        Expenditure expenditure = getExpenditureById(expenditureId);

        List<ExpenditureItem> items = itemsRequest.stream()
                .map(itemReq -> ExpenditureItem.builder()
                        .expenditure(expenditure)
                        .category(itemReq.getCategory())
                        .quantity(itemReq.getQuantity())
                        .actualQuantity(0)
                        .price(BigDecimal.ZERO)
                        .expectedPrice(itemReq.getExpectedPrice())
                        .note(itemReq.getNote())
                        .build())
                .collect(Collectors.toList());

        expenditureItemRepository.saveAll(items);
        return recalculateExpenditureTotals(expenditureId);
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
        Expenditure expenditure = getExpenditureById(expenditureId);
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

        BigDecimal variance = totalExpectedAmount.subtract(totalAmount);

        expenditure.setTotalExpectedAmount(totalExpectedAmount);
        expenditure.setTotalAmount(totalAmount);
        expenditure.setVariance(variance);

        return expenditureRepository.save(expenditure);
    }

    @Override
    @Transactional
    public Expenditure updateEvidenceStatus(Long id, String status) {
        Expenditure expenditure = getExpenditureById(id);
        expenditure.setEvidenceStatus(status);
        return expenditureRepository.save(expenditure);
    }
}
