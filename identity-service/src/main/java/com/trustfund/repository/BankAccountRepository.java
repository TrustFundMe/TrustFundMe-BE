package com.trustfund.repository;

import com.trustfund.model.BankAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {
    List<BankAccount> findByUser_Id(Long userId);

    org.springframework.data.domain.Page<BankAccount> findByStatus(String status,
            org.springframework.data.domain.Pageable pageable);

    boolean existsByAccountNumberAndBankCodeAndUserIdNot(String accountNumber, String bankCode, Long userId);
}
