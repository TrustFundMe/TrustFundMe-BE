package com.trustfund.repository;

import com.trustfund.model.ExpenditureItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExpenditureItemRepository extends JpaRepository<ExpenditureItem, Long> {
        @Query("SELECT ei FROM ExpenditureItem ei WHERE ei.expenditure.id = :expenditureId")
        List<ExpenditureItem> findByExpenditureId(@Param("expenditureId") Long expenditureId);

        @Query("SELECT ei FROM ExpenditureItem ei WHERE ei.expenditure.campaignId = :campaignId")
        List<ExpenditureItem> findByExpenditureCampaignId(@Param("campaignId") Long campaignId);
}
