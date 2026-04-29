package com.trustfund.repository;

import com.trustfund.model.ExpenditureCatology;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExpenditureCatologyRepository extends JpaRepository<ExpenditureCatology, Long> {
    @Query("SELECT ec FROM ExpenditureCatology ec WHERE ec.expenditure.id = :expenditureId")
    List<ExpenditureCatology> findByExpenditureId(@Param("expenditureId") Long expenditureId);
}
