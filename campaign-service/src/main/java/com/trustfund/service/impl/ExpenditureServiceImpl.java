package com.trustfund.service.impl;

import com.trustfund.model.Expenditure;
import com.trustfund.model.ExpenditureCatology;
import com.trustfund.model.ExpenditureItem;
import com.trustfund.model.ExpenditureTransaction;
import com.trustfund.repository.ExpenditureCatologyRepository;
import com.trustfund.repository.ExpenditureTransactionRepository;
import com.trustfund.model.response.CampaignResponse;
import com.trustfund.model.response.ExpenditureCatologyResponse;
import com.trustfund.model.response.ExpenditureResponse;
import com.trustfund.model.response.ExpenditureTransactionResponse;
import com.trustfund.model.response.ExpenditureItemResponse;
import com.trustfund.model.request.CreateExpenditureCatologyRequest;
import com.trustfund.model.request.CreateExpenditureRequest;
import com.trustfund.model.request.CreateExpenditureItemRequest;
import com.trustfund.model.request.UpdateExpenditureActualsRequest;
import com.trustfund.service.CampaignService;
import com.trustfund.utils.ExpenditureExcelHelper;
import com.trustfund.repository.ExpenditureItemRepository;
import com.trustfund.repository.ExpenditureRepository;
import com.trustfund.service.ExpenditureService;
import com.trustfund.client.IdentityServiceClient;
import com.trustfund.client.NotificationServiceClient;
import com.trustfund.model.response.BankAccountResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.jdbc.core.JdbcTemplate;
import jakarta.annotation.PostConstruct;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExpenditureServiceImpl implements ExpenditureService {

    private final ExpenditureRepository expenditureRepository;
    private final ExpenditureItemRepository expenditureItemRepository;
    private final ExpenditureCatologyRepository catologyRepository;
    private final ExpenditureTransactionRepository transactionRepository;
    private final CampaignService campaignService;
    private final IdentityServiceClient identityServiceClient;
    private final NotificationServiceClient notificationServiceClient;
    private final com.trustfund.service.TrustScoreService trustScoreService;
    private final com.trustfund.client.PerplexityClient perplexityClient;
    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void cleanupOldDatabaseConstraints() {
        try {
            log.info("Checking and removing old unique constraints on expenditures table...");
            // Thử xóa UNIQUE constraint/index cũ ở field campaign_id nếu tồn tại (do code
            // cũ)
            jdbcTemplate.execute("ALTER TABLE expenditures DROP INDEX campaign_id");
            log.info("✅ Successfully dropped old 'campaign_id' unique index.");
        } catch (Exception e) {
            log.info("Old unique index for 'campaign_id' not found or already dropped. Safe to ignore.");
        }
    }

    private final com.trustfund.service.ApprovalTaskService approvalTaskService;

    @Override
    @Transactional
    public ExpenditureResponse createExpenditure(CreateExpenditureRequest request) {
        CampaignResponse campaign = campaignService.getById(request.getCampaignId());
        log.info("Creating expenditure for campaign ID: {}, plan: {}", request.getCampaignId(), request.getPlan());

        if ("DISABLED".equalsIgnoreCase(campaign.getStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Chiến dịch đã bị vô hiệu hóa, không thể yêu cầu chi tiêu.");
        }

        // ITEMIZED: cần staff duyệt trước (PENDING_REVIEW) — giống AUTHORIZED
        // AUTHORIZED: cần staff duyệt trước (PENDING_REVIEW)
        String initialStatus = "PENDING_REVIEW";

        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal totalExpectedAmount = BigDecimal.ZERO;

        // Tính tổng từ categories (nếu có) hoặc items trực tiếp (backward compat)
        List<CreateExpenditureItemRequest> allItemRequests = new java.util.ArrayList<>();
        if (request.getCategories() != null && !request.getCategories().isEmpty()) {
            for (CreateExpenditureCatologyRequest cat : request.getCategories()) {
                if (cat.getItems() != null) {
                    allItemRequests.addAll(cat.getItems());
                }
            }
        } else if (request.getItems() != null) {
            allItemRequests.addAll(request.getItems());
        }

        if (!allItemRequests.isEmpty()) {
            totalAmount = allItemRequests.stream()
                    .map(item -> {
                        BigDecimal price = item.getActualPrice() != null ? item.getActualPrice() : BigDecimal.ZERO;
                        int qty = item.getExpectedQuantity() != null ? item.getExpectedQuantity() : 0;
                        return price.multiply(BigDecimal.valueOf(qty));
                    })
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            totalExpectedAmount = allItemRequests.stream()
                    .map(item -> {
                        BigDecimal price = item.getExpectedPrice() != null ? item.getExpectedPrice() : BigDecimal.ZERO;
                        int qty = item.getExpectedQuantity() != null ? item.getExpectedQuantity() : 0;
                        return price.multiply(BigDecimal.valueOf(qty));
                    })
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        log.info("Calculated amounts - total: {}, expected: {}", totalAmount, totalExpectedAmount);

        BigDecimal variance = totalExpectedAmount.subtract(totalAmount);

        // Tính totalReceivedAmount: ITEMIZED = campaign.balance (đã bao gồm dư kỳ trước
        // + variance đã hoàn),
        // AUTHORIZED = totalExpectedAmount
        BigDecimal totalReceivedAmount;
        if ("ITEMIZED".equalsIgnoreCase(campaign.getType())) {
            totalReceivedAmount = (campaign.getBalance() != null) ? campaign.getBalance() : BigDecimal.ZERO;
        } else {
            totalReceivedAmount = totalExpectedAmount;
        }

        Expenditure expenditure = Expenditure.builder()
                .campaignId(request.getCampaignId())
                .evidenceDueAt(request.getEvidenceDueAt())
                .evidenceStatus(request.getEvidenceStatus() != null ? request.getEvidenceStatus() : "PENDING")
                .totalAmount(totalAmount)
                .totalExpectedAmount(totalExpectedAmount)
                .totalReceivedAmount(totalReceivedAmount)
                .variance(variance)
                .plan(request.getPlan())
                .status(initialStatus)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .build();

        final Expenditure savedExpenditure = expenditureRepository.save(expenditure);

        if (request.getCategories() != null && !request.getCategories().isEmpty()) {
            for (CreateExpenditureCatologyRequest catReq : request.getCategories()) {
                BigDecimal catExpectedAmount = BigDecimal.ZERO;
                if (catReq.getItems() != null) {
                    catExpectedAmount = catReq.getItems().stream()
                            .map(i -> {
                                BigDecimal price = i.getExpectedPrice() != null ? i.getExpectedPrice()
                                        : BigDecimal.ZERO;
                                int qty = i.getExpectedQuantity() != null ? i.getExpectedQuantity() : 0;
                                return price.multiply(BigDecimal.valueOf(qty));
                            })
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                }

                ExpenditureCatology catology = ExpenditureCatology.builder()
                        .expenditure(savedExpenditure)
                        .name(catReq.getName())
                        .description(catReq.getDescription())
                        .expectedAmount(catExpectedAmount)
                        .actualAmount(BigDecimal.ZERO)
                        .withdrawalCondition(catReq.getWithdrawalCondition())
                        .build();
                ExpenditureCatology savedCatology = catologyRepository.save(catology);

                if (catReq.getItems() != null) {
                    List<ExpenditureItem> items = catReq.getItems().stream()
                            .map(itemReq -> ExpenditureItem.builder()
                                    .expenditure(savedExpenditure)
                                    .catology(savedCatology)
                                    .name(itemReq.getName())
                                    .expectedPurchaseLink(itemReq.getExpectedPurchaseLink())
                                    .expectedQuantity(itemReq.getExpectedQuantity())
                                    .actualQuantity(0)
                                    .quantityLeft(itemReq.getExpectedQuantity())
                                    .actualPrice(BigDecimal.ZERO)
                                    .expectedPrice(itemReq.getExpectedPrice())
                                    .note(itemReq.getNote())
                                    .purchaseLocation(itemReq.getPurchaseLocation())
                                    .brand(itemReq.getBrand())
                                    .unit(itemReq.getUnit())
                                    .build())
                            .collect(Collectors.toList());
                    expenditureItemRepository.saveAll(items);
                }
            }
        } else if (request.getItems() != null && !request.getItems().isEmpty()) {
            List<ExpenditureItem> items = request.getItems().stream()
                    .map(itemReq -> ExpenditureItem.builder()
                            .expenditure(savedExpenditure)
                            .name(itemReq.getName())
                            .expectedPurchaseLink(itemReq.getExpectedPurchaseLink())
                            .expectedQuantity(itemReq.getExpectedQuantity())
                            .actualQuantity(0)
                            .quantityLeft(itemReq.getExpectedQuantity())
                            .actualPrice(BigDecimal.ZERO)
                            .expectedPrice(itemReq.getExpectedPrice())
                            .note(itemReq.getNote())
                            .purchaseLocation(itemReq.getPurchaseLocation())
                            .brand(itemReq.getBrand())
                            .unit(itemReq.getUnit())
                            .build())
                    .collect(Collectors.toList());
            expenditureItemRepository.saveAll(items);
        }

        // Create Approval Task for the new expenditure
        if ("PENDING_REVIEW".equalsIgnoreCase(savedExpenditure.getStatus())) {
            com.trustfund.model.ApprovalTask task = approvalTaskService.createAndAssignTask("EXPENDITURE",
                    savedExpenditure.getId());
            if (task != null && task.getStaffId() != null) {
                savedExpenditure.setStaffReviewId(task.getStaffId());
                expenditureRepository.save(savedExpenditure);
            }
        }

        return mapToResponse(savedExpenditure);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExpenditureResponse> getExpendituresByCampaign(Long campaignId) {
        List<Expenditure> expenditures = expenditureRepository.findByCampaignId(campaignId);
        log.info("getExpendituresByCampaign({}): found {} expenditures", campaignId, expenditures.size());
        return expenditures.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExpenditureItemResponse> getExpenditureItemsByCampaign(Long campaignId) {
        return expenditureItemRepository.findByExpenditureCampaignId(campaignId).stream()
                .map(this::mapToItemResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExpenditureItemResponse> getApprovedItemsByCampaign(Long campaignId) {
        List<Expenditure> expenditures = expenditureRepository.findByCampaignIdOrderByCreatedAtDesc(campaignId);
        // Tìm expenditure APPROVED mới nhất
        for (Expenditure exp : expenditures) {
            if ("APPROVED".equalsIgnoreCase(exp.getStatus())) {
                return expenditureItemRepository.findByExpenditureId(exp.getId()).stream()
                        .map(this::mapToItemResponse)
                        .collect(Collectors.toList());
            }
        }
        // Không có expenditure APPROVED nào
        return null;
    }

    private ExpenditureItemResponse mapToItemResponse(ExpenditureItem item) {
        return ExpenditureItemResponse.builder()
                .id(item.getId())
                .expenditureId(item.getExpenditure().getId())
                .name(item.getName())
                .expectedPurchaseLink(item.getExpectedPurchaseLink())
                .actualPurchaseLink(item.getActualPurchaseLink())
                .expectedQuantity(item.getExpectedQuantity())
                .actualQuantity(item.getActualQuantity())
                .quantityLeft(item.getQuantityLeft())
                .actualPrice(item.getActualPrice())
                .expectedPrice(item.getExpectedPrice())
                .note(item.getNote())
                .purchaseLocation(item.getPurchaseLocation())
                .brand(item.getBrand())
                .unit(item.getUnit())
                .catologyId(item.getCatologyId())
                .catologyName(item.getCatology() != null ? item.getCatology().getName() : null)
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
        log.info("mapToResponse: expenditureId={}, campaignId={}, status={}",
                expenditure.getId(), expenditure.getCampaignId(), expenditure.getStatus());
        try {
            List<ExpenditureTransactionResponse> txResponses = java.util.Collections.emptyList();
            if (expenditure.getTransactions() != null) {
                txResponses = expenditure.getTransactions().stream()
                        .map(this::mapToTransactionResponse)
                        .collect(java.util.stream.Collectors.toList());
            }

            String proofUrl = null;
            if (expenditure.getTransactions() != null) {
                var payoutTx = expenditure.getTransactions().stream()
                        .filter(t -> "PAYOUT".equalsIgnoreCase(t.getType()) && t.getProofUrl() != null
                                && t.getCreatedAt() != null)
                        .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                        .findFirst()
                        .orElse(null);
                if (payoutTx != null) {
                    proofUrl = payoutTx.getProofUrl();
                }
            }

            return ExpenditureResponse.builder()
                    .id(expenditure.getId())
                    .campaignId(expenditure.getCampaignId())
                    .evidenceDueAt(expenditure.getEvidenceDueAt())
                    .evidenceStatus(expenditure.getEvidenceStatus())
                    .evidenceSubmittedAt(expenditure.getEvidenceSubmittedAt())
                    .totalAmount(expenditure.getTotalAmount())
                    .totalExpectedAmount(expenditure.getTotalExpectedAmount())
                    .totalReceivedAmount(expenditure.getTotalReceivedAmount())
                    .variance(expenditure.getVariance())
                    .isWithdrawalRequested(expenditure.getIsWithdrawalRequested())
                    .plan(expenditure.getPlan())
                    .status(expenditure.getStatus())
                    .startDate(expenditure.getStartDate())
                    .endDate(expenditure.getEndDate())
                    .staffReviewId(expenditure.getStaffReviewId())
                    .rejectReason(expenditure.getRejectReason())
                    .createdAt(expenditure.getCreatedAt())
                    .updatedAt(expenditure.getUpdatedAt())
                    .transactions(txResponses)
                    .disbursementProofUrl(proofUrl)
                    .build();
        } catch (Exception e) {
            log.error("Error mapping expenditure {}: {}", expenditure.getId(), e.getMessage(), e);
            throw e;
        }
    }

    private ExpenditureTransactionResponse mapToTransactionResponse(ExpenditureTransaction t) {
        return ExpenditureTransactionResponse.builder()
                .id(t.getId())
                .expenditureId(t.getExpenditure() != null ? t.getExpenditure().getId() : null)
                .campaignId(t.getExpenditure() != null ? t.getExpenditure().getCampaignId() : null)
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
    public ExpenditureResponse updateExpenditureStatus(Long id,
            com.trustfund.model.request.ReviewExpenditureRequest request) {
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

            // Tự động tạo bản ghi giao dịch PENDING cho AUTHORIZED (đã duyệt và tự yêu cầu
            // rút tiền)
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
                    log.error("⚠️ Warning: Could not fetch bank details for AUTHORIZED transaction: {}",
                            e.getMessage());
                }
                transactionRepository.save(transaction);
                log.info("➔ Created PENDING transaction for AUTHORIZED expenditure {} after Staff approval", id);

            } else if ("ITEMIZED".equalsIgnoreCase(campaign.getType())) {
                log.info("➔ Expenditure {} (ITEMIZED) approved but requires manual withdrawal request", id);
            }

            // Trust Score: cộng điểm nộp đúng hạn hoặc trừ điểm nộp muộn
            // Chỉ cộng nếu evidence chưa được submit (tránh trùng với updateEvidenceStatus)
            // → Điểm uy tín nộp minh chứng được xử lý trong updateEvidenceStatus (khi
            // SUBMITTED)
        }

        // Logic Giải ngân (DISBURSED)
        if ("DISBURSED".equalsIgnoreCase(status)) {
            // Tìm giao dịch PENDING PAYOUT để cập nhật thành COMPLETED
            ExpenditureTransaction transaction = transactionRepository
                    .findByExpenditureIdAndTypeAndStatus(id, "PAYOUT", "PENDING")
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
                // ITEMIZED: dùng balance hiện tại (toàn bộ quỹ vật phẩm Tổng quyên góp)
                // AUTHORIZED: dùng amount từ expenditure (số kế hoạch) vì balance chứ nhiều đợt
                BigDecimal disbursementAmount;
                if ("ITEMIZED".equalsIgnoreCase(campaign.getType())) {
                    // ITEMIZED: dùng transaction.amount đã được set đúng ở requestWithdrawal
                    // (luôn dùng campaign.balance tại thời điểm withdrawal, không dùng
                    // totalReceivedAmount vì bị stale nếu có refund sau khi tạo expenditure)
                    disbursementAmount = (transaction.getAmount() != null
                            && transaction.getAmount().compareTo(BigDecimal.ZERO) > 0)
                                    ? transaction.getAmount()
                                    : (expenditure.getTotalReceivedAmount() != null
                                            ? expenditure.getTotalReceivedAmount()
                                            : BigDecimal.ZERO);
                    log.info(
                            "➔ ITEMIZED disbursement for expenditure {}: using transaction.amount {} (vs stale totalReceivedAmount {})",
                            id, disbursementAmount, expenditure.getTotalReceivedAmount());
                } else {
                    // AUTHORIZED: lấy từ transaction đã tạo khi withdrawal request, fallback về
                    // expenditure totalExpectedAmount
                    disbursementAmount = (transaction.getAmount() != null
                            && transaction.getAmount().compareTo(BigDecimal.ZERO) > 0)
                                    ? transaction.getAmount()
                                    : expenditure.getTotalExpectedAmount();
                }
                transaction.setAmount(disbursementAmount);
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

                // Cập nhật totalReceivedAmount của expenditure = số tiền transaction thực tế
                expenditure.setTotalReceivedAmount(disbursementAmount);
                expenditureRepository.save(expenditure);

                // Trừ số dư chiến dịch khi giải ngân
                campaignService.updateBalance(campaign.getId(), transaction.getAmount().negate());

                log.info("✅ SUCCESS: Completed PAYOUT transaction for expenditure {} — amount={}, campaignId={}", id,
                        disbursementAmount, campaign.getId());

            } catch (Exception e) {
                log.error("❌ ERROR: Failed to complete transaction for disbursement: {}", e.getMessage());
            }
        }

        approvalTaskService.completeTask("EXPENDITURE", id);

        // Gửi thông báo duyệt chi tiêu cho chủ sở hữu chiến dịch
        if ("APPROVED".equalsIgnoreCase(status) || "REJECTED".equalsIgnoreCase(status)
                || "DISBURSED".equalsIgnoreCase(status)) {
            try {
                CampaignResponse campaign = campaignService.getById(expenditure.getCampaignId());
                String notiType;
                String title;
                String content;

                if ("APPROVED".equalsIgnoreCase(status)) {
                    notiType = "EXPENDITURE_APPROVED";
                    title = "Yêu cầu chi tiêu đã được duyệt";
                    content = String.format("Yêu cầu chi tiêu cho chiến dịch '%s' đã được phê duyệt.",
                            campaign.getTitle());
                } else if ("REJECTED".equalsIgnoreCase(status)) {
                    notiType = "EXPENDITURE_REJECTED";
                    title = "Yêu cầu chi tiêu bị từ chối";
                    content = String.format("Yêu cầu chi tiêu cho chiến dịch '%s' đã bị từ chối. Lý do: %s",
                            campaign.getTitle(), request.getReasonReject());
                } else {
                    notiType = "EXPENDITURE_DISBURSED";
                    title = "Khoản chi đã được giải ngân";
                    content = String.format(
                            "Khoản chi cho chiến dịch '%s' đã được giải ngân thành công. Vui lòng kiểm tra tài khoản.",
                            campaign.getTitle());
                }

                java.util.Map<String, Object> notificationData = new java.util.HashMap<>();
                notificationData.put("expenditureId", id);
                notificationData.put("campaignId", campaign.getId());

                com.trustfund.model.request.NotificationRequest notiReq = com.trustfund.model.request.NotificationRequest
                        .builder()
                        .userId(campaign.getFundOwnerId())
                        .type(notiType)
                        .targetId(campaign.getId())
                        .targetType("CAMPAIGN")
                        .title(title)
                        .content(content)
                        .data(notificationData)
                        .build();

                notificationServiceClient.sendNotification(notiReq);
                log.info("➔ Sent notification {} for expenditure request {} to user {}", notiType, id,
                        campaign.getFundOwnerId());
            } catch (Exception e) {
                log.error("❌ Failed to send notification for expenditure status update: {}", e.getMessage());
            }
        }

        return mapToResponse(expenditureRepository.save(expenditure));
    }

    @Override
    @Transactional
    public ExpenditureResponse requestWithdrawal(Long id, java.time.LocalDateTime evidenceDueAt) {
        return requestWithdrawal(id, evidenceDueAt, null);
    }

    @Override
    @Transactional
    public ExpenditureResponse requestWithdrawal(Long id, java.time.LocalDateTime evidenceDueAt,
            BigDecimal withdrawAmount) {
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

        // Xác định số tiền rút: ITEMIZED dùng withdrawAmount từ FE (nếu có) hoặc
        // balance, MONEY dùng totalExpectedAmount
        CampaignResponse campaign = campaignService.getById(expenditure.getCampaignId());
        log.info(
                "requestWithdrawal DEBUG: expenditureId={}, campaignId={}, campaignType={}, campaignBalance={}, totalExpectedAmount={}, withdrawAmountFromUser={}",
                id, expenditure.getCampaignId(), campaign.getType(), campaign.getBalance(),
                expenditure.getTotalExpectedAmount(), withdrawAmount);

        if ("ITEMIZED".equalsIgnoreCase(campaign.getType())) {
            // ITEMIZED: dùng withdrawAmount từ FE, fallback về campaign.balance
            if (withdrawAmount == null || withdrawAmount.compareTo(BigDecimal.ZERO) <= 0) {
                withdrawAmount = (campaign.getBalance() != null) ? campaign.getBalance() : BigDecimal.ZERO;
            }
            log.info("➔ ITEMIZED withdrawal request for expenditure {}: withdrawAmount={}", id, withdrawAmount);
        } else {
            // AUTHORIZED: dùng totalExpectedAmount
            withdrawAmount = expenditure.getTotalExpectedAmount();
            log.info("➔ AUTHORIZED withdrawal request for expenditure {}: totalExpectedAmount={}", id, withdrawAmount);
        }

        // Tạo bản ghi giao dịch PENDING
        ExpenditureTransaction transaction = ExpenditureTransaction.builder()
                .expenditure(expenditure)
                .amount(withdrawAmount)
                .fromUserId(1L)
                .toUserId(campaign.getFundOwnerId())
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
            if (updateItem.getActualPrice() != null) {
                item.setActualPrice(updateItem.getActualPrice());
            }
            if (updateItem.getActualPurchaseLink() != null) {
                item.setActualPurchaseLink(updateItem.getActualPurchaseLink());
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
    public ExpenditureResponse addItemsToExpenditure(Long expenditureId,
            List<CreateExpenditureItemRequest> itemsRequest) {
        Expenditure expenditure = expenditureRepository.findById(expenditureId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Expenditure not found: " + expenditureId));

        List<ExpenditureItem> items = itemsRequest.stream()
                .map(itemReq -> ExpenditureItem.builder()
                        .expenditure(expenditure)
                        .name(itemReq.getName())
                        .expectedPurchaseLink(itemReq.getExpectedPurchaseLink())
                        .expectedQuantity(itemReq.getExpectedQuantity())
                        .actualQuantity(0)
                        .quantityLeft(itemReq.getExpectedQuantity())
                        .actualPrice(BigDecimal.ZERO)
                        .expectedPrice(itemReq.getExpectedPrice())
                        .note(itemReq.getNote())
                        .brand(itemReq.getBrand())
                        .unit(itemReq.getUnit())
                        .purchaseLocation(itemReq.getPurchaseLocation())
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

        int currentLeft = item.getQuantityLeft() != null ? item.getQuantityLeft() : item.getExpectedQuantity();
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
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Expenditure not found: " + expenditureId));
        List<ExpenditureItem> items = expenditureItemRepository.findByExpenditureId(expenditureId);

        BigDecimal totalExpectedAmount = items.stream()
                .map(item -> item.getExpectedPrice().multiply(BigDecimal.valueOf(item.getExpectedQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalAmount = items.stream()
                .map(item -> {
                    BigDecimal price = item.getActualPrice() != null ? item.getActualPrice() : BigDecimal.ZERO;
                    Integer qty = item.getActualQuantity() != null ? item.getActualQuantity() : 0;
                    return price.multiply(BigDecimal.valueOf(qty));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        expenditure.setTotalExpectedAmount(totalExpectedAmount);
        expenditure.setTotalAmount(totalAmount);
        expenditure.setVariance(
                (expenditure.getTotalReceivedAmount() != null ? expenditure.getTotalReceivedAmount() : BigDecimal.ZERO)
                        .subtract(totalAmount));

        // Cập nhật amounts cho từng catology
        List<ExpenditureCatology> catologies = catologyRepository.findByExpenditureId(expenditureId);
        for (ExpenditureCatology cat : catologies) {
            List<ExpenditureItem> catItems = items.stream()
                    .filter(i -> cat.getId().equals(i.getCatologyId()))
                    .collect(Collectors.toList());

            BigDecimal catExpected = catItems.stream()
                    .map(i -> i.getExpectedPrice().multiply(BigDecimal.valueOf(i.getExpectedQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal catActual = catItems.stream()
                    .map(i -> {
                        BigDecimal p = i.getActualPrice() != null ? i.getActualPrice() : BigDecimal.ZERO;
                        Integer q = i.getActualQuantity() != null ? i.getActualQuantity() : 0;
                        return p.multiply(BigDecimal.valueOf(q));
                    })
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            cat.setExpectedAmount(catExpected);
            cat.setActualAmount(catActual);
        }
        catologyRepository.saveAll(catologies);

        return expenditureRepository.save(expenditure);
    }

    @Override
    @Transactional
    public ExpenditureResponse updateEvidenceStatus(Long id, String status) {
        Expenditure expenditure = expenditureRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Expenditure not found: " + id));
        expenditure.setEvidenceStatus(status);
        if ("SUBMITTED".equalsIgnoreCase(status)) {
            expenditure.setEvidenceSubmittedAt(LocalDateTime.now());
        }
        Expenditure saved = expenditureRepository.save(expenditure);

        if ("SUBMITTED".equalsIgnoreCase(status)) {
            approvalTaskService.createAndAssignTask("EVIDENCE", id);

            // Trust Score: cộng/trừ điểm khi nộp minh chứng đúng hạn hoặc muộn
            try {
                CampaignResponse camp = campaignService.getById(expenditure.getCampaignId());
                LocalDateTime dueAt = expenditure.getEvidenceDueAt();
                LocalDateTime now = LocalDateTime.now();
                boolean isOnTime = dueAt == null || !dueAt.isBefore(now);
                String ruleKey = isOnTime ? "ON_TIME_SUBMIT" : "LATE_SUBMIT";
                String description = isOnTime
                        ? "Chi tiêu cho chiến dịch '" + camp.getTitle() + "' nộp minh chứng đúng hạn"
                        : "Chi tiêu cho chiến dịch '" + camp.getTitle() + "' nộp minh chứng muộn";
                trustScoreService.addScore(camp.getFundOwnerId(), ruleKey, id, "EXPENDITURE", description);
            } catch (Exception e) {
                log.error("Error updating trust score for evidence submission of expenditure {}: {}", id,
                        e.getMessage());
            }
        } else if ("APPROVED".equalsIgnoreCase(status) || "REJECTED".equalsIgnoreCase(status)) {
            approvalTaskService.completeTask("EVIDENCE", id);

            // Gửi thông báo duyệt minh chứng chi tiêu cho chủ sở hữu chiến dịch
            try {
                CampaignResponse campaign = campaignService.getById(expenditure.getCampaignId());
                String notiType = "APPROVED".equalsIgnoreCase(status) ? "EVIDENCE_APPROVED" : "EVIDENCE_REJECTED";
                String title = "APPROVED".equalsIgnoreCase(status) ? "Minh chứng chi tiêu đã được duyệt"
                        : "Minh chứng chi tiêu bị từ chối";
                String content = "APPROVED".equalsIgnoreCase(status)
                        ? String.format("Minh chứng cho khoản chi của chiến dịch '%s' đã được phê duyệt.",
                                campaign.getTitle())
                        : String.format(
                                "Minh chứng cho khoản chi của chiến dịch '%s' đã bị từ chối. Vui lòng kiểm tra lại.",
                                campaign.getTitle());

                java.util.Map<String, Object> notificationData = new java.util.HashMap<>();
                notificationData.put("expenditureId", id);
                notificationData.put("campaignId", campaign.getId());

                com.trustfund.model.request.NotificationRequest notiReq = com.trustfund.model.request.NotificationRequest
                        .builder()
                        .userId(campaign.getFundOwnerId())
                        .type(notiType)
                        .targetId(campaign.getId())
                        .targetType("CAMPAIGN")
                        .title(title)
                        .content(content)
                        .data(notificationData)
                        .build();

                notificationServiceClient.sendNotification(notiReq);
                log.info("➔ Sent notification {} for expenditure evidence {} to user {}", notiType, id,
                        campaign.getFundOwnerId());
            } catch (Exception e) {
                log.error("❌ Failed to send notification for expenditure evidence status update: {}", e.getMessage());
            }
        }

        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public ExpenditureTransactionResponse createRefund(Long expenditureId, BigDecimal amount, Long fromUserId,
            String proofUrl,
            String fromBankCode, String fromAccountNumber, String fromAccountHolderName,
            String toBankCode, String toAccountNumber, String toAccountHolderName) {
        Expenditure expenditure = expenditureRepository.findById(expenditureId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Expenditure not found: " + expenditureId));

        // Lấy campaign để biết fundOwnerId
        CampaignResponse campaign = campaignService.getById(expenditure.getCampaignId());
        Long fundOwnerId = campaign.getFundOwnerId();

        // Mặc định toUser = admin (1L)
        Long toUserId = 1L;

        // Tự động fetch bank info từ identity service
        String resolvedFromBankCode = null;
        String resolvedFromAccountNumber = null;
        String resolvedFromAccountHolderName = null;
        String resolvedToBankCode = null;
        String resolvedToAccountNumber = null;
        String resolvedToAccountHolderName = null;

        try {
            // Lấy bank info người gửi (fund owner)
            BankAccountResponse fromBank = identityServiceClient.getPrimaryBankAccount(fundOwnerId);
            if (fromBank != null) {
                resolvedFromBankCode = fromBank.getBankCode();
                resolvedFromAccountNumber = fromBank.getAccountNumber();
                resolvedFromAccountHolderName = fromBank.getAccountHolderName();
            }
        } catch (Exception e) {
            log.warn("Could not fetch from-bank details for refund (userId={}): {}", fundOwnerId, e.getMessage());
        }

        try {
            // Lấy bank info người nhận (admin)
            BankAccountResponse toBank = identityServiceClient.getPrimaryBankAccount(toUserId);
            if (toBank != null) {
                resolvedToBankCode = toBank.getBankCode();
                resolvedToAccountNumber = toBank.getAccountNumber();
                resolvedToAccountHolderName = toBank.getAccountHolderName();
            }
        } catch (Exception e) {
            log.warn("Could not fetch to-bank details for refund (adminId={}): {}", toUserId, e.getMessage());
        }

        ExpenditureTransaction transaction = ExpenditureTransaction.builder()
                .expenditure(expenditure)
                .fromUserId(fundOwnerId)
                .toUserId(toUserId)
                .amount(amount)
                .type("REFUND")
                .status("COMPLETED")
                .proofUrl(proofUrl)
                .fromBankCode(resolvedFromBankCode)
                .fromAccountNumber(resolvedFromAccountNumber)
                .fromAccountHolderName(resolvedFromAccountHolderName)
                .toBankCode(resolvedToBankCode)
                .toAccountNumber(resolvedToAccountNumber)
                .toAccountHolderName(resolvedToAccountHolderName)
                .createdAt(java.time.LocalDateTime.now())
                .build();

        transaction = transactionRepository.save(transaction);

        // Cộng số tiền hoàn dư vào balance chiến dịch
        campaignService.updateBalance(campaign.getId(), amount);
        log.info("✅ Refund: credited {} back to campaign {} balance", amount, campaign.getId());

        return mapToTransactionResponse(transaction);
    }

    @Override
    public java.io.ByteArrayInputStream exportItemsToExcel(Long campaignId) {
        List<ExpenditureItemResponse> items = getExpenditureItemsByCampaign(campaignId);
        return ExpenditureExcelHelper.itemsToExcel(items);
    }

    @Override
    public java.io.ByteArrayInputStream exportItemsToExcelTemplate() {
        return ExpenditureExcelHelper.itemsToExcelTemplate();
    }

    @Override
    public List<ExpenditureTransactionResponse> getAllTransactions() {
        List<ExpenditureTransaction> all = transactionRepository.findAll();
        log.info("getAllTransactions: found {} transactions", all.size());
        return all.stream()
                .map(this::mapToTransactionResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ExpenditureResponse> getExpendituresByStatus(String status) {
        return expenditureRepository.findByStatusOrderByCreatedAtDesc(status).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public BigDecimal getTotalDisbursedByFundOwner(Long fundOwnerId) {
        BigDecimal sum = transactionRepository.sumCompletedPayoutsByFundOwnerId(fundOwnerId);
        return sum != null ? sum : BigDecimal.ZERO;
    }

    @Override
    public List<ExpenditureResponse> getExpendituresByFundOwner(Long fundOwnerId) {
        List<Long> campaignIds = campaignService.getCampaignIdsByFundOwner(fundOwnerId);
        if (campaignIds.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return expenditureRepository.findByCampaignIdInOrderByCreatedAtDesc(campaignIds).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExpenditureCatologyResponse> getExpenditureCategories(Long expenditureId) {
        List<ExpenditureCatology> catologies = catologyRepository.findByExpenditureId(expenditureId);
        return catologies.stream().map(cat -> {
            List<ExpenditureItemResponse> itemResponses = cat.getItems() != null
                    ? cat.getItems().stream().map(this::mapToItemResponse).collect(Collectors.toList())
                    : java.util.Collections.emptyList();
            return ExpenditureCatologyResponse.builder()
                    .id(cat.getId())
                    .expenditureId(cat.getExpenditureId())
                    .name(cat.getName())
                    .description(cat.getDescription())
                    .expectedAmount(cat.getExpectedAmount())
                    .actualAmount(cat.getActualAmount())
                    .withdrawalCondition(cat.getWithdrawalCondition())
                    .items(itemResponses)
                    .createdAt(cat.getCreatedAt())
                    .updatedAt(cat.getUpdatedAt())
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    public com.trustfund.model.response.AuditResultResponse auditExpenditure(Long id) {
        Expenditure expenditure = expenditureRepository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Expenditure not found: " + id));

        List<ExpenditureItem> items = expenditureItemRepository.findByExpenditureId(id);

        List<java.util.Map<String, Object>> itemsToAudit = items.stream().map(item -> {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("itemName", item.getName());
            map.put("brand", item.getBrand());
            map.put("unit", item.getUnit());
            map.put("purchaseLocation", item.getPurchaseLocation());
            map.put("note", item.getNote());
            map.put("declaredPrice", item.getExpectedPrice());
            map.put("quantity", item.getExpectedQuantity());
            return map;
        }).collect(java.util.stream.Collectors.toList());

        com.trustfund.model.response.AuditResultResponse response = perplexityClient.auditExpenseItems(itemsToAudit);
        if (response == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
                    "Cannot get audit result from Perplexity AI");
        }
        return response;
    }
}
