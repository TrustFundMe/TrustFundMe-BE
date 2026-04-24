package com.trustfund.repository;

import com.trustfund.model.OtpToken;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
class OtpTokenRepositoryTest {

    @Autowired private TestEntityManager em;
    @Autowired private OtpTokenRepository repo;

    private OtpToken persist(String email, String otp, LocalDateTime expiresAt, Boolean used) {
        return em.persistAndFlush(OtpToken.builder()
                .email(email).otp(otp).expiresAt(expiresAt).used(used).build());
    }

    @Test @DisplayName("findByEmailAndOtpAndUsedFalse_returns")
    void findValid() {
        persist("u@e.com", "123456", LocalDateTime.now().plusMinutes(5), false);
        assertThat(repo.findByEmailAndOtpAndUsedFalse("u@e.com", "123456")).isPresent();
    }

    @Test @DisplayName("findByEmailAndOtpAndUsedFalse_usedIsExcluded")
    void findValid_used() {
        persist("u@e.com", "123456", LocalDateTime.now().plusMinutes(5), true);
        assertThat(repo.findByEmailAndOtpAndUsedFalse("u@e.com", "123456")).isEmpty();
    }

    @Test @DisplayName("findByEmailAndOtpAndUsedFalse_wrongOtp")
    void findValid_wrongOtp() {
        persist("u@e.com", "123456", LocalDateTime.now().plusMinutes(5), false);
        assertThat(repo.findByEmailAndOtpAndUsedFalse("u@e.com", "000000")).isEmpty();
    }

    @Test @Transactional @DisplayName("markAsUsed_setsUsedTrue")
    void markUsed() {
        OtpToken t = persist("u@e.com", "123456", LocalDateTime.now().plusMinutes(5), false);
        repo.markAsUsed("u@e.com", "123456");
        em.flush(); em.clear();
        assertThat(repo.findById(t.getId()).get().getUsed()).isTrue();
    }

    @Test @Transactional @DisplayName("deleteExpiredOtp_removesExpired")
    void deleteExpired() {
        persist("u@e.com", "123456", LocalDateTime.now().minusHours(1), false);
        persist("v@e.com", "111111", LocalDateTime.now().plusHours(1), false);
        repo.deleteExpiredOtp(LocalDateTime.now());
        em.flush(); em.clear();
        assertThat(repo.count()).isEqualTo(1L);
    }

    @Test @DisplayName("save_persists")
    void save() {
        OtpToken t = persist("u@e.com", "111", LocalDateTime.now().plusMinutes(5), false);
        assertThat(repo.findById(t.getId())).isPresent();
    }
}
