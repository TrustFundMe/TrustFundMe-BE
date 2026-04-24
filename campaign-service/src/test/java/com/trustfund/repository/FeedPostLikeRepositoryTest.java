package com.trustfund.repository;

import com.trustfund.model.FeedPostLike;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
class FeedPostLikeRepositoryTest {

    @Autowired private TestEntityManager em;
    @Autowired private FeedPostLikeRepository repo;

    private FeedPostLike persist(Long postId, Long userId) {
        return em.persistAndFlush(FeedPostLike.builder().postId(postId).userId(userId).build());
    }

    @Test @DisplayName("existsByPostIdAndUserId_true")
    void exists() {
        persist(1L, 10L);
        assertThat(repo.existsByPostIdAndUserId(1L, 10L)).isTrue();
    }

    @Test @DisplayName("existsByPostIdAndUserId_false")
    void exists_false() {
        assertThat(repo.existsByPostIdAndUserId(99L, 99L)).isFalse();
    }

    @Test @DisplayName("findByPostIdAndUserId_returnsLike")
    void findOne() {
        persist(1L, 10L);
        assertThat(repo.findByPostIdAndUserId(1L, 10L)).isPresent();
    }

    @Test @DisplayName("countByPostId_countsLikes")
    void count() {
        persist(1L, 10L);
        persist(1L, 20L);
        persist(2L, 10L);
        assertThat(repo.countByPostId(1L)).isEqualTo(2);
    }

    @Test @DisplayName("countByPostId_zero")
    void count_zero() {
        assertThat(repo.countByPostId(999L)).isZero();
    }

    @Test @Transactional @DisplayName("deleteByPostIdAndUserId_removes")
    void deleteOne() {
        persist(1L, 10L);
        repo.deleteByPostIdAndUserId(1L, 10L);
        em.flush();
        assertThat(repo.existsByPostIdAndUserId(1L, 10L)).isFalse();
    }

    @Test @Transactional @DisplayName("deleteByPostId_removesAll")
    void deleteByPost() {
        persist(1L, 10L);
        persist(1L, 20L);
        repo.deleteByPostId(1L);
        em.flush();
        assertThat(repo.countByPostId(1L)).isZero();
    }
}
