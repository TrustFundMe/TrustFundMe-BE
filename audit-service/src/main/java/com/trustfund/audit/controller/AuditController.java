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

    @PostMapping
    public ResponseEntity<AuditLog> create(@RequestBody AuditLog auditLog) {
        return ResponseEntity.ok(auditLogRepository.save(auditLog));
    }

    @GetMapping("/{id}/verify")
    public ResponseEntity<Map<String, Object>> verify(@PathVariable Long id) {
        return ResponseEntity.ok(auditService.verifyIntegrity(id));
    }
}
