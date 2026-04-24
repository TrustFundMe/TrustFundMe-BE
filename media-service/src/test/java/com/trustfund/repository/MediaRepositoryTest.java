package com.trustfund.repository;

import com.trustfund.model.Media;
import com.trustfund.model.enums.MediaType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
class MediaRepositoryTest {

    @Autowired private TestEntityManager em;
    @Autowired private MediaRepository repo;

    private Media persist(Long postId, Long campaignId, MediaType type, String status) {
        return em.persistAndFlush(Media.builder().postId(postId).campaignId(campaignId)
                .mediaType(type).url("http://x").status(status).build());
    }

    @Test @DisplayName("findByPostId")
    void byPost() {
        persist(1L, null, MediaType.PHOTO, "ACTIVE");
        assertThat(repo.findByPostId(1L)).hasSize(1);
    }

    @Test @DisplayName("findByCampaignId")
    void byCampaign() {
        persist(null, 10L, MediaType.PHOTO, "ACTIVE");
        assertThat(repo.findByCampaignId(10L)).hasSize(1);
    }

    @Test @DisplayName("findByExpenditureIdAndStatusNot_excludesDeleted")
    void byExpenditure_exclude() {
        em.persistAndFlush(Media.builder().expenditureId(5L).mediaType(MediaType.PHOTO)
                .url("http://x").status("ACTIVE").build());
        em.persistAndFlush(Media.builder().expenditureId(5L).mediaType(MediaType.PHOTO)
                .url("http://y").status("DELETED").build());
        assertThat(repo.findByExpenditureIdAndStatusNot(5L, "DELETED")).hasSize(1);
    }

    @Test @DisplayName("findFirstByCampaignIdAndMediaType_returnsFirst")
    void firstImage() {
        persist(null, 10L, MediaType.PHOTO, "ACTIVE");
        persist(null, 10L, MediaType.PHOTO, "ACTIVE");
        Optional<Media> r = repo.findFirstByCampaignIdAndMediaTypeOrderByCreatedAtAsc(10L, MediaType.PHOTO);
        assertThat(r).isPresent();
    }

    @Test @DisplayName("findFirstByCampaignIdAndMediaType_empty")
    void firstImage_empty() {
        assertThat(repo.findFirstByCampaignIdAndMediaTypeOrderByCreatedAtAsc(99L, MediaType.PHOTO)).isEmpty();
    }

    @Test @DisplayName("findByConversationId")
    void byConv() {
        em.persistAndFlush(Media.builder().conversationId(7L).mediaType(MediaType.PHOTO)
                .url("http://x").status("A").build());
        assertThat(repo.findByConversationId(7L)).hasSize(1);
    }

    @Test @DisplayName("save_persistsAndRetrieves")
    void save() {
        Media m = persist(1L, 1L, MediaType.FILE, "ACTIVE");
        assertThat(repo.findById(m.getId())).isPresent();
    }
}
