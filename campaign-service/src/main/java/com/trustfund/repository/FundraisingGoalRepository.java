package com.trustfund.repository;

import com.trustfund.model.FundraisingGoal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FundraisingGoalRepository extends JpaRepository<FundraisingGoal, Long> {

    List<FundraisingGoal> findByCampaignId(Long campaignId);

    List<FundraisingGoal> findByIsActive(Boolean isActive);

    List<FundraisingGoal> findByCampaignIdAndIsActive(Long campaignId, Boolean isActive);
}
