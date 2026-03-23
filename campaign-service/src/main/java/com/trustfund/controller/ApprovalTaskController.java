package com.trustfund.controller;

import com.trustfund.model.ApprovalTask;
import com.trustfund.model.response.ApprovalTaskResponse;
import com.trustfund.service.ApprovalTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/tasks")
@RequiredArgsConstructor
@Tag(name = "Admin Task Management", description = "APIs dành cho Admin quản lý và điều phối nhiệm vụ duyệt")
@Slf4j
public class ApprovalTaskController {

    private final ApprovalTaskService approvalTaskService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lấy tất cả các nhiệm vụ duyệt (Admin only)")
    public ResponseEntity<List<ApprovalTask>> getAllTasks() {
        return ResponseEntity.ok(approvalTaskService.getAllTasks());
    }

    @GetMapping("/staff/{staffId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Lấy danh sách nhiệm vụ của một Staff")
    public ResponseEntity<List<ApprovalTaskResponse>> getTasksByStaff(@PathVariable("staffId") Long staffId) {
        log.info("Received request to fetch tasks for staffId: {}", staffId);
        List<ApprovalTask> tasks = approvalTaskService.getTasksByStaff(staffId);
        List<ApprovalTaskResponse> response = tasks.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{taskId}/reassign")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Giao lại nhiệm vụ cho Staff khác (Admin only)")
    public ResponseEntity<ApprovalTask> reassignTask(
            @PathVariable("taskId") Long taskId,
            @RequestParam("newStaffId") Long newStaffId) {
        return ResponseEntity.ok(approvalTaskService.reassignTask(taskId, newStaffId));
    }

    @GetMapping("/campaign/{campaignId}")
    @Operation(summary = "Lấy task duyệt của một campaign (cho FundOwner xem staff phụ trách)")
    public ResponseEntity<ApprovalTaskResponse> getTaskByCampaign(@PathVariable("campaignId") Long campaignId) {
        ApprovalTask task = approvalTaskService.getTaskByCampaignId(campaignId);
        if (task == null) {
            return ResponseEntity.ok(ApprovalTaskResponse.builder().build());
        }
        return ResponseEntity.ok(convertToResponse(task));
    }

    private ApprovalTaskResponse convertToResponse(ApprovalTask task) {
        return ApprovalTaskResponse.builder()
                .id(task.getId())
                .type(task.getType())
                .targetId(task.getTargetId())
                .staffId(task.getStaffId())
                .status(task.getStatus())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }
}
