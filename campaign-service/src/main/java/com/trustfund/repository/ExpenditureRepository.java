package com.trustfund.repository;

import com.trustfund.model.Expenditure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExpenditureRepository extends JpaRepository<Expenditure, Long> {
    List<Expenditure> findByCampaignId(Long campaignId);
}
