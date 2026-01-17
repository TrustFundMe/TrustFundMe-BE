package com.trustfund.controller;

import com.trustfund.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/users")
@RequiredArgsConstructor
@Tag(name = "Internal", description = "API nội bộ cho service khác gọi (VD: campaign-service)")
public class InternalUserController {

    private final UserRepository userRepository;

    @GetMapping("/{id}/exists")
    @Operation(summary = "Kiểm tra user có tồn tại không (dùng bởi campaign-service khi tạo campaign)")
    public ResponseEntity<Void> exists(@PathVariable Long id) {
        if (userRepository.existsById(id)) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
}
