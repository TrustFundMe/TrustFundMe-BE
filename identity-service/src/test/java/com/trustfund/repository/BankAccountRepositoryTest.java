package com.trustfund.repository;

import com.trustfund.model.BankAccount;
import com.trustfund.model.User;
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
class BankAccountRepositoryTest {

    @Autowired private TestEntityManager em;
    @Autowired private BankAccountRepository repo;

    private User persistUser(String email) {
        return em.persistAndFlush(User.builder().email(email).password("p").fullName("F")
                .role(User.Role.USER).isActive(true).verified(true).build());
    }

    private BankAccount persistBank(User u, String acct, String code, String status) {
        return em.persistAndFlush(BankAccount.builder().user(u).accountNumber(acct)
                .bankCode(code).accountHolderName("A").isVerified(true).status(status).build());
    }

    @Test @DisplayName("findByUser_Id_returnsList")
    void findByUserId() {
        User u = persistUser("x1@e.com");
        persistBank(u, "111", "VCB", "APPROVED");
        List<BankAccount> r = repo.findByUser_Id(u.getId());
        assertThat(r).hasSize(1);
    }

    @Test @DisplayName("findByUser_Id_empty")
    void findByUserId_empty() {
        assertThat(repo.findByUser_Id(999L)).isEmpty();
    }

    @Test @DisplayName("existsByAccountNumberAndBankCodeAndUserIdNot_trueForOther")
    void exists_otherUser() {
        User u1 = persistUser("u1@e.com");
        persistBank(u1, "123", "VCB", "APPROVED");
        User u2 = persistUser("u2@e.com");
        assertThat(repo.existsByAccountNumberAndBankCodeAndUserIdNot("123", "VCB", u2.getId())).isTrue();
    }

    @Test @DisplayName("existsByAccountNumberAndBankCodeAndUserIdNot_falseForSameOwner")
    void exists_sameOwner() {
        User u = persistUser("u@e.com");
        persistBank(u, "123", "VCB", "APPROVED");
        assertThat(repo.existsByAccountNumberAndBankCodeAndUserIdNot("123", "VCB", u.getId())).isFalse();
    }

    @Test @DisplayName("findByStatus_paginated")
    void findByStatus() {
        User u = persistUser("p@e.com");
        persistBank(u, "111", "VCB", "PENDING");
        assertThat(repo.findByStatus("PENDING", PageRequest.of(0, 10)).getTotalElements()).isEqualTo(1);
    }

    @Test @DisplayName("save_thenFind_returnsAccount")
    void save() {
        User u = persistUser("s@e.com");
        BankAccount b = persistBank(u, "111", "VCB", "APPROVED");
        assertThat(repo.findById(b.getId())).isPresent();
    }

    @Test @DisplayName("delete_removesAccount")
    void delete() {
        User u = persistUser("d@e.com");
        BankAccount b = persistBank(u, "111", "VCB", "APPROVED");
        repo.deleteById(b.getId());
        em.flush();
        assertThat(repo.findById(b.getId())).isEmpty();
    }
}
