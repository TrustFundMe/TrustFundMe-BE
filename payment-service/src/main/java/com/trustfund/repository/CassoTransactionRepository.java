package com.trustfund.repository;

import com.trustfund.model.CassoTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CassoTransactionRepository extends JpaRepository<CassoTransaction, Long> {
    boolean existsByTid(String tid);
    List<CassoTransaction> findAllByOrderByCreatedAtDesc();
    List<CassoTransaction> findByAccountNumberOrderByCreatedAtDesc(String accountNumber);
    List<CassoTransaction> findByAccountNumberAndBankAbbreviationOrderByCreatedAtDesc(String accountNumber, String bankAbbreviation);
    List<CassoTransaction> findByCampaignIdOrderByCreatedAtDesc(Long campaignId);
    List<CassoTransaction> findByAccountNumberAndCreatedAtAfterOrderByCreatedAtDesc(String accountNumber, java.time.LocalDateTime since);
}
