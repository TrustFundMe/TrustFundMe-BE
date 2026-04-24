package com.trustfund.repository;

import com.trustfund.model.Notification;
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
class NotificationRepositoryTest {

    @Autowired private TestEntityManager em;
    @Autowired private NotificationRepository repo;

    private Notification persist(Long userId, String title, Boolean isRead) {
        return em.persistAndFlush(Notification.builder()
                .userId(userId).type("TEST").targetId(1L).targetType("CAMPAIGN")
                .title(title).content("c").isRead(isRead).build());
    }

    @Test @DisplayName("findByUserIdOrderByCreatedAtDesc_returnsList")
    void byUser() {
        persist(1L, "T1", false);
        persist(1L, "T2", false);
        assertThat(repo.findByUserIdOrderByCreatedAtDesc(1L)).hasSize(2);
    }

    @Test @DisplayName("findByUserIdOrderByCreatedAtDesc_empty")
    void byUser_empty() {
        assertThat(repo.findByUserIdOrderByCreatedAtDesc(999L)).isEmpty();
    }

    @Test @DisplayName("countByUserIdAndIsReadFalse_countsUnread")
    void countUnread() {
        persist(1L, "T1", false);
        persist(1L, "T2", true);
        persist(1L, "T3", false);
        assertThat(repo.countByUserIdAndIsReadFalse(1L)).isEqualTo(2);
    }

    @Test @DisplayName("countByUserIdAndIsReadFalse_zero")
    void countUnread_zero() {
        persist(1L, "T1", true);
        assertThat(repo.countByUserIdAndIsReadFalse(1L)).isZero();
    }

    @Test @DisplayName("findTop15_limitsResults")
    void top15() {
        for (int i = 0; i < 20; i++) persist(1L, "T" + i, false);
        List<Notification> r = repo.findTop15ByUserIdOrderByCreatedAtDesc(1L);
        assertThat(r).hasSize(15);
    }

    @Test @DisplayName("save_persistsAndRetrieves")
    void save() {
        Notification n = persist(1L, "X", false);
        assertThat(repo.findById(n.getId())).isPresent();
    }

    @Test @DisplayName("deleteById_removes")
    void delete() {
        Notification n = persist(1L, "X", false);
        repo.deleteById(n.getId());
        em.flush();
        assertThat(repo.findById(n.getId())).isEmpty();
    }
}
