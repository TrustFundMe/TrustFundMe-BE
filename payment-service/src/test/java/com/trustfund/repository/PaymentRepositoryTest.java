package com.trustfund.repository;

import com.trustfund.model.Payment;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
class PaymentRepositoryTest {

    @Autowired private TestEntityManager em;
    @Autowired private PaymentRepository repo;

    private Payment persist(String linkId, String status) {
        return em.persistAndFlush(Payment.builder()
                .description("d").amount(BigDecimal.TEN).orderCode(System.nanoTime())
                .paymentLinkId(linkId).status(status).build());
    }

    @Test @DisplayName("findByPaymentLinkId_returnsPayment")
    void byLinkId() {
        persist("link-1", "PENDING");
        assertThat(repo.findByPaymentLinkId("link-1")).isPresent();
    }

    @Test @DisplayName("findByPaymentLinkId_notFound_empty")
    void byLinkId_empty() {
        assertThat(repo.findByPaymentLinkId("nope")).isEmpty();
    }

    @Test @DisplayName("findAllByStatusAndCreatedAtBefore")
    void byStatusBefore() {
        persist("link-old", "PENDING");
        assertThat(repo.findAllByStatusAndCreatedAtBefore("PENDING",
                LocalDateTime.now().plusHours(1))).hasSize(1);
    }

    @Test @DisplayName("findAllByStatusAndCreatedAtBefore_filtersByDate")
    void byStatusBefore_filterDate() {
        persist("link-1", "PENDING");
        assertThat(repo.findAllByStatusAndCreatedAtBefore("PENDING",
                LocalDateTime.now().minusDays(1))).isEmpty();
    }

    @Test @DisplayName("save_persistsAndFinds")
    void save() {
        Payment p = persist("link-1", "PENDING");
        assertThat(repo.findById(p.getId())).isPresent();
    }

    @Test @DisplayName("delete_removes")
    void delete() {
        Payment p = persist("link-1", "PENDING");
        repo.deleteById(p.getId());
        em.flush();
        assertThat(repo.findById(p.getId())).isEmpty();
    }
}
