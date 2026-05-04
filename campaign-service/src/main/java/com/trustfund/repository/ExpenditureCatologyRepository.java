package com.trustfund.repository;

import com.trustfund.model.ExpenditureCatology;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExpenditureCatologyRepository extends JpaRepository<ExpenditureCatology, Long> {
    @Query("SELECT ec FROM ExpenditureCatology ec WHERE ec.expenditure.id = :expenditureId")
    List<ExpenditureCatology> findByExpenditureId(@Param("expenditureId") Long expenditureId);

    @Modifying
    @Query("DELETE FROM ExpenditureCatology ec WHERE ec.expenditure.id = :expenditureId")
    void deleteByExpenditureId(@Param("expenditureId") Long expenditureId);

    /**
     * Fetch ExpenditureCatology with items eagerly loaded to avoid N+1 queries.
     */
    @EntityGraph(attributePaths = {"items"})
    @Query("SELECT ec FROM ExpenditureCatology ec WHERE ec.expenditure.id = :expenditureId")
    List<ExpenditureCatology> findByExpenditureIdWithItems(@Param("expenditureId") Long expenditureId);
}
