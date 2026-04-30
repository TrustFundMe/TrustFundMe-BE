package com.trustfund.service.impl;

import com.trustfund.entity.SystemConfig;
import com.trustfund.repository.SystemConfigRepository;
import com.trustfund.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SystemConfigServiceImpl implements SystemConfigService {

    private final SystemConfigRepository repository;

    @Override
    @Cacheable(value = "systemConfigCache", key = "#key", unless = "#result == null")
    public SystemConfig getConfigByKey(String key) {
        log.info("Fetching config from DB for key: {}", key);
        return repository.findByConfigKey(key).orElse(null);
    }

    @Override
    @Cacheable(value = "systemConfigCache_v", key = "#key")
    public String getConfigValue(String key, String defaultValue) {
        return repository.findByConfigKey(key)
                .map(SystemConfig::getConfigValue)
                .orElse(defaultValue);
    }

    @Override
    @Transactional
    @CachePut(value = "systemConfigCache", key = "#config.configKey")
    public SystemConfig saveOrUpdate(SystemConfig config) {
        SystemConfig existing = repository.findByConfigKey(config.getConfigKey()).orElse(null);
        if (existing != null) {
            existing.setConfigValue(config.getConfigValue());
            existing.setConfigGroup(config.getConfigGroup());
            existing.setMetadata(config.getMetadata());
            existing.setDescription(config.getDescription());
            existing.setUpdatedBy(config.getUpdatedBy());
            return repository.save(existing);
        }
        return repository.save(config);
    }
}
