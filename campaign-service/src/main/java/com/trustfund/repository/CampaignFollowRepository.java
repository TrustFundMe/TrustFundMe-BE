package com.trustfund.repository;

import com.trustfund.model.CampaignFollow;
import com.trustfund.model.CampaignFollowId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CampaignFollowRepository extends JpaRepository<CampaignFollow, CampaignFollowId> {

    boolean existsById_CampaignIdAndId_UserId(Long campaignId, Long userId);

    long countById_CampaignId(Long campaignId);

    List<CampaignFollow> findById_CampaignIdOrderByFollowedAtDesc(Long campaignId);

    List<CampaignFollow> findById_UserIdOrderByFollowedAtDesc(Long userId);

    @Query("select cf.id.campaignId from CampaignFollow cf where cf.id.userId = :userId order by cf.followedAt desc")
    List<Long> findCampaignIdsFollowedByUser(Long userId);
}

