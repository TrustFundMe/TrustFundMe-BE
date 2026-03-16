package com.trustfund.repository;

import com.trustfund.model.ApprovalTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApprovalTaskRepository extends JpaRepository<ApprovalTask, Long> {
    List<ApprovalTask> findByStaffId(Long staffId);
    List<ApprovalTask> findByStatus(String status);
    Optional<ApprovalTask> findByTypeAndTargetId(String type, Long targetId);
    List<ApprovalTask> findByStaffIdAndStatus(Long staffId, String status);
}
