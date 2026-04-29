package com.trustfund.service;

import com.trustfund.entity.SystemConfig;

public interface SystemConfigService {
    SystemConfig getConfigByKey(String key);

    String getConfigValue(String key, String defaultValue);

    SystemConfig saveOrUpdate(SystemConfig config);
}
