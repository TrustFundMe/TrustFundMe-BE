package com.trustfund.audit.repository;

import com.trustfund.audit.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    
    Optional<AuditLog> findTopByOrderByCreatedAtDesc();
    
    Optional<AuditLog> findFirstByCreatedAtBeforeOrderByCreatedAtDesc(java.time.LocalDateTime createdAt);
    
    Page<AuditLog> findByEntityTypeAndEntityId(String entityType, Long entityId, Pageable pageable);
    
    List<AuditLog> findByAuditHash(String auditHash);
    
    @Query("SELECT a FROM AuditLog a WHERE " +
           "STR(a.entityId) LIKE %:query% OR " +
           "a.entityType LIKE %:query% OR " +
           "a.auditHash LIKE %:query% OR " +
           "a.actorName LIKE %:query%")
    Page<AuditLog> search(@Param("query") String query, Pageable pageable);
}
