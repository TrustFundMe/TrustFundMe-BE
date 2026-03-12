package com.trustfund.repository;

import com.trustfund.model.FeedPostComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FeedPostCommentRepository extends JpaRepository<FeedPostComment, Long> {
    Page<FeedPostComment> findByPostIdOrderByCreatedAtDesc(Long postId, Pageable pageable);
    Page<FeedPostComment> findByPostIdAndParentCommentIdIsNullOrderByCreatedAtDesc(Long postId, Pageable pageable);
    java.util.List<FeedPostComment> findByParentCommentIdOrderByCreatedAtAsc(Long parentCommentId);
    void deleteByPostId(Long postId);
    int countByPostId(Long postId);
}
