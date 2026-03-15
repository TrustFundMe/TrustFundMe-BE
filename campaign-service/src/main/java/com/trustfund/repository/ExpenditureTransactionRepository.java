package com.trustfund.repository;

import com.trustfund.model.ExpenditureTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExpenditureTransactionRepository extends JpaRepository<ExpenditureTransaction, Long> {
    List<ExpenditureTransaction> findByExpenditureId(Long expenditureId);
    List<ExpenditureTransaction> findByExpenditureIdAndTypeAndStatus(Long expenditureId, String type, String status);
}
