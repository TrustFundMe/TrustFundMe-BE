package com.trustfund.repository;

import com.trustfund.model.BankAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {
    List<BankAccount> findByUser_Id(Long userId);

    org.springframework.data.domain.Page<BankAccount> findByStatus(String status,
            org.springframework.data.domain.Pageable pageable);

    List<BankAccount> findByAccountNumber(String accountNumber);

    Optional<BankAccount> findByAccountNumberAndBankCode(String accountNumber, String bankCode);

    Optional<BankAccount> findByCampaignId(Long campaignId);

    boolean existsByAccountNumberAndBankCodeAndUserIdNot(String accountNumber, String bankCode, Long userId);
}
