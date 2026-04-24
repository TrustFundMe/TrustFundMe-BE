package com.trustfund.repository;

import com.trustfund.model.CampaignFollow;
import com.trustfund.model.CampaignFollowId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
class CampaignFollowRepositoryTest {

    @Autowired private TestEntityManager em;
    @Autowired private CampaignFollowRepository repo;

    private CampaignFollow persist(Long campaignId, Long userId) {
        return em.persistAndFlush(CampaignFollow.builder()
                .id(new CampaignFollowId(campaignId, userId)).build());
    }

    @Test @DisplayName("existsById_CampaignIdAndId_UserId_true")
    void exists() {
        persist(10L, 1L);
        assertThat(repo.existsById_CampaignIdAndId_UserId(10L, 1L)).isTrue();
    }

    @Test @DisplayName("existsById_CampaignIdAndId_UserId_false")
    void exists_false() {
        assertThat(repo.existsById_CampaignIdAndId_UserId(99L, 99L)).isFalse();
    }

    @Test @DisplayName("countById_CampaignId_returnsCount")
    void count() {
        persist(10L, 1L);
        persist(10L, 2L);
        persist(11L, 1L);
        assertThat(repo.countById_CampaignId(10L)).isEqualTo(2L);
    }

    @Test @DisplayName("findById_CampaignIdOrdered")
    void findByCampaign() {
        persist(10L, 1L);
        persist(10L, 2L);
        assertThat(repo.findById_CampaignIdOrderByFollowedAtDesc(10L)).hasSize(2);
    }

    @Test @DisplayName("findById_UserIdOrdered")
    void findByUser() {
        persist(10L, 1L);
        persist(11L, 1L);
        assertThat(repo.findById_UserIdOrderByFollowedAtDesc(1L)).hasSize(2);
    }

    @Test @DisplayName("save_persists")
    void save() {
        persist(10L, 1L);
        assertThat(repo.count()).isEqualTo(1L);
    }
}
