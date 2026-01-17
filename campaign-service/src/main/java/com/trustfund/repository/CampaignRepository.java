package com.trustfund.repository;

import com.trustfund.model.Campaign;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CampaignRepository extends JpaRepository<Campaign, Long> {

    List<Campaign> findByFundOwnerId(Long fundOwnerId);

    List<Campaign> findByStatus(String status);
}
