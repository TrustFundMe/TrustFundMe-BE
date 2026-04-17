package com.trustfund.service.impl;

import com.trustfund.model.Campaign;
import com.trustfund.model.ExpenditureTransaction;
import com.trustfund.model.InternalTransaction;
import com.trustfund.model.response.AggregatedTransactionResponse;
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
import java.util.Comparator;
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
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    url, HttpMethod.GET, null, new ParameterizedTypeReference<List<Map<String, Object>>>() {});
            
            if (response.getBody() != null) {
                response.getBody().forEach(d -> {
                    boolean isAnon = Boolean.TRUE.equals(d.get("anonymous"));
                    String donorName = isAnon ? "Người ủng hộ ẩn danh" : "Quyên góp từ #" + d.get("donorId");
                    allTransactions.add(AggregatedTransactionResponse.builder()
                            .id("DON-" + d.get("id"))
                            .type("DONATION")
                            .description(donorName)
                            .amount(new BigDecimal(d.get("donationAmount").toString()))
                            .date((String) d.get("createdAt"))
                            .build());
                });
            }
        } catch (Exception e) {
            log.error("Failed to fetch donations for campaign {}", campaignId, e);
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

        // 3. Fetch Internal Transactions
        List<InternalTransaction> internals = internalTransactionRepository.findByToCampaignIdAndStatusOrderByCreatedAtDesc(
                campaignId, com.trustfund.model.enums.InternalTransactionStatus.APPROVED);
        internals.forEach(t -> {
            allTransactions.add(AggregatedTransactionResponse.builder()
                    .id("INT-" + t.getId())
                    .type("INTERNAL_TRANSFER")
                    .description(t.getReason() != null ? "Quỹ chung: " + t.getReason() : "Chuyển từ Quỹ Chung")
                    .amount(t.getAmount())
                    .date(t.getCreatedAt() != null ? t.getCreatedAt().toString() : null)
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
