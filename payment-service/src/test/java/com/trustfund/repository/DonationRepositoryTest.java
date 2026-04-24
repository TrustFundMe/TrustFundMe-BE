package com.trustfund.repository;

import com.trustfund.model.Donation;
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
class DonationRepositoryTest {

    @Autowired private TestEntityManager em;
    @Autowired private DonationRepository repo;

    private Donation persist(Long campaignId, Long donorId, String status, BigDecimal amount) {
        Donation d = Donation.builder()
                .campaignId(campaignId).donorId(donorId).status(status)
                .donationAmount(amount).tipAmount(BigDecimal.ZERO).totalAmount(amount)
                .isAnonymous(false).isBalanceSynchronized(false).build();
        return em.persistAndFlush(d);
    }

    @Test @DisplayName("findByCampaignId_returnsList")
    void byCampaign() {
        persist(10L, 1L, "PAID", new BigDecimal("100"));
        assertThat(repo.findByCampaignId(10L)).hasSize(1);
    }

    @Test @DisplayName("findByDonorId_returnsList")
    void byDonor() {
        persist(10L, 5L, "PAID", new BigDecimal("100"));
        assertThat(repo.findByDonorId(5L)).hasSize(1);
    }

    @Test @DisplayName("sumDonationAmountByCampaignId_returnsTotal")
    void sumByCampaign() {
        persist(10L, 1L, "PAID", new BigDecimal("100"));
        persist(10L, 2L, "PAID", new BigDecimal("250"));
        persist(10L, 3L, "PENDING", new BigDecimal("999"));
        assertThat(repo.sumDonationAmountByCampaignId(10L)).isEqualByComparingTo("350");
    }

    @Test @DisplayName("sumDonationAmountByCampaignIds_aggregates")
    void sumByCampaigns() {
        persist(10L, 1L, "PAID", new BigDecimal("100"));
        persist(20L, 2L, "PAID", new BigDecimal("200"));
        assertThat(repo.sumDonationAmountByCampaignIds(List.of(10L, 20L))).isEqualByComparingTo("300");
    }

    @Test @DisplayName("countUniqueDonorsByCampaignId_distinct")
    void uniqueDonors() {
        persist(10L, 1L, "PAID", new BigDecimal("100"));
        persist(10L, 1L, "PAID", new BigDecimal("100"));
        persist(10L, 2L, "PAID", new BigDecimal("100"));
        assertThat(repo.countUniqueDonorsByCampaignId(10L)).isEqualTo(2L);
    }

    @Test @DisplayName("countByDonorId_paidOnly")
    void countByDonor() {
        persist(10L, 1L, "PAID", new BigDecimal("100"));
        persist(11L, 1L, "PENDING", new BigDecimal("100"));
        assertThat(repo.countByDonorId(1L)).isEqualTo(1L);
    }

    @Test @DisplayName("findByStatusOrderByCreatedAtDesc_returnsList")
    void byStatus() {
        persist(10L, 1L, "PAID", new BigDecimal("100"));
        assertThat(repo.findByStatusOrderByCreatedAtDesc("PAID")).hasSize(1);
    }

    @Test @DisplayName("findByCampaignIdAndStatusOrderByCreatedAtAsc")
    void byCampaignStatus() {
        persist(10L, 1L, "PAID", new BigDecimal("100"));
        assertThat(repo.findByCampaignIdAndStatusOrderByCreatedAtAsc(10L, "PAID")).hasSize(1);
    }
}
