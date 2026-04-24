package com.trustfund.repository;

import com.trustfund.model.Expenditure;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
class ExpenditureRepositoryTest {

    @Autowired private TestEntityManager em;
    @Autowired private ExpenditureRepository repo;

    private Expenditure persist(Long campaignId, String status, BigDecimal totalAmount) {
        return em.persistAndFlush(Expenditure.builder().campaignId(campaignId).status(status)
                .totalAmount(totalAmount).totalExpectedAmount(totalAmount)
                .totalReceivedAmount(totalAmount).variance(BigDecimal.ZERO)
                .evidenceStatus("PENDING").isWithdrawalRequested(false).build());
    }

    @Test @DisplayName("findByCampaignId_returnsList")
    void byCampaign() {
        persist(10L, "APPROVED", BigDecimal.TEN);
        assertThat(repo.findByCampaignId(10L)).hasSize(1);
    }

    @Test @DisplayName("findByCampaignIdOrderByCreatedAtDesc")
    void byCampaignOrdered() {
        persist(10L, "APPROVED", BigDecimal.TEN);
        persist(10L, "PENDING", BigDecimal.TEN);
        assertThat(repo.findByCampaignIdOrderByCreatedAtDesc(10L)).hasSize(2);
    }

    @Test @DisplayName("findByStatusOrderByCreatedAtDesc")
    void byStatus() {
        persist(10L, "APPROVED", BigDecimal.TEN);
        persist(11L, "APPROVED", BigDecimal.TEN);
        persist(12L, "PENDING", BigDecimal.TEN);
        assertThat(repo.findByStatusOrderByCreatedAtDesc("APPROVED")).hasSize(2);
    }

    @Test @DisplayName("findByCampaignIdInOrderByCreatedAtDesc")
    void byCampaignIds() {
        persist(10L, "APPROVED", BigDecimal.TEN);
        persist(11L, "APPROVED", BigDecimal.TEN);
        assertThat(repo.findByCampaignIdInOrderByCreatedAtDesc(List.of(10L, 11L))).hasSize(2);
    }

    @Test @DisplayName("sumTotalAmountByCampaignIds_onlyDisbursed")
    void sumDisbursed() {
        persist(10L, "DISBURSED", new BigDecimal("100"));
        persist(10L, "DISBURSED", new BigDecimal("200"));
        persist(10L, "PENDING", new BigDecimal("999"));
        assertThat(repo.sumTotalAmountByCampaignIds(List.of(10L))).isEqualByComparingTo("300");
    }

    @Test @DisplayName("sumTotalAmountByCampaignIds_noResults_zero")
    void sumEmpty() {
        assertThat(repo.sumTotalAmountByCampaignIds(List.of(99L))).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
