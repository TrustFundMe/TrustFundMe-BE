package com.trustfund.controller;

import com.trustfund.model.request.UpdateTrustScoreConfigRequest;
import com.trustfund.model.response.LeaderboardResponse;
import com.trustfund.model.response.TrustScoreConfigResponse;
import com.trustfund.model.response.TrustScoreLogResponse;
import com.trustfund.model.response.UserTrustScoreResponse;
import com.trustfund.service.TrustScoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/trust-score")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Trust Score", description = "API quản lý điểm uy tín")
public class TrustScoreController {

    private final TrustScoreService trustScoreService;

    // ─── Config ────────────────────────────────────────────────────────────────

    @GetMapping("/config")
    @Operation(summary = "Lấy tất cả cấu hình điểm", description = "Công khai")
    public List<TrustScoreConfigResponse> getAllConfigs() {
        return trustScoreService.getAllConfigs();
    }

    @PutMapping("/config/{ruleKey}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cập nhật cấu hình điểm", description = "Chỉ Admin")
    public TrustScoreConfigResponse updateConfig(
            @PathVariable("ruleKey") String ruleKey,
            @Valid @RequestBody UpdateTrustScoreConfigRequest request) {
        return trustScoreService.updateConfig(ruleKey, request);
    }

    // ─── Logs ─────────────────────────────────────────────────────────────────

    @GetMapping("/logs")
    @Operation(summary = "Xem nhật ký điểm uy tín", description = "Công khai, hỗ trợ filter + phân trang")
    public Page<TrustScoreLogResponse> getLogs(
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(value = "ruleKey", required = false) String ruleKey,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        return trustScoreService.getLogs(userId, ruleKey, startDate, endDate,
                PageRequest.of(page, size));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Lấy điểm uy tín của 1 user", description = "Công khai")
    public UserTrustScoreResponse getUserScore(@PathVariable("userId") Long userId) {
        log.info("CONTROLLER: Received request for user score: {}", userId);
        return trustScoreService.getUserScore(userId);
    }

    @GetMapping("/leaderboard")
    @Operation(summary = "Bảng xếp hạng điểm uy tín", description = "Công khai")
    public List<LeaderboardResponse> getLeaderboard(
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
        return trustScoreService.getLeaderboard(PageRequest.of(0, limit));
    }
}
