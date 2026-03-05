package com.trustfund.repository;

import com.trustfund.model.ExpenditureItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExpenditureItemRepository extends JpaRepository<ExpenditureItem, Long> {
    List<ExpenditureItem> findByExpenditureId(Long expenditureId);

    @Modifying
    void deleteByExpenditureId(Long expenditureId);

    @org.springframework.data.jpa.repository.Query("SELECT ei FROM ExpenditureItem ei JOIN ei.expenditure e WHERE e.campaignId = :campaignId")
    List<ExpenditureItem> findByExpenditureCampaignId(
            @org.springframework.data.repository.query.Param("campaignId") Long campaignId);

}
