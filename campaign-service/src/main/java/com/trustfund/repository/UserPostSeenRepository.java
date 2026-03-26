package com.trustfund.repository;

import com.trustfund.model.UserPostSeen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserPostSeenRepository extends JpaRepository<UserPostSeen, Long> {
    Optional<UserPostSeen> findByUserIdAndPostId(Long userId, Long postId);
    boolean existsByUserIdAndPostId(Long userId, Long postId);
    List<UserPostSeen> findByUserId(Long userId);
    List<UserPostSeen> findByUserIdAndPostIdIn(Long userId, List<Long> postIds);
    void deleteByUserIdAndPostId(Long userId, Long postId);
}
