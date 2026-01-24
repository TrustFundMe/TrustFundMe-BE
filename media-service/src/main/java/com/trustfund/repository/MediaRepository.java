package com.trustfund.repository;

import com.trustfund.model.Media;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MediaRepository extends JpaRepository<Media, Long> {
    List<Media> findByPostId(Long postId);

    List<Media> findByCampaignId(Long campaignId);
}
