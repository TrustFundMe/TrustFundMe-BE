package com.trustfund.repository;

import com.trustfund.model.ExpenditureEvidence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExpenditureEvidenceRepository extends JpaRepository<ExpenditureEvidence, Long> {
    List<ExpenditureEvidence> findByExpenditureId(Long expenditureId);

    Optional<ExpenditureEvidence> findByCassoTransactionId(String cassoTransactionId);

    List<ExpenditureEvidence> findByStatusAndDueAtBefore(String status, java.time.LocalDateTime now);

    /**
     * Find evidences by campaign IDs and statuses - replaces
     * findAll().stream().filter() bottleneck
     */
    List<ExpenditureEvidence> findByCampaignIdInAndStatusIn(List<Long> campaignIds, List<String> statuses);

    /**
     * Find orphan evidences (not assigned to any expenditure) for a specific
     * campaign
     */
    @org.springframework.data.jpa.repository.Query("SELECT e FROM ExpenditureEvidence e WHERE e.campaignId = :campaignId AND e.expenditure IS NULL ORDER BY e.createdAt DESC")
    List<ExpenditureEvidence> findOrphanByCampaignId(
            @org.springframework.data.repository.query.Param("campaignId") Long campaignId);
}
