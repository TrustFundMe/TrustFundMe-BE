package com.trustfund.repository;

import com.trustfund.model.Expenditure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExpenditureRepository extends JpaRepository<Expenditure, Long> {
    List<Expenditure> findByCampaignId(Long campaignId);

    List<Expenditure> findByCampaignIdOrderByCreatedAtDesc(Long campaignId);

    List<Expenditure> findByStatusOrderByCreatedAtDesc(String status);

    List<Expenditure> findByCampaignIdInOrderByCreatedAtDesc(java.util.List<Long> campaignIds);

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(e.totalAmount), 0) FROM Expenditure e WHERE e.campaignId IN :campaignIds AND e.status = 'DISBURSED'")
    java.math.BigDecimal sumTotalAmountByCampaignIds(
            @org.springframework.data.repository.query.Param("campaignIds") java.util.List<Long> campaignIds);
}
