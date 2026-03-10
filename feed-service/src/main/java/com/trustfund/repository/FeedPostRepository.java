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
      ORDER BY p.isPinned DESC, p.updatedAt DESC
      """)
  Page<FeedPost> findVisibleActivePosts(@Param("currentUserId") Long currentUserId, Pageable pageable);

  @Query("""
      SELECT p FROM FeedPost p
      WHERE p.status IN ('ACTIVE', 'PUBLISHED')
        AND p.campaignId = :campaignId
        AND (
          p.visibility IN ('PUBLIC', 'FOLLOWERS')
          OR (p.visibility = 'PRIVATE' AND p.authorId = :currentUserId)
        )
      ORDER BY p.isPinned DESC, p.createdAt DESC
      """)
  Page<FeedPost> findVisibleActivePostsByCampaignId(
      @Param("campaignId") Long campaignId,
      @Param("currentUserId") Long currentUserId,
      Pageable pageable);

  Long countByCategoryAndStatus(String category, String status);

  Page<FeedPost> findByCategoryAndStatusOrderByCreatedAtDesc(String category, String status, Pageable pageable);

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

  @Query("""
      SELECT p FROM FeedPost p
      WHERE (:status IS NULL OR p.status = :status)
        AND (:type IS NULL OR p.type = :type)
        AND (:keyword IS NULL
             OR LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
             OR LOWER(p.content) LIKE LOWER(CONCAT('%', :keyword, '%')))
      ORDER BY p.isPinned DESC, p.createdAt DESC
      """)
  Page<FeedPost> findAllWithFilters(
      @Param("status") String status,
      @Param("type") String type,
      @Param("keyword") String keyword,
      Pageable pageable);
}
