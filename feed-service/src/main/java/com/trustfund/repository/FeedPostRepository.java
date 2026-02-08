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
      WHERE p.status IN ('ACTIVE', 'PUBLISHED')
        AND (
          p.visibility IN ('PUBLIC', 'FOLLOWERS')
          OR (p.visibility = 'PRIVATE' AND p.authorId = :currentUserId)
        )
      """)
  Page<FeedPost> findVisibleActivePosts(@Param("currentUserId") Long currentUserId, Pageable pageable);

  Long countByCategoryIdAndStatus(Long categoryId, String status);

  Page<FeedPost> findByCategoryIdAndStatusOrderByCreatedAtDesc(Long categoryId, String status, Pageable pageable);

  @Query("""
      SELECT p FROM FeedPost p
      WHERE p.parentPostId = :parentPostId
      ORDER BY p.createdAt ASC
      """)
  java.util.List<FeedPost> findRepliesByParentPostId(@Param("parentPostId") Long parentPostId);
  @org.springframework.data.jpa.repository.Modifying
  @org.springframework.transaction.annotation.Transactional
  @Query("UPDATE FeedPost p SET p.viewCount = p.viewCount + 1 WHERE p.id = :id")
  void incrementViewCount(@Param("id") Long id);
}
