package com.trustfund.service.impl;

import com.trustfund.model.enums.InternalTransactionStatus;
import com.trustfund.model.enums.InternalTransactionType;
import com.trustfund.model.response.CampaignStatisticsResponse;
import com.trustfund.repository.InternalTransactionRepository;
import com.trustfund.model.response.ExpenditureResponse;
import com.trustfund.repository.CampaignRepository;
import com.trustfund.repository.ExpenditureRepository;
import com.trustfund.service.CampaignService;
import com.trustfund.service.CampaignStatisticsService;
import com.trustfund.service.ExpenditureService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CampaignStatisticsServiceImpl implements CampaignStatisticsService {

    private final CampaignRepository campaignRepository;
    private final ExpenditureRepository expenditureRepository;
    private final InternalTransactionRepository internalTransactionRepository;
    private final CampaignService campaignService;
    private final ExpenditureService expenditureService;

    private static final String ICON_TOTAL_RECEIVED = "https://cdn-icons-png.flaticon.com/512/3069/3069472.png";
    private static final String ICON_TOTAL_SPENT = "https://cdn-icons-png.flaticon.com/512/4393/4393152.png";
    private static final String ICON_CURRENT_BALANCE = "https://cdn-icons-png.flaticon.com/512/16184/16184101.png";
    private static final String ICON_TOTAL_RECEIVED_FROM_GENERAL_FUND = "https://cdn-icons-png.flaticon.com/512/5529/5529892.png";

    @Override
    @Transactional(readOnly = true)
    public CampaignStatisticsResponse getStatisticsByFundOwner(Long fundOwnerId) {
        List<Long> campaignIds = campaignService.getCampaignIdsByFundOwner(fundOwnerId);

        // currentBalance = tong balance cac campaign cua user
        BigDecimal currentBalance = campaignRepository.sumBalanceByFundOwnerId(fundOwnerId);
        if (currentBalance == null) {
            currentBalance = BigDecimal.ZERO;
        }

        // totalSpent = tong cac khoan da giai ngan (DISBURSED)
        BigDecimal totalSpent = expenditureRepository.sumTotalAmountByCampaignIds(campaignIds);
        if (totalSpent == null) {
            totalSpent = BigDecimal.ZERO;
        }

        // totalReceivedFromGeneralFund = tong InternalTransaction type=SUPPORT, status=APPROVED, fromCampaignId IN campaignIds
        BigDecimal totalReceivedFromGeneralFund = BigDecimal.ZERO;
        if (!campaignIds.isEmpty()) {
            for (Long campaignId : campaignIds) {
                BigDecimal sum = internalTransactionRepository
                        .sumAmountByFromCampaignIdAndTypeAndStatus(
                                campaignId,
                                InternalTransactionType.SUPPORT,
                                InternalTransactionStatus.APPROVED);
                if (sum != null) {
                    totalReceivedFromGeneralFund = totalReceivedFromGeneralFund.add(sum);
                }
            }
        }

        // totalReceived = totalSpent + currentBalance + totalReceivedFromGeneralFund
        BigDecimal totalReceived = totalSpent.add(currentBalance).add(totalReceivedFromGeneralFund);

        // danh sach expenditure thuoc cac campaign cua user, chi lay status DISBURSED
        List<ExpenditureResponse> allExpenditures = expenditureService.getExpendituresByFundOwner(fundOwnerId);
        List<ExpenditureResponse> expenditures = allExpenditures.stream()
                .filter(e -> "DISBURSED".equalsIgnoreCase(e.getStatus()))
                .collect(Collectors.toList());

        // campaignMap = map campaignId -> title de hien thi ten quy (query tat ca campaigns cua user)
        Map<Long, String> campaignMap = new HashMap<>();
        campaignRepository.findByFundOwnerId(fundOwnerId)
                .forEach(c -> campaignMap.put(c.getId(), c.getTitle()));

        return CampaignStatisticsResponse.builder()
                .totalReceived(totalReceived)
                .totalSpent(totalSpent)
                .currentBalance(currentBalance)
                .totalReceivedFromGeneralFund(totalReceivedFromGeneralFund)
                .iconTotalReceived(ICON_TOTAL_RECEIVED)
                .iconTotalSpent(ICON_TOTAL_SPENT)
                .iconCurrentBalance(ICON_CURRENT_BALANCE)
                .iconTotalReceivedFromGeneralFund(ICON_TOTAL_RECEIVED_FROM_GENERAL_FUND)
                .expenditures(expenditures)
                .campaignMap(campaignMap)
                .build();
    }
}
