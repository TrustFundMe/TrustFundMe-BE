package com.trustfund.service.interfaceServices;

import com.trustfund.model.AuditLog;

import java.util.Map;

public interface AuditLogService {

    AuditLog saveLog(AuditLog log);

    Map<String, Object> verifyIntegrity(Long id);
}
