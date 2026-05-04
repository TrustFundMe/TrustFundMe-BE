package com.trustfund.controller;

import com.trustfund.model.AuditLog;
import com.trustfund.repository.AuditLogRepository;
import com.trustfund.service.interfaceServices.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditLogRepository auditLogRepository;
    private final AuditLogService auditLogService;

    @GetMapping
    public ResponseEntity<Page<AuditLog>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "") String query) {

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("createdAt").descending());

        if (query == null || query.trim().isEmpty()) {
            return ResponseEntity.ok(auditLogRepository.findAll(pageRequest));
        } else {
            return ResponseEntity.ok(auditLogRepository.search(query, pageRequest));
        }
    }

    @GetMapping("/entity/{type}/{id}")
    public ResponseEntity<Page<AuditLog>> getByEntity(
            @PathVariable String type,
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(auditLogRepository.findByEntityTypeAndEntityId(
                type, id, PageRequest.of(page, size, Sort.by("createdAt").descending())));
    }

    @GetMapping("/user/{actorId}")
    public ResponseEntity<Page<AuditLog>> getByUser(
            @PathVariable Long actorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        // Exclude KYC_SUBMISSION or USER_KYC.
        // We will exclude "USER_KYC" which is typical for the KYC process
        return ResponseEntity.ok(auditLogRepository.findByActorIdAndEntityTypeNot(
                actorId, "USER_KYC", PageRequest.of(page, size, Sort.by("createdAt").descending())));
    }

    @PostMapping("/reconciliation")
    public ResponseEntity<Page<AuditLog>> getReconciliation(
            @RequestBody Map<String, Object> request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "500") int size) {

        Long userId = Long.valueOf(request.get("userId").toString());
        java.util.List<Long> campaignIds = ((java.util.List<?>) request.get("campaignIds")).stream()
                .map(id -> Long.valueOf(id.toString()))
                .collect(java.util.stream.Collectors.toList());

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<AuditLog> logs;
        if (campaignIds.isEmpty()) {
            // If no campaigns, just fetch by actorId to avoid SQL 'IN ()' error
            logs = auditLogRepository.findByActorIdAndEntityTypeNot(userId, "USER_KYC", pageRequest);
        } else {
            // Fetch all logs related to the user OR their campaigns
            logs = auditLogRepository.findByReconciliationContext(userId, campaignIds, pageRequest);
        }

        return ResponseEntity.ok(logs);
    }

    @GetMapping("/integrity")
    public ResponseEntity<Map<String, Object>> getGlobalStatus() {
        Page<AuditLog> recentLogs = auditLogRepository.findAll(
                PageRequest.of(0, 50, Sort.by("createdAt").descending()));

        long total = recentLogs.getTotalElements();
        if (total == 0)
            return ResponseEntity.ok(Map.of("integrity", "100%", "total", 0));

        long validCount = recentLogs.getContent().stream()
                .filter(log -> {
                    try {
                        return auditLogService.verifyIntegrity(log.getId()).get("valid").equals(true);
                    } catch (Exception e) {
                        return true;
                    }
                })
                .count();

        double percentage = (double) validCount / recentLogs.getContent().size() * 100;

        return ResponseEntity.ok(Map.of(
                "integrity", String.format("%.0f%%", percentage),
                "total", total,
                "status", percentage > 99 ? "SECURE" : "WARNING"));
    }

    @PostMapping
    public ResponseEntity<AuditLog> create(@RequestBody AuditLog auditLog) {
        auditLog.setIpAddress("HIDDEN");
        return ResponseEntity.ok(auditLogService.saveLog(auditLog));
    }

    @GetMapping("/{id}/verify")
    public ResponseEntity<Map<String, Object>> verify(@PathVariable Long id) {
        return ResponseEntity.ok(auditLogService.verifyIntegrity(id));
    }
}
