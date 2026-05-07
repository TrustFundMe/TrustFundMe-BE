package com.trustfund.service.implementServices;

import com.trustfund.model.AuditLog;
import com.trustfund.repository.AuditLogRepository;
import com.trustfund.service.interfaceServices.AuditLogService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @PersistenceContext
    private EntityManager entityManager;

    private String computeHash(String data, String previousHash) {
        try {
            String content = data + previousHash;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not found", e);
        }
    }

    @Override
    @Transactional
    public AuditLog saveLog(AuditLog log) {
        Optional<AuditLog> latestLog = auditLogRepository.findTopByOrderByCreatedAtDesc();
        log.setPreviousHash(latestLog.isPresent() ? latestLog.get().getAuditHash() : GENESIS_HASH);

        // Step 1: Save with placeholder hash
        log.setAuditHash("PENDING");
        AuditLog saved = auditLogRepository.save(log);
        auditLogRepository.flush(); // Flush to DB immediately

        // Step 2: Evict from L1 cache to force fresh read from TiDB
        entityManager.refresh(saved);

        // Step 3: Now saved.getDataSnapshot() contains EXACTLY what TiDB stored
        String realHash = computeHash(saved.getDataSnapshot(), saved.getPreviousHash());

        System.out.println("=== [AUDIT SAVE] id=" + saved.getId() + " entity=" + saved.getEntityType());
        System.out.println("=== [AUDIT SAVE] snapshotAfterRefresh=" + saved.getDataSnapshot());
        System.out.println("=== [AUDIT SAVE] hash=" + realHash);

        // Step 4: Update with the real hash
        saved.setAuditHash(realHash);
        return auditLogRepository.save(saved);
    }

    @Override
    public Map<String, Object> verifyIntegrity(Long id) {
        Optional<AuditLog> logOpt = auditLogRepository.findById(id);
        if (logOpt.isEmpty()) {
            return Map.of("error", "Not found");
        }

        AuditLog log = logOpt.get();
        String currentDataHash = computeHash(log.getDataSnapshot(), log.getPreviousHash());

        System.out.println("=== [AUDIT VERIFY] id=" + id);
        System.out.println("=== [AUDIT VERIFY] snapshotInDb=" + log.getDataSnapshot());
        System.out.println("=== [AUDIT VERIFY] recomputed=" + currentDataHash);
        System.out.println("=== [AUDIT VERIFY] stored=" + log.getAuditHash());

        boolean isDataValid = currentDataHash.equalsIgnoreCase(log.getAuditHash());

        boolean isChainValid = true;
        String prevEntityInfo = null;

        Optional<AuditLog> prevLogOpt = auditLogRepository.findFirstByIdLessThanOrderByIdDesc(log.getId());
        if (prevLogOpt.isPresent()) {
            AuditLog prevLog = prevLogOpt.get();
            isChainValid = log.getPreviousHash() != null
                    && log.getPreviousHash().equalsIgnoreCase(prevLog.getAuditHash());
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
}
