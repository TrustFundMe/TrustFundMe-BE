package com.trustfund.repository;

import com.trustfund.model.FeedPostCommentLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FeedPostCommentLikeRepository extends JpaRepository<FeedPostCommentLike, Long> {
    Optional<FeedPostCommentLike> findByCommentIdAndUserId(Long commentId, Long userId);
    boolean existsByCommentIdAndUserId(Long commentId, Long userId);
    void deleteByCommentId(Long commentId);
}
