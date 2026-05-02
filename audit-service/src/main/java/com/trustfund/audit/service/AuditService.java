package com.trustfund.audit.service;

import com.trustfund.audit.entity.AuditLog;
import com.trustfund.audit.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditLog saveLog(AuditLog log) {
        // Find the latest record to link the chain
        Optional<AuditLog> latestLog = auditLogRepository.findTopByOrderByCreatedAtDesc();
        
        if (latestLog.isPresent()) {
            log.setPreviousHash(latestLog.get().getAuditHash());
        } else {
            log.setPreviousHash("0000000000000000000000000000000000000000000000000000000000000000");
        }
        
        return auditLogRepository.save(log);
    }

    public Map<String, Object> verifyIntegrity(Long id) {
        Optional<AuditLog> logOpt = auditLogRepository.findById(id);
        if (logOpt.isEmpty()) {
            return Map.of("error", "Not found");
        }

        AuditLog log = logOpt.get();
        String currentDataHash = calculateHash(log.getDataSnapshot());
        boolean isDataValid = currentDataHash.equalsIgnoreCase(log.getAuditHash());
        
        // Chain Verification: Check if this record links correctly to the previous one
        boolean isChainValid = true;
        String expectedPreviousHash = null;
        
        String prevEntityInfo = null;
        Optional<AuditLog> prevLogOpt = auditLogRepository.findFirstByCreatedAtBeforeOrderByCreatedAtDesc(log.getCreatedAt());
        if (prevLogOpt.isPresent()) {
            AuditLog prevLog = prevLogOpt.get();
            expectedPreviousHash = prevLog.getAuditHash();
            isChainValid = log.getPreviousHash() != null && log.getPreviousHash().equalsIgnoreCase(expectedPreviousHash);
            if (!isChainValid) {
                prevEntityInfo = prevLog.getEntityType() + " #" + prevLog.getEntityId() + " (" + (prevLog.getActorName() != null ? prevLog.getActorName() : "System") + ")";
            }
        } else {
            isChainValid = "0000000000000000000000000000000000000000000000000000000000000000".equals(log.getPreviousHash());
        }
        
        return Map.of(
                "valid", isDataValid && isChainValid,
                "dataValid", isDataValid,
                "chainValid", isChainValid,
                "storedHash", log.getAuditHash(),
                "actualHash", currentDataHash,
                "tamperedEntity", prevEntityInfo != null ? prevEntityInfo : "None"
        );
    }

    private String calculateHash(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }
}
