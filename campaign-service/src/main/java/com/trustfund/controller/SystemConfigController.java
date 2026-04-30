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
}
