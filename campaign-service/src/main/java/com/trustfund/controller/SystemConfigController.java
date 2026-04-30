package com.trustfund.controller;

import com.trustfund.entity.SystemConfig;
import com.trustfund.repository.SystemConfigRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/system-configs")
@RequiredArgsConstructor
@Tag(name = "System Config", description = "API lấy cấu hình hệ thống")
public class SystemConfigController {

    private final SystemConfigRepository systemConfigRepository;

    @GetMapping("/{key}")
    @Operation(summary = "Lấy cấu hình theo key", description = "Lấy giá trị cấu hình hệ thống (ví dụ: Prompt AI) theo key.")
    public ResponseEntity<SystemConfig> getByKey(@PathVariable("key") String key) {
        return systemConfigRepository.findByConfigKey(key)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/group/{group}")
    @Operation(summary = "Lấy danh sách cấu hình theo nhóm", description = "Lấy tất cả cấu hình thuộc một nhóm nhất định (ví dụ: AI).")
    public ResponseEntity<java.util.List<SystemConfig>> getByGroup(@PathVariable("group") String group) {
        return ResponseEntity.ok(systemConfigRepository.findAllByConfigGroup(group));
    }

    @PutMapping("/{key}")
    @Operation(summary = "Cập nhật cấu hình", description = "Cập nhật giá trị cấu hình hệ thống.")
    public ResponseEntity<SystemConfig> update(@PathVariable("key") String key, @RequestBody SystemConfig newConfig) {
        return systemConfigRepository.findByConfigKey(key)
                .map(config -> {
                    config.setConfigValue(newConfig.getConfigValue());
                    config.setDescription(newConfig.getDescription());
                    config.setUpdatedBy("ADMIN_UI");
                    return ResponseEntity.ok(systemConfigRepository.save(config));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
