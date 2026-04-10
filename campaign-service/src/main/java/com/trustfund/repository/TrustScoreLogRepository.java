package com.trustfund.repository;

import com.trustfund.model.TrustScoreLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TrustScoreLogRepository extends JpaRepository<TrustScoreLog, Long> {

    Page<TrustScoreLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<TrustScoreLog> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @Query("SELECT l FROM TrustScoreLog l WHERE " +
           "(:userId IS NULL OR l.userId = :userId) AND " +
           "(:ruleKey IS NULL OR l.ruleKey = :ruleKey) AND " +
           "(:startDate IS NULL OR l.createdAt >= :startDate) AND " +
           "(:endDate IS NULL OR l.createdAt <= :endDate) " +
           "ORDER BY l.createdAt DESC")
    Page<TrustScoreLog> findByFilters(
            @Param("userId") Long userId,
            @Param("ruleKey") String ruleKey,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    boolean existsByUserIdAndRuleKeyAndCreatedAtAfter(Long userId, String ruleKey, LocalDateTime after);

    @Query("SELECT l FROM TrustScoreLog l WHERE " +
           "l.userId = :userId AND " +
           "l.ruleKey = :ruleKey AND " +
           "l.referenceId = :referenceId")
    List<TrustScoreLog> findByUserIdAndRuleKeyAndReference(
            @Param("userId") Long userId,
            @Param("ruleKey") String ruleKey,
            @Param("referenceId") Long referenceId);
}
