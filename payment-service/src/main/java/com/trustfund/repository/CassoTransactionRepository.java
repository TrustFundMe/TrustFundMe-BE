package com.trustfund.repository;

import com.trustfund.model.CassoTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.List;

@Repository
public interface CassoTransactionRepository extends JpaRepository<CassoTransaction, Long> {
    boolean existsByTid(String tid);
    List<CassoTransaction> findAllByOrderByCreatedAtDesc();
    List<CassoTransaction> findByAccountNumberOrderByCreatedAtDesc(String accountNumber);
    List<CassoTransaction> findByAccountNumberAndBankAbbreviationOrderByCreatedAtDesc(String accountNumber, String bankAbbreviation);
    List<CassoTransaction> findByCampaignIdOrderByCreatedAtDesc(Long campaignId);
    List<CassoTransaction> findByAccountNumberAndCreatedAtAfterOrderByCreatedAtDesc(String accountNumber, java.time.LocalDateTime since);

    @Query("SELECT COALESCE(SUM(c.amount), 0) FROM CassoTransaction c WHERE c.campaignId IN :campaignIds AND c.amount > 0")
    BigDecimal sumPositiveAmountByCampaignIds(@Param("campaignIds") List<Long> campaignIds);

    @Query("SELECT COALESCE(SUM(c.amount), 0) FROM CassoTransaction c WHERE c.campaignId IN :campaignIds AND c.amount < 0")
    BigDecimal sumNegativeAmountByCampaignIds(@Param("campaignIds") List<Long> campaignIds);
}
