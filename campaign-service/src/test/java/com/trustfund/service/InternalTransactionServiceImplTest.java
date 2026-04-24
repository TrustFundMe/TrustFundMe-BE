package com.trustfund.service;

import com.trustfund.model.Campaign;
import com.trustfund.model.InternalTransaction;
import com.trustfund.model.enums.InternalTransactionStatus;
import com.trustfund.model.enums.InternalTransactionType;
import com.trustfund.repository.CampaignRepository;
import com.trustfund.repository.InternalTransactionRepository;
import com.trustfund.service.impl.InternalTransactionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InternalTransactionServiceImplTest {

    @Mock private InternalTransactionRepository txRepo;
    @Mock private CampaignRepository campaignRepo;

    @InjectMocks private InternalTransactionServiceImpl service;

    private Campaign src;
    private Campaign dst;

    @BeforeEach
    void setUp() {
        src = Campaign.builder().id(1L).type(Campaign.TYPE_GENERAL_FUND).balance(new BigDecimal("1000")).build();
        dst = Campaign.builder().id(2L).type("ITEMIZED").balance(new BigDecimal("0")).build();
    }

    @Test @DisplayName("createTransaction_zeroAmount_throws")
    void zeroAmount() {
        assertThatThrownBy(() -> service.createTransaction(1L, 2L, BigDecimal.ZERO,
                InternalTransactionType.SUPPORT, "r", 1L, null, InternalTransactionStatus.PENDING))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test @DisplayName("createTransaction_generalFundInsufficient_throws")
    void insufficient() {
        when(campaignRepo.findById(1L)).thenReturn(Optional.of(src));
        assertThatThrownBy(() -> service.createTransaction(1L, 2L, new BigDecimal("5000"),
                InternalTransactionType.SUPPORT, "r", 1L, null, InternalTransactionStatus.PENDING))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test @DisplayName("createTransaction_pendingStatus_savesWithoutTransfer")
    void pending_ok() {
        when(campaignRepo.findById(1L)).thenReturn(Optional.of(src));
        when(campaignRepo.findById(2L)).thenReturn(Optional.of(dst));
        when(txRepo.save(any())).thenAnswer(i -> { InternalTransaction t = i.getArgument(0); t.setId(1L); return t; });
        InternalTransaction r = service.createTransaction(1L, 2L, new BigDecimal("100"),
                InternalTransactionType.SUPPORT, "r", 1L, null, InternalTransactionStatus.PENDING);
        assertThat(r.getId()).isEqualTo(1L);
        verify(campaignRepo, never()).save(any());
    }

    @Test @DisplayName("createTransaction_completed_transfersFunds")
    void completed_transfers() {
        when(campaignRepo.findById(1L)).thenReturn(Optional.of(src));
        when(campaignRepo.findById(2L)).thenReturn(Optional.of(dst));
        when(txRepo.save(any())).thenAnswer(i -> { InternalTransaction t = i.getArgument(0); t.setId(1L); return t; });
        service.createTransaction(1L, 2L, new BigDecimal("100"),
                InternalTransactionType.SUPPORT, "r", 1L, null, InternalTransactionStatus.COMPLETED);
        verify(campaignRepo, atLeast(2)).save(any());
    }

    @Test @DisplayName("getAll_returnsList")
    void getAll() {
        when(txRepo.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(InternalTransaction.builder().id(1L).build()));
        assertThat(service.getAll()).hasSize(1);
    }

    @Test @DisplayName("getById_notFound_throws")
    void getById_notFound() {
        when(txRepo.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getById(1L)).isInstanceOf(ResponseStatusException.class);
    }

    @Test @DisplayName("delete_completedTx_throws")
    void delete_completed() {
        InternalTransaction tx = InternalTransaction.builder().id(1L).status(InternalTransactionStatus.COMPLETED).build();
        when(txRepo.findById(1L)).thenReturn(Optional.of(tx));
        assertThatThrownBy(() -> service.delete(1L)).isInstanceOf(ResponseStatusException.class);
    }

    @Test @DisplayName("delete_pendingTx_ok")
    void delete_pending() {
        InternalTransaction tx = InternalTransaction.builder().id(1L).status(InternalTransactionStatus.PENDING).build();
        when(txRepo.findById(1L)).thenReturn(Optional.of(tx));
        service.delete(1L);
        verify(txRepo).deleteById(1L);
    }

    @Test @DisplayName("updateTransactionStatus_alreadyCompleted_throws")
    void update_completed() {
        InternalTransaction tx = InternalTransaction.builder().id(1L).status(InternalTransactionStatus.COMPLETED).build();
        when(txRepo.findById(1L)).thenReturn(Optional.of(tx));
        assertThatThrownBy(() -> service.updateTransactionStatus(1L, InternalTransactionStatus.APPROVED))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test @DisplayName("updateEvidence_notFound_throws")
    void updateEvidence_notFound() {
        when(txRepo.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.updateEvidence(1L, 10L)).isInstanceOf(ResponseStatusException.class);
    }

    @Test @DisplayName("updateEvidence_ok")
    void updateEvidence_ok() {
        InternalTransaction tx = InternalTransaction.builder().id(1L).build();
        when(txRepo.findById(1L)).thenReturn(Optional.of(tx));
        when(txRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        InternalTransaction r = service.updateEvidence(1L, 99L);
        assertThat(r.getEvidenceImageId()).isEqualTo(99L);
    }

    @Test @DisplayName("getGeneralFundStats_returnsMap")
    void stats() {
        when(campaignRepo.findById(1L)).thenReturn(Optional.of(src));
        when(txRepo.sumOutcomeFromGeneralFund()).thenReturn(new BigDecimal("100"));
        when(txRepo.sumIncomeToGeneralFund()).thenReturn(new BigDecimal("50"));
        when(txRepo.countByStatus(InternalTransactionStatus.APPROVED)).thenReturn(5L);
        assertThat(service.getGeneralFundStats()).containsKeys("balance", "outcome", "income", "transactionCount");
    }
}
