package com.trustfund.repository;

import com.trustfund.model.AppointmentSchedule;
import com.trustfund.model.AppointmentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
class AppointmentScheduleRepositoryTest {

    @Autowired private TestEntityManager em;
    @Autowired private AppointmentScheduleRepository repo;

    private AppointmentSchedule persist(Long donor, Long staff, AppointmentStatus s) {
        return em.persistAndFlush(AppointmentSchedule.builder()
                .donorId(donor).staffId(staff)
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(1))
                .status(s).location("HN").purpose("p").build());
    }

    @Test @DisplayName("findByDonorId_returnsList")
    void byDonor() {
        persist(1L, 100L, AppointmentStatus.PENDING);
        assertThat(repo.findByDonorId(1L)).hasSize(1);
    }

    @Test @DisplayName("findByDonorId_empty")
    void byDonor_empty() { assertThat(repo.findByDonorId(999L)).isEmpty(); }

    @Test @DisplayName("findByStaffId_returnsList")
    void byStaff() {
        persist(1L, 100L, AppointmentStatus.PENDING);
        persist(2L, 100L, AppointmentStatus.PENDING);
        assertThat(repo.findByStaffId(100L)).hasSize(2);
    }

    @Test @DisplayName("save_thenFind_ok")
    void save() {
        AppointmentSchedule a = persist(1L, 100L, AppointmentStatus.PENDING);
        assertThat(repo.findById(a.getId())).isPresent();
    }

    @Test @DisplayName("delete_removes")
    void delete() {
        AppointmentSchedule a = persist(1L, 100L, AppointmentStatus.PENDING);
        repo.deleteById(a.getId());
        em.flush();
        assertThat(repo.findById(a.getId())).isEmpty();
    }

    @Test @DisplayName("findAll_returnsAll")
    void findAll() {
        persist(1L, 100L, AppointmentStatus.PENDING);
        persist(2L, 100L, AppointmentStatus.PENDING);
        assertThat(repo.findAll()).hasSize(2);
    }
}
