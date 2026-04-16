package com.trustfund.repository;

import com.trustfund.model.ExpenditureTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ExpenditureTransactionRepository extends JpaRepository<ExpenditureTransaction, Long> {
    List<ExpenditureTransaction> findByExpenditureId(Long expenditureId);

    List<ExpenditureTransaction> findByExpenditureIdAndTypeAndStatus(Long expenditureId, String type, String status);

    List<ExpenditureTransaction> findByTypeAndStatus(String type, String status);

    Page<ExpenditureTransaction> findByTypeAndStatus(String type, String status, Pageable pageable);

    @Query("SELECT SUM(t.amount) FROM ExpenditureTransaction t, Campaign c WHERE t.expenditure.campaignId = c.id AND c.fundOwnerId = :fundOwnerId AND t.type = 'PAYOUT' AND t.status = 'COMPLETED'")
    BigDecimal sumCompletedPayoutsByFundOwnerId(@Param("fundOwnerId") Long fundOwnerId);
}
