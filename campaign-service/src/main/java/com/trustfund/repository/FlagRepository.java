package com.trustfund.repository;

import com.trustfund.model.Flag;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public interface FlagRepository extends JpaRepository<Flag, Long> {
    Page<Flag> findByStatus(String status, Pageable pageable);

    Page<Flag> findByPostId(Long postId, Pageable pageable);

    Page<Flag> findByCampaignId(Long campaignId, Pageable pageable);

    Page<Flag> findByUserId(Long userId, Pageable pageable);

    /** Duplicate guard: check if the user already flagged this campaign */
    boolean existsByUserIdAndCampaignId(Long userId, Long campaignId);

    /** Duplicate guard: check if the user already flagged this post */
    boolean existsByUserIdAndPostId(Long userId, Long postId);

    @Query("""
        SELECT f.postId, COUNT(f)
        FROM Flag f
        WHERE f.postId IN :postIds
          AND f.status = :status
        GROUP BY f.postId
    """)
    List<Object[]> countPendingFlagsByPostIds(
            @Param("postIds") List<Long> postIds,
            @Param("status") String status
    );
}
