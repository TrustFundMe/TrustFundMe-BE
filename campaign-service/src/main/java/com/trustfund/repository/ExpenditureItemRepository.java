package com.trustfund.repository;

import com.trustfund.model.ExpenditureItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExpenditureItemRepository extends JpaRepository<ExpenditureItem, Long> {
    List<ExpenditureItem> findByExpenditureId(Long expenditureId);
}
