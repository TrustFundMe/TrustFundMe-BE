package com.trustfund.repository;

import com.trustfund.model.Flag;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
class FlagRepositoryTest {

    @Autowired private TestEntityManager em;
    @Autowired private FlagRepository repo;

    private Flag persist(Long userId, Long campaignId, Long postId, String status) {
        return em.persistAndFlush(Flag.builder()
                .userId(userId).campaignId(campaignId).postId(postId)
                .reason("r").status(status).build());
    }

    @Test @DisplayName("existsByUserIdAndCampaignId_true")
    void existsCampaign() {
        persist(1L, 10L, null, "PENDING");
        assertThat(repo.existsByUserIdAndCampaignId(1L, 10L)).isTrue();
    }

    @Test @DisplayName("existsByUserIdAndCampaignId_false")
    void existsCampaign_false() {
        assertThat(repo.existsByUserIdAndCampaignId(99L, 99L)).isFalse();
    }

    @Test @DisplayName("existsByUserIdAndPostId_true")
    void existsPost() {
        persist(1L, null, 20L, "PENDING");
        assertThat(repo.existsByUserIdAndPostId(1L, 20L)).isTrue();
    }

    @Test @DisplayName("findByStatus_paged")
    void byStatus() {
        persist(1L, 10L, null, "PENDING");
        assertThat(repo.findByStatus("PENDING", PageRequest.of(0, 10)).getTotalElements()).isEqualTo(1L);
    }

    @Test @DisplayName("findByCampaignId_paged")
    void byCampaign() {
        persist(1L, 10L, null, "PENDING");
        assertThat(repo.findByCampaignId(10L, PageRequest.of(0, 10)).getTotalElements()).isEqualTo(1L);
    }

    @Test @DisplayName("findByPostId_paged")
    void byPost() {
        persist(1L, null, 20L, "PENDING");
        assertThat(repo.findByPostId(20L, PageRequest.of(0, 10)).getTotalElements()).isEqualTo(1L);
    }

    @Test @DisplayName("findByUserId_paged")
    void byUser() {
        persist(1L, 10L, null, "PENDING");
        persist(1L, 11L, null, "PENDING");
        assertThat(repo.findByUserId(1L, PageRequest.of(0, 10)).getTotalElements()).isEqualTo(2L);
    }

    @Test @DisplayName("countPendingFlagsByPostIds_groupsByPost")
    void countPendingByPost() {
        persist(1L, null, 20L, "PENDING");
        persist(2L, null, 20L, "PENDING");
        persist(1L, null, 21L, "PENDING");
        List<Object[]> rows = repo.countPendingFlagsByPostIds(List.of(20L, 21L), "PENDING");
        assertThat(rows).hasSize(2);
    }
}
