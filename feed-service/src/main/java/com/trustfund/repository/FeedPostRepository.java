package com.trustfund.repository;

import com.trustfund.model.FeedPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FeedPostRepository extends JpaRepository<FeedPost, Long> {

    @Query("""
            SELECT p FROM FeedPost p
            WHERE p.status = 'ACTIVE'
              AND (
                p.visibility IN ('PUBLIC', 'FOLLOWERS')
                OR (p.visibility = 'PRIVATE' AND p.authorId = :currentUserId)
              )
            """)
    Page<FeedPost> findVisibleActivePosts(@Param("currentUserId") Long currentUserId, Pageable pageable);
}

