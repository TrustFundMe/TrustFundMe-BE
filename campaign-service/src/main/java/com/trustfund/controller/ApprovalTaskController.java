package com.trustfund.controller;

import com.trustfund.model.ApprovalTask;
import com.trustfund.service.ApprovalTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/tasks")
@RequiredArgsConstructor
@Tag(name = "Admin Task Management", description = "APIs dành cho Admin quản lý và điều phối nhiệm vụ duyệt")
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
    public ResponseEntity<List<ApprovalTask>> getTasksByStaff(@PathVariable Long staffId) {
        return ResponseEntity.ok(approvalTaskService.getTasksByStaff(staffId));
    }

    @PutMapping("/{taskId}/reassign")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Giao lại nhiệm vụ cho Staff khác (Admin only)")
    public ResponseEntity<ApprovalTask> reassignTask(
            @PathVariable Long taskId,
            @RequestParam Long newStaffId) {
        return ResponseEntity.ok(approvalTaskService.reassignTask(taskId, newStaffId));
    }
}
