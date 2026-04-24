package com.trustfund.repository;

import com.trustfund.model.User;
import com.trustfund.model.UserKYC;
import com.trustfund.model.enums.KYCStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
class UserKYCRepositoryTest {

    @Autowired private TestEntityManager em;
    @Autowired private UserKYCRepository kycRepo;

    private User persistUser(String email) {
        User u = User.builder().email(email).password("p").fullName("F")
                .role(User.Role.USER).isActive(true).verified(true).build();
        return em.persistAndFlush(u);
    }

    private UserKYC persistKyc(User u, String idNumber, KYCStatus status) {
        UserKYC k = UserKYC.builder().user(u).idType("CCCD").idNumber(idNumber)
                .issueDate(LocalDate.now().minusYears(1)).expiryDate(LocalDate.now().plusYears(5))
                .issuePlace("HN").idImageFront("f").idImageBack("b").selfieImage("s").status(status).build();
        return em.persistAndFlush(k);
    }

    @Test @DisplayName("findByUserId_present")
    void findByUserId_ok() {
        User u = persistUser("a@e.com");
        persistKyc(u, "111", KYCStatus.PENDING);
        Optional<UserKYC> r = kycRepo.findByUserId(u.getId());
        assertThat(r).isPresent();
    }

    @Test @DisplayName("findByUserId_absent_returnsEmpty")
    void findByUserId_empty() {
        assertThat(kycRepo.findByUserId(999L)).isEmpty();
    }

    @Test @DisplayName("existsByUserId_true")
    void existsByUserId() {
        User u = persistUser("a@e.com");
        persistKyc(u, "222", KYCStatus.PENDING);
        assertThat(kycRepo.existsByUserId(u.getId())).isTrue();
    }

    @Test @DisplayName("existsByIdNumber_true")
    void existsByIdNumber() {
        User u = persistUser("b@e.com");
        persistKyc(u, "333", KYCStatus.APPROVED);
        assertThat(kycRepo.existsByIdNumber("333")).isTrue();
    }

    @Test @DisplayName("findFirstByIdNumber_returnsFirst")
    void findFirstByIdNumber() {
        User u = persistUser("c@e.com");
        persistKyc(u, "444", KYCStatus.PENDING);
        assertThat(kycRepo.findFirstByIdNumber("444")).isPresent();
    }

    @Test @DisplayName("countByStatus_returnsCount")
    void count() {
        User u1 = persistUser("d1@e.com"); persistKyc(u1, "555", KYCStatus.APPROVED);
        User u2 = persistUser("d2@e.com"); persistKyc(u2, "666", KYCStatus.APPROVED);
        User u3 = persistUser("d3@e.com"); persistKyc(u3, "777", KYCStatus.PENDING);
        assertThat(kycRepo.countByStatus(KYCStatus.APPROVED)).isEqualTo(2L);
    }

    @Test @DisplayName("findByStatus_paginated")
    void findByStatus() {
        User u = persistUser("e@e.com");
        persistKyc(u, "888", KYCStatus.PENDING);
        assertThat(kycRepo.findByStatus(KYCStatus.PENDING, org.springframework.data.domain.PageRequest.of(0, 10))
                .getTotalElements()).isEqualTo(1L);
    }

    @Test @DisplayName("save_persistsKyc")
    void save() {
        User u = persistUser("f@e.com");
        UserKYC k = persistKyc(u, "999", KYCStatus.PENDING);
        assertThat(kycRepo.findById(k.getId())).isPresent();
    }
}
