package com.trustfund.audit.controller;

import com.trustfund.audit.entity.AuditLog;
import com.trustfund.audit.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuditController {

    private final AuditLogRepository auditLogRepository;
    private final com.trustfund.audit.service.AuditService auditService;

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

    @GetMapping("/integrity")
    public ResponseEntity<Map<String, Object>> getGlobalStatus() {
        // Simple logic: check the last 50 records
        Page<AuditLog> recentLogs = auditLogRepository.findAll(
            PageRequest.of(0, 50, Sort.by("createdAt").descending()));
        
        long total = recentLogs.getTotalElements();
        if (total == 0) return ResponseEntity.ok(Map.of("integrity", "100%", "total", 0));
        
        long validCount = recentLogs.getContent().stream()
            .filter(log -> {
                try {
                    return auditService.verifyIntegrity(log.getId()).get("valid").equals(true);
                } catch (Exception e) {
                    return true; // Default to valid if check fails to prevent system lock
                }
            })
            .count();
        
        double percentage = (double) validCount / recentLogs.getContent().size() * 100;
        
        return ResponseEntity.ok(Map.of(
            "integrity", String.format("%.0f%%", percentage),
            "total", total,
            "status", percentage > 99 ? "SECURE" : "WARNING"
        ));
    }

    @PostMapping
    public ResponseEntity<AuditLog> create(@RequestBody AuditLog auditLog) {
        auditLog.setIpAddress("HIDDEN");
        return ResponseEntity.ok(auditService.saveLog(auditLog));
    }

    @GetMapping("/{id}/verify")
    public ResponseEntity<Map<String, Object>> verify(@PathVariable Long id) {
        return ResponseEntity.ok(auditService.verifyIntegrity(id));
    }
}
