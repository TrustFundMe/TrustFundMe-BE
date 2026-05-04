package com.trustfund.repository;

import com.trustfund.model.Expenditure;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExpenditureRepository extends JpaRepository<Expenditure, Long> {
    List<Expenditure> findByCampaignId(Long campaignId);

    List<Expenditure> findByCampaignIdOrderByCreatedAtDesc(Long campaignId);

    List<Expenditure> findByStatusOrderByCreatedAtDesc(String status);

    List<Expenditure> findByCampaignIdInOrderByCreatedAtDesc(java.util.List<Long> campaignIds);

    @Query("SELECT COALESCE(SUM(e.totalAmount), 0) FROM Expenditure e WHERE e.campaignId IN :campaignIds AND e.status = 'DISBURSED'")
    java.math.BigDecimal sumTotalAmountByCampaignIds(
            @Param("campaignIds") java.util.List<Long> campaignIds);

    /**
     * Fetch Expenditure with transactions and evidences eagerly loaded to avoid N+1 queries.
     * Uses @EntityGraph instead of JOIN FETCH to safely handle multiple bag collections.
     */
    @EntityGraph(attributePaths = {"transactions", "evidences"})
    @Query("SELECT e FROM Expenditure e WHERE e.campaignId = :campaignId")
    List<Expenditure> findByCampaignIdWithRelations(@Param("campaignId") Long campaignId);
}
