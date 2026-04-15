package com.trustfund.repository;

import com.trustfund.model.CampaignCommitment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CampaignCommitmentRepository extends JpaRepository<CampaignCommitment, Long> {
    Optional<CampaignCommitment> findByCampaignId(Long campaignId);
    boolean existsByCampaignIdAndStatus(Long campaignId, String status);
}
