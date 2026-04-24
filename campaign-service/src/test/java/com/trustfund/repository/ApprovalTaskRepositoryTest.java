package com.trustfund.repository;

import com.trustfund.model.ApprovalTask;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
class ApprovalTaskRepositoryTest {

    @Autowired private TestEntityManager em;
    @Autowired private ApprovalTaskRepository repo;

    private ApprovalTask persist(String type, Long targetId, Long staffId, String status) {
        return em.persistAndFlush(ApprovalTask.builder()
                .type(type).targetId(targetId).staffId(staffId).status(status).build());
    }

    @Test @DisplayName("findByStaffId_returnsTasks")
    void byStaff() {
        persist("CAMPAIGN", 1L, 100L, "PENDING");
        persist("FLAG", 2L, 100L, "PENDING");
        assertThat(repo.findByStaffId(100L)).hasSize(2);
    }

    @Test @DisplayName("findByStatus_returnsList")
    void byStatus() {
        persist("CAMPAIGN", 1L, 100L, "PENDING");
        persist("CAMPAIGN", 2L, 100L, "COMPLETED");
        assertThat(repo.findByStatus("PENDING")).hasSize(1);
    }

    @Test @DisplayName("findByTypeAndTargetId_returnsTask")
    void byTypeAndTarget() {
        persist("CAMPAIGN", 5L, 100L, "PENDING");
        assertThat(repo.findByTypeAndTargetId("CAMPAIGN", 5L)).isPresent();
    }

    @Test @DisplayName("findByTypeAndTargetId_notFound_empty")
    void byTypeAndTarget_empty() {
        assertThat(repo.findByTypeAndTargetId("CAMPAIGN", 999L)).isEmpty();
    }

    @Test @DisplayName("findByStaffIdAndStatus_filters")
    void byStaffStatus() {
        persist("CAMPAIGN", 1L, 100L, "PENDING");
        persist("CAMPAIGN", 2L, 100L, "COMPLETED");
        assertThat(repo.findByStaffIdAndStatus(100L, "PENDING")).hasSize(1);
    }

    @Test @DisplayName("save_persists")
    void save() {
        ApprovalTask t = persist("CAMPAIGN", 1L, 100L, "PENDING");
        assertThat(repo.findById(t.getId())).isPresent();
    }
}
