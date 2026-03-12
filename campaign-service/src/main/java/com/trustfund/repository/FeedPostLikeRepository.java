package com.trustfund.repository;

import com.trustfund.model.FeedPostLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FeedPostLikeRepository extends JpaRepository<FeedPostLike, Long> {
    Optional<FeedPostLike> findByPostIdAndUserId(Long postId, Long userId);
    void deleteByPostIdAndUserId(Long postId, Long userId);
    boolean existsByPostIdAndUserId(Long postId, Long userId);
    void deleteByPostId(Long postId);
    int countByPostId(Long postId);
}
