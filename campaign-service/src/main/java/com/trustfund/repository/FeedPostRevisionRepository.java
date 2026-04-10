package com.trustfund.repository;

import com.trustfund.model.FeedPostRevision;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FeedPostRevisionRepository extends JpaRepository<FeedPostRevision, Long> {

    Page<FeedPostRevision> findByPostIdOrderByRevisionNoDesc(Long postId, Pageable pageable);

    @Query("SELECT COALESCE(MAX(r.revisionNo), 0) FROM FeedPostRevision r WHERE r.postId = :postId")
    int findMaxRevisionNoByPostId(@Param("postId") Long postId);

    Optional<FeedPostRevision> findByPostIdAndId(Long postId, Long id);

    boolean existsByPostId(Long postId);

    FeedPostRevision findTopByPostIdOrderByRevisionNoDesc(Long postId);
}
