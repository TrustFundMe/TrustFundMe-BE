package com.trustfund.repository;

import com.trustfund.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Optional<AuditLog> findTopByOrderByCreatedAtDesc();

    Optional<AuditLog> findFirstByCreatedAtBeforeOrderByCreatedAtDesc(LocalDateTime createdAt);
    
    Optional<AuditLog> findFirstByIdLessThanOrderByIdDesc(Long id);

    Page<AuditLog> findByEntityTypeAndEntityId(String entityType, Long entityId, Pageable pageable);

    List<AuditLog> findByAuditHash(String auditHash);

    @Query("SELECT a FROM AuditLog a WHERE " +
            "STR(a.entityId) LIKE %:query% OR " +
            "a.entityType LIKE %:query% OR " +
            "a.auditHash LIKE %:query% OR " +
            "a.actorName LIKE %:query%")
    Page<AuditLog> search(@Param("query") String query, Pageable pageable);

    Page<AuditLog> findByActorId(Long actorId, Pageable pageable);

    Page<AuditLog> findByActorIdAndEntityTypeNot(Long actorId, String entityType, Pageable pageable);

    @Query("SELECT a FROM AuditLog a WHERE a.actorId = :actorId OR (a.entityType IN ('DONATION_TRANSACTION', 'CAMPAIGN', 'EVIDENCE_SUBMISSION', 'EXPENDITURE_WITHDRAWAL', 'EXPENDITURE_REVIEW', 'EVIDENCE_REVIEW', 'CAMPAIGN_COMMITMENT') AND a.entityId IN :campaignIds)")
    Page<AuditLog> findByReconciliationContext(@Param("actorId") Long actorId,
            @Param("campaignIds") List<Long> campaignIds, Pageable pageable);
}
