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

    public Map<String, Object> verifyIntegrity(Long id) {
        Optional<AuditLog> logOpt = auditLogRepository.findById(id);
        if (logOpt.isEmpty()) {
            return Map.of("error", "Not found");
        }

        AuditLog log = logOpt.get();
        String currentDataHash = calculateHash(log.getDataSnapshot());
        
        boolean isValid = currentDataHash.equalsIgnoreCase(log.getAuditHash());
        
        return Map.of(
                "valid", isValid,
                "storedHash", log.getAuditHash(),
                "actualHash", currentDataHash
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
