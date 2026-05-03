package com.trustfund.service.implementServices;

import com.trustfund.model.AuditLog;
import com.trustfund.repository.AuditLogRepository;
import com.trustfund.service.interfaceServices.AuditLogService;
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
public class AuditLogServiceImpl implements AuditLogService {

    private static final String GENESIS_HASH = "0000000000000000000000000000000000000000000000000000000000000000";

    private final AuditLogRepository auditLogRepository;

    @Override
    public AuditLog saveLog(AuditLog log) {
        Optional<AuditLog> latestLog = auditLogRepository.findTopByOrderByCreatedAtDesc();

        if (latestLog.isPresent()) {
            log.setPreviousHash(latestLog.get().getAuditHash());
        } else {
            log.setPreviousHash(GENESIS_HASH);
        }

        return auditLogRepository.save(log);
    }

    @Override
    public Map<String, Object> verifyIntegrity(Long id) {
        Optional<AuditLog> logOpt = auditLogRepository.findById(id);
        if (logOpt.isEmpty()) {
            return Map.of("error", "Not found");
        }

        AuditLog log = logOpt.get();
        String currentDataHash = calculateHash(log.getDataSnapshot());
        boolean isDataValid = currentDataHash.equalsIgnoreCase(log.getAuditHash());

        boolean isChainValid = true;
        String prevEntityInfo = null;

        Optional<AuditLog> prevLogOpt = auditLogRepository.findFirstByCreatedAtBeforeOrderByCreatedAtDesc(log.getCreatedAt());
        if (prevLogOpt.isPresent()) {
            AuditLog prevLog = prevLogOpt.get();
            String expectedPreviousHash = prevLog.getAuditHash();
            isChainValid = log.getPreviousHash() != null && log.getPreviousHash().equalsIgnoreCase(expectedPreviousHash);
            if (!isChainValid) {
                prevEntityInfo = prevLog.getEntityType() + " #" + prevLog.getEntityId()
                        + " (" + (prevLog.getActorName() != null ? prevLog.getActorName() : "System") + ")";
            }
        } else {
            isChainValid = GENESIS_HASH.equals(log.getPreviousHash());
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
