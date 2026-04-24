package com.trustfund.service;

import com.trustfund.client.IdentityServiceClient;
import com.trustfund.client.NotificationServiceClient;
import com.trustfund.model.Expenditure;
import com.trustfund.model.response.CampaignResponse;
import com.trustfund.model.response.ExpenditureResponse;
import com.trustfund.repository.ExpenditureItemRepository;
import com.trustfund.repository.ExpenditureRepository;
import com.trustfund.repository.ExpenditureTransactionRepository;
import com.trustfund.service.impl.ExpenditureServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExpenditureServiceImplTest {

    @Mock private ExpenditureRepository expenditureRepository;
    @Mock private ExpenditureItemRepository expenditureItemRepository;
    @Mock private ExpenditureTransactionRepository transactionRepository;
    @Mock private CampaignService campaignService;
    @Mock private IdentityServiceClient identityServiceClient;
    @Mock private NotificationServiceClient notificationServiceClient;
    @Mock private TrustScoreService trustScoreService;
    @Mock private JdbcTemplate jdbcTemplate;
    @Mock private ApprovalTaskService approvalTaskService;

    @InjectMocks private ExpenditureServiceImpl service;

    private Expenditure exp;

    @BeforeEach
    void setUp() {
        exp = Expenditure.builder().id(1L).campaignId(10L).status("PENDING_REVIEW")
                .totalAmount(BigDecimal.valueOf(100)).totalExpectedAmount(BigDecimal.valueOf(120))
                .totalReceivedAmount(BigDecimal.valueOf(100)).variance(BigDecimal.ZERO)
                .isWithdrawalRequested(false).evidenceStatus("PENDING").build();
    }

    @Test @DisplayName("getExpendituresByCampaign_returnsList")
    void getByCampaign() {
        when(expenditureRepository.findByCampaignId(10L)).thenReturn(List.of(exp));
        assertThat(service.getExpendituresByCampaign(10L)).hasSize(1);
    }

    @Test @DisplayName("getExpenditureById_notFound_throws")
    void getById_notFound() {
        when(expenditureRepository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getExpenditureById(1L)).isInstanceOf(ResponseStatusException.class);
    }

    @Test @DisplayName("getExpenditureById_ok")
    void getById_ok() {
        when(expenditureRepository.findById(1L)).thenReturn(Optional.of(exp));
        ExpenditureResponse r = service.getExpenditureById(1L);
        assertThat(r.getId()).isEqualTo(1L);
    }

    @Test @DisplayName("requestWithdrawal_alreadyRequested_throws")
    void requestWithdrawal_dup() {
        exp.setIsWithdrawalRequested(true);
        when(expenditureRepository.findById(1L)).thenReturn(Optional.of(exp));
        assertThatThrownBy(() -> service.requestWithdrawal(1L, LocalDateTime.now()))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test @DisplayName("requestWithdrawal_authorized_ok")
    void requestWithdrawal_ok() {
        when(expenditureRepository.findById(1L)).thenReturn(Optional.of(exp));
        when(campaignService.getById(10L)).thenReturn(CampaignResponse.builder()
                .id(10L).type("AUTHORIZED").fundOwnerId(99L).build());
        when(expenditureRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        ExpenditureResponse r = service.requestWithdrawal(1L, LocalDateTime.now());
        assertThat(r.getStatus()).isEqualTo("WITHDRAWAL_REQUESTED");
    }

    @Test @DisplayName("updateEvidenceStatus_submitted_createsTask")
    void updateEvidenceStatus_submitted() {
        when(expenditureRepository.findById(1L)).thenReturn(Optional.of(exp));
        when(expenditureRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(campaignService.getById(10L)).thenReturn(CampaignResponse.builder()
                .id(10L).type("ITEMIZED").fundOwnerId(99L).title("T").build());
        ExpenditureResponse r = service.updateEvidenceStatus(1L, "SUBMITTED");
        assertThat(r.getEvidenceStatus()).isEqualTo("SUBMITTED");
        verify(approvalTaskService).createAndAssignTask("EVIDENCE", 1L);
    }

    @Test @DisplayName("updateEvidenceStatus_approved_completesTask")
    void updateEvidenceStatus_approved() {
        when(expenditureRepository.findById(1L)).thenReturn(Optional.of(exp));
        when(expenditureRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(campaignService.getById(10L)).thenReturn(CampaignResponse.builder()
                .id(10L).type("ITEMIZED").fundOwnerId(99L).title("T").build());
        service.updateEvidenceStatus(1L, "APPROVED");
        verify(approvalTaskService).completeTask("EVIDENCE", 1L);
    }

    @Test @DisplayName("getTotalDisbursedByFundOwner_returnsZeroWhenNull")
    void getTotalDisbursed_null() {
        when(transactionRepository.sumCompletedPayoutsByFundOwnerId(1L)).thenReturn(null);
        assertThat(service.getTotalDisbursedByFundOwner(1L)).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test @DisplayName("getTotalDisbursedByFundOwner_returnsSum")
    void getTotalDisbursed_value() {
        when(transactionRepository.sumCompletedPayoutsByFundOwnerId(1L)).thenReturn(new BigDecimal("500"));
        assertThat(service.getTotalDisbursedByFundOwner(1L)).isEqualByComparingTo(new BigDecimal("500"));
    }

    @Test @DisplayName("getExpendituresByFundOwner_emptyCampaigns_returnsEmpty")
    void getByFundOwner_empty() {
        when(campaignService.getCampaignIdsByFundOwner(1L)).thenReturn(List.of());
        assertThat(service.getExpendituresByFundOwner(1L)).isEmpty();
    }

    @Test @DisplayName("getExpendituresByFundOwner_returnsResults")
    void getByFundOwner_ok() {
        when(campaignService.getCampaignIdsByFundOwner(1L)).thenReturn(List.of(10L));
        when(expenditureRepository.findByCampaignIdInOrderByCreatedAtDesc(any())).thenReturn(List.of(exp));
        assertThat(service.getExpendituresByFundOwner(1L)).hasSize(1);
    }

    @Test @DisplayName("getExpendituresByStatus_returnsList")
    void byStatus() {
        when(expenditureRepository.findByStatusOrderByCreatedAtDesc("APPROVED")).thenReturn(List.of(exp));
        assertThat(service.getExpendituresByStatus("APPROVED")).hasSize(1);
    }

    @Test @DisplayName("getApprovedItemsByCampaign_noApproved_returnsNull")
    void approvedItems_none() {
        when(expenditureRepository.findByCampaignIdOrderByCreatedAtDesc(10L)).thenReturn(List.of(exp));
        assertThat(service.getApprovedItemsByCampaign(10L)).isNull();
    }
}
