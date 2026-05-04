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
     * Find evidences by campaign IDs and statuses - replaces findAll().stream().filter() bottleneck
     */
    List<ExpenditureEvidence> findByCampaignIdInAndStatusIn(List<Long> campaignIds, List<String> statuses);
}
