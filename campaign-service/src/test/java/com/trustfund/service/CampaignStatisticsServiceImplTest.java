package com.trustfund.service;

import com.trustfund.model.Campaign;
import com.trustfund.model.enums.InternalTransactionStatus;
import com.trustfund.model.enums.InternalTransactionType;
import com.trustfund.model.response.CampaignStatisticsResponse;
import com.trustfund.model.response.ExpenditureResponse;
import com.trustfund.repository.CampaignRepository;
import com.trustfund.repository.ExpenditureRepository;
import com.trustfund.repository.InternalTransactionRepository;
import com.trustfund.service.impl.CampaignStatisticsServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CampaignStatisticsServiceImplTest {

    @Mock private CampaignRepository campaignRepository;
    @Mock private ExpenditureRepository expenditureRepository;
    @Mock private InternalTransactionRepository internalTransactionRepository;
    @Mock private CampaignService campaignService;
    @Mock private ExpenditureService expenditureService;

    @InjectMocks private CampaignStatisticsServiceImpl service;

    @Test @DisplayName("noCampaigns_returnsZero")
    void noCampaigns() {
        when(campaignService.getCampaignIdsByFundOwner(1L)).thenReturn(List.of());
        when(campaignRepository.sumBalanceByFundOwnerId(1L)).thenReturn(null);
        when(expenditureRepository.sumTotalAmountByCampaignIds(any())).thenReturn(null);
        when(expenditureService.getExpendituresByFundOwner(1L)).thenReturn(List.of());
        when(campaignRepository.findByFundOwnerId(1L)).thenReturn(List.of());
        CampaignStatisticsResponse r = service.getStatisticsByFundOwner(1L);
        assertThat(r.getTotalReceived()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(r.getCurrentBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test @DisplayName("withCampaigns_sumsCorrectly")
    void withCampaigns() {
        when(campaignService.getCampaignIdsByFundOwner(1L)).thenReturn(List.of(10L));
        when(campaignRepository.sumBalanceByFundOwnerId(1L)).thenReturn(new BigDecimal("500"));
        when(expenditureRepository.sumTotalAmountByCampaignIds(any())).thenReturn(new BigDecimal("200"));
        when(internalTransactionRepository.sumAmountByFromCampaignIdAndTypeAndStatus(10L,
                InternalTransactionType.SUPPORT, InternalTransactionStatus.APPROVED))
                .thenReturn(new BigDecimal("100"));
        when(expenditureService.getExpendituresByFundOwner(1L)).thenReturn(List.of());
        when(campaignRepository.findByFundOwnerId(1L)).thenReturn(
                List.of(Campaign.builder().id(10L).title("Camp").build()));

        CampaignStatisticsResponse r = service.getStatisticsByFundOwner(1L);
        assertThat(r.getTotalReceived()).isEqualByComparingTo(new BigDecimal("800"));
        assertThat(r.getTotalSpent()).isEqualByComparingTo(new BigDecimal("200"));
        assertThat(r.getCurrentBalance()).isEqualByComparingTo(new BigDecimal("500"));
        assertThat(r.getTotalReceivedFromGeneralFund()).isEqualByComparingTo(new BigDecimal("100"));
    }

    @Test @DisplayName("filtersExpendituresByDisbursedStatus")
    void filterDisbursed() {
        ExpenditureResponse disbursed = ExpenditureResponse.builder().id(1L).status("DISBURSED").build();
        ExpenditureResponse pending = ExpenditureResponse.builder().id(2L).status("PENDING").build();
        when(campaignService.getCampaignIdsByFundOwner(1L)).thenReturn(List.of());
        when(campaignRepository.sumBalanceByFundOwnerId(1L)).thenReturn(BigDecimal.ZERO);
        when(expenditureRepository.sumTotalAmountByCampaignIds(any())).thenReturn(BigDecimal.ZERO);
        when(expenditureService.getExpendituresByFundOwner(1L)).thenReturn(List.of(disbursed, pending));
        when(campaignRepository.findByFundOwnerId(1L)).thenReturn(List.of());
        CampaignStatisticsResponse r = service.getStatisticsByFundOwner(1L);
        assertThat(r.getExpenditures()).hasSize(1);
    }

    @Test @DisplayName("buildsCampaignMap")
    void campaignMap() {
        when(campaignService.getCampaignIdsByFundOwner(1L)).thenReturn(List.of());
        when(campaignRepository.sumBalanceByFundOwnerId(1L)).thenReturn(BigDecimal.ZERO);
        when(expenditureRepository.sumTotalAmountByCampaignIds(any())).thenReturn(BigDecimal.ZERO);
        when(expenditureService.getExpendituresByFundOwner(1L)).thenReturn(List.of());
        when(campaignRepository.findByFundOwnerId(1L)).thenReturn(
                List.of(Campaign.builder().id(10L).title("A").build(),
                        Campaign.builder().id(20L).title("B").build()));
        CampaignStatisticsResponse r = service.getStatisticsByFundOwner(1L);
        assertThat(r.getCampaignMap()).containsEntry(10L, "A").containsEntry(20L, "B");
    }

    @Test @DisplayName("iconFieldsPopulated")
    void icons() {
        when(campaignService.getCampaignIdsByFundOwner(1L)).thenReturn(List.of());
        when(campaignRepository.sumBalanceByFundOwnerId(1L)).thenReturn(BigDecimal.ZERO);
        when(expenditureRepository.sumTotalAmountByCampaignIds(any())).thenReturn(BigDecimal.ZERO);
        when(expenditureService.getExpendituresByFundOwner(1L)).thenReturn(List.of());
        when(campaignRepository.findByFundOwnerId(1L)).thenReturn(List.of());
        CampaignStatisticsResponse r = service.getStatisticsByFundOwner(1L);
        assertThat(r.getIconTotalReceived()).isNotBlank();
        assertThat(r.getIconTotalSpent()).isNotBlank();
        assertThat(r.getIconCurrentBalance()).isNotBlank();
    }
}
