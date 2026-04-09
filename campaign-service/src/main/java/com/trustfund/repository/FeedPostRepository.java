package com.trustfund.repository;

import com.trustfund.model.FeedPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FeedPostRepository extends JpaRepository<FeedPost, Long> {

  @Query("""
      SELECT p FROM FeedPost p
      WHERE p.status IN ('PUBLISHED')
        AND p.isLocked = false
        AND (
          p.visibility IN ('PUBLIC', 'FOLLOWERS')
          OR (p.visibility = 'PRIVATE' AND p.authorId = :currentUserId)
        )
      ORDER BY p.isPinned DESC, p.updatedAt DESC
      """)
  Page<FeedPost> findVisibleActivePosts(@Param("currentUserId") Long currentUserId, Pageable pageable);

  @Query("""
      SELECT p FROM FeedPost p
      WHERE p.authorId = :authorId
        AND (:status IS NULL OR UPPER(p.status) = UPPER(:status))
      ORDER BY p.updatedAt DESC
      """)
  Page<FeedPost> findMyPosts(@Param("authorId") Long authorId, @Param("status") String status, Pageable pageable);

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

  List<FeedPost> findByTargetIdAndTargetTypeOrderByCreatedAtDesc(Long targetId, String targetType);
}
