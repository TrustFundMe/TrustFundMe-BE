package com.trustfund.service.impl;

import com.trustfund.client.IdentityServiceClient;
import com.trustfund.model.Campaign;
import com.trustfund.model.ExpenditureTransaction;
import com.trustfund.model.InternalTransaction;
import com.trustfund.model.response.AggregatedTransactionResponse;
import com.trustfund.model.response.UserInfoResponse;
import com.trustfund.repository.CampaignRepository;
import com.trustfund.repository.ExpenditureTransactionRepository;
import com.trustfund.repository.InternalTransactionRepository;
import com.trustfund.service.CampaignTransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CampaignTransactionServiceImpl implements CampaignTransactionService {

    private final CampaignRepository campaignRepository;
    private final ExpenditureTransactionRepository expenditureTransactionRepository;
    private final InternalTransactionRepository internalTransactionRepository;
    private final RestTemplate restTemplate;
    private final IdentityServiceClient identityServiceClient;

    @Value("${payment.service.url}")
    private String paymentServiceUrl;

    @Override
    public List<AggregatedTransactionResponse> getCampaignTransactionHistory(Long campaignId) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new RuntimeException("Campaign not found"));

        List<AggregatedTransactionResponse> allTransactions = new ArrayList<>();

        // 1. Fetch Donations from payment-service (using donationAmount which excludes tips)
        try {
            String url = paymentServiceUrl + "/api/payments/campaign/" + campaignId + "/paid-donations";
            log.info("➔ [TRANSACTION-HISTORY] Fetching donations from: {}", url);
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    url, HttpMethod.GET, null, new ParameterizedTypeReference<List<Map<String, Object>>>() {});
            
            if (response.getBody() != null) {
                log.info("➔ [TRANSACTION-HISTORY] Received {} donations from payment-service", response.getBody().size());
                response.getBody().forEach(d -> {
                    try {
                        boolean isAnon = Boolean.TRUE.equals(d.get("anonymous"));
                        Object donorId = d.get("donorId");
                        Object amountObj = d.get("donationAmount");
                        Object dateObj = d.get("createdAt");
                        Object idObj = d.get("id");

                        String donorName;
                        if (isAnon) {
                            donorName = "Người ủng hộ ẩn danh";
                        } else {
                            UserInfoResponse donor = identityServiceClient.getUserById(
                                    donorId != null ? Long.valueOf(donorId.toString()) : null);
                            donorName = (donor != null && donor.getFullName() != null)
                                    ? "Quyên góp từ " + donor.getFullName()
                                    : "Quyên góp từ #" + donorId;
                        }

                        allTransactions.add(AggregatedTransactionResponse.builder()
                                .id("DON-" + (idObj != null ? idObj.toString() : "unknown"))
                                .type("DONATION")
                                .description(donorName)
                                .amount(amountObj != null ? new BigDecimal(amountObj.toString()) : BigDecimal.ZERO)
                                .date(dateObj != null ? dateObj.toString() : null)
                                .build());
                    } catch (Exception e) {
                        log.error("➔ [TRANSACTION-HISTORY] Error processing individual donation: {}", d, e);
                    }
                });
            } else {
                log.warn("➔ [TRANSACTION-HISTORY] payment-service returned null body");
            }
        } catch (Exception e) {
            log.error("➔ [TRANSACTION-HISTORY] Failed to fetch donations for campaign {}: {}", campaignId, e.getMessage());
        }

        // 2. Fetch Expenditure Transactions
        List<ExpenditureTransaction> expenditures = expenditureTransactionRepository.findByCampaignIdAndStatus(campaignId, "COMPLETED");
        expenditures.forEach(e -> {
            boolean isRefund = "REFUND".equals(e.getType());
            allTransactions.add(AggregatedTransactionResponse.builder()
                    .id("EXP-" + e.getId())
                    .type(isRefund ? "REFUND" : "EXPENDITURE")
                    .description(isRefund ? "Hoàn trả chi phí #" + e.getExpenditure().getId() : "Chi tiêu #" + e.getExpenditure().getId())
                    .amount(isRefund ? e.getAmount() : e.getAmount().negate())
                    .date(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null)
                    .build());
        });

        // 3. Fetch Internal Transactions with APPROVED status
        List<InternalTransaction> allForCampaign = internalTransactionRepository.findByFromCampaignIdOrToCampaignIdOrderByCreatedAtDesc(campaignId, campaignId);
        allForCampaign = allForCampaign.stream()
                .filter(t -> com.trustfund.model.enums.InternalTransactionStatus.APPROVED.equals(t.getStatus()))
                .collect(java.util.stream.Collectors.toList());
        allForCampaign.forEach(t -> {
            BigDecimal amount;
            Long relatedCampaignId;
            String description;
            if (campaignId.equals(t.getFromCampaignId())) {
                // Đang gửi tiền → hiển thị campaign đích
                amount = t.getAmount().negate();
                relatedCampaignId = t.getToCampaignId();
                Campaign related = campaignRepository.findById(t.getToCampaignId()).orElse(null);
                String targetTitle = related != null ? related.getTitle() : "#" + t.getToCampaignId();
                description = "Hỗ trợ cho chiến dịch " + targetTitle;
            } else {
                // Đang nhận tiền
                amount = t.getAmount();
                relatedCampaignId = t.getFromCampaignId();
                description = "Nhận tiền hỗ trợ từ Quỹ Chung";
            }
            allTransactions.add(AggregatedTransactionResponse.builder()
                    .id("INT-" + t.getId())
                    .type("INTERNAL_TRANSFER")
                    .description(description)
                    .amount(amount)
                    .date(t.getCreatedAt() != null ? t.getCreatedAt().toString() : null)
                    .relatedCampaignId(relatedCampaignId)
                    .build());
        });

        // Sort by date desc
        allTransactions.sort((a, b) -> {
            if (a.getDate() == null || b.getDate() == null) return 0;
            return b.getDate().compareTo(a.getDate());
        });

        // Calculate running balance backwards from current balance
        BigDecimal runningBalance = campaign.getBalance();
        for (AggregatedTransactionResponse tx : allTransactions) {
            tx.setBalanceAfter(runningBalance);
            runningBalance = runningBalance.subtract(tx.getAmount());
        }

        return allTransactions;
    }
}
