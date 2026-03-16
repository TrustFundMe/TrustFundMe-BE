package com.trustfund.service.impl;

import com.trustfund.client.IdentityServiceClient;
import com.trustfund.model.ApprovalTask;
import com.trustfund.repository.ApprovalTaskRepository;
import com.trustfund.service.ApprovalTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApprovalTaskServiceImpl implements ApprovalTaskService {

    private final ApprovalTaskRepository approvalTaskRepository;
    private final IdentityServiceClient identityServiceClient;
    private final Random random = new Random();

    @Override
    @Transactional
    public ApprovalTask createAndAssignTask(String type, Long targetId) {
        log.info("Creating and assigning task for type: {} and targetId: {}", type, targetId);
        
        List<Long> staffIds = identityServiceClient.getStaffIds();
        Long assignedStaffId = null;
        
        if (staffIds != null && !staffIds.isEmpty()) {
            assignedStaffId = staffIds.get(random.nextInt(staffIds.size()));
            log.info("Assigned task to staffId: {}", assignedStaffId);
        } else {
            log.warn("No staff found to assign task for type: {} and targetId: {}", type, targetId);
        }

        ApprovalTask task = ApprovalTask.builder()
                .type(type)
                .targetId(targetId)
                .staffId(assignedStaffId)
                .status("PENDING")
                .build();

        return approvalTaskRepository.save(task);
    }

    @Override
    @Transactional
    public ApprovalTask reassignTask(Long taskId, Long newStaffId) {
        ApprovalTask task = approvalTaskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found with id: " + taskId));
        
        log.info("Reassigning task {} from staff {} to staff {}", taskId, task.getStaffId(), newStaffId);
        task.setStaffId(newStaffId);
        task.setStatus("REASSIGNED"); // Or keep it PENDING if the new person hasn't started
        
        return approvalTaskRepository.save(task);
    }

    @Override
    public List<ApprovalTask> getAllTasks() {
        return approvalTaskRepository.findAll();
    }

    @Override
    public List<ApprovalTask> getTasksByStaff(Long staffId) {
        return approvalTaskRepository.findByStaffId(staffId);
    }

    @Override
    @Transactional
    public ApprovalTask completeTask(String type, Long targetId) {
        log.info("Completing task for type: {} and targetId: {}", type, targetId);
        return approvalTaskRepository.findByTypeAndTargetId(type, targetId)
                .map(task -> {
                    task.setStatus("COMPLETED");
                    return approvalTaskRepository.save(task);
                })
                .orElse(null);
    }
}
