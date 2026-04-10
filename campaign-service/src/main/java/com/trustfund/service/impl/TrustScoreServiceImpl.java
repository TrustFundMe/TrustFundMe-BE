package com.trustfund.service.impl;

import com.trustfund.exception.ResourceNotFoundException;
import com.trustfund.model.TrustScoreConfig;
import com.trustfund.model.TrustScoreLog;
import com.trustfund.model.request.UpdateTrustScoreConfigRequest;
import com.trustfund.model.response.LeaderboardResponse;
import com.trustfund.model.response.TrustScoreConfigResponse;
import com.trustfund.model.response.TrustScoreLogResponse;
import com.trustfund.model.response.UserTrustScoreResponse;
import com.trustfund.repository.TrustScoreConfigRepository;
import com.trustfund.repository.TrustScoreLogRepository;
import com.trustfund.service.TrustScoreService;
import com.trustfund.client.UserInfoClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrustScoreServiceImpl implements TrustScoreService {

    private final TrustScoreConfigRepository configRepository;
    private final TrustScoreLogRepository logRepository;
    private final UserInfoClient userInfoClient;
    private final com.trustfund.client.IdentityServiceClient identityServiceClient;

    // Cache config lookup: ruleKey -> points (with cache invalidation on update)
    private final Map<String, Integer> configCache = new ConcurrentHashMap<>();

    // ─── Config ───────────────────────────────────────────────────────────────

    @Override
    public List<TrustScoreConfigResponse> getAllConfigs() {
        return configRepository.findAll().stream()
                .map(this::mapConfigToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public TrustScoreConfigResponse updateConfig(String ruleKey, UpdateTrustScoreConfigRequest request) {
        TrustScoreConfig config = configRepository.findByRuleKey(ruleKey)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy quy tắc với key: " + ruleKey));

        config.setPoints(request.getPoints());
        if (request.getIsActive() != null) {
            config.setIsActive(request.getIsActive());
        }
        if (request.getRuleName() != null && !request.getRuleName().isBlank()) {
            config.setRuleName(request.getRuleName());
        }
        if (request.getDescription() != null && !request.getDescription().isBlank()) {
            config.setDescription(request.getDescription());
        }

        // Invalidate cache
        configCache.remove(ruleKey);

        return mapConfigToResponse(configRepository.save(config));
    }

    // ─── Trigger & Score ──────────────────────────────────────────────────────

    /**
     * Core method: add or deduct points for a user.
     * Reads points from DB (with cache), writes log, updates user_trust_score.
     */
    @Override
    @Transactional
    public void addScore(Long userId, String ruleKey, Long referenceId, String referenceType, String description) {
        if (userId == null) return;

        TrustScoreConfig config = configRepository.findByRuleKey(ruleKey).orElse(null);
        if (config == null || !config.getIsActive()) {
            log.debug("Trust score rule '{}' is inactive or not found, skipping.", ruleKey);
            return;
        }

        // Duplicate check: for DAILY_POST, only allow 1 per day per user
        if ("DAILY_POST".equals(ruleKey)) {
            LocalDateTime startOfDay = LocalDateTime.now().with(java.time.LocalTime.MIN);
            boolean alreadyPosted = logRepository.existsByUserIdAndRuleKeyAndCreatedAtAfter(userId, ruleKey, startOfDay);
            if (alreadyPosted) {
                log.debug("Trust score: user {} already got DAILY_POST today, skipping.", userId);
                return;
            }
        } else {
            // For other rules, check by reference (campaign/expenditure already scored)
            if (referenceId != null) {
                List<TrustScoreLog> existing = logRepository.findByUserIdAndRuleKeyAndReference(
                        userId, ruleKey, referenceId);
                if (!existing.isEmpty()) {
                    log.debug("Trust score: user {} rule {} ref {} already scored, skipping.",
                            userId, ruleKey, referenceId);
                    return;
                }
            }
        }

        int points = config.getPoints();
        log.info("➔ Triggering trust score update: userId={}, rule={}, points={}", userId, ruleKey, points);

        // Write log
        TrustScoreLog logEntry = TrustScoreLog.builder()
                .userId(userId)
                .ruleKey(ruleKey)
                .pointsChange(points)
                .referenceId(referenceId)
                .referenceType(referenceType)
                .description(description)
                .build();
        logRepository.save(logEntry);

        // Sync with Identity Service (Merge field into User table)
        log.info("➔ Calling Identity Service to sync {} points for user {}", points, userId);
        identityServiceClient.updateTrustScore(userId, points);

        log.info("Trust score synced to Identity Service: userId={}, rule={}, delta={}",
                userId, ruleKey, points);
    }

    // ─── Log Queries ───────────────────────────────────────────────────────────

    @Override
    public Page<TrustScoreLogResponse> getLogs(Long userId, String ruleKey,
                                               LocalDateTime startDate, LocalDateTime endDate,
                                               Pageable pageable) {
        log.info("Getting trust score logs for userId={}, ruleKey={}", userId, ruleKey);
        try {
            LocalDateTime start = startDate != null ? startDate.with(LocalTime.MIN) : null;
            LocalDateTime end = endDate != null ? endDate.with(LocalTime.MAX) : null;

            Page<TrustScoreLog> logs = logRepository.findByFilters(userId, ruleKey, start, end, pageable);
            log.debug("Found {} log entries", logs.getNumberOfElements());

            // Pre-fetch user info
            Map<Long, UserInfoClient.UserInfo> userInfoMap = new java.util.HashMap<>();
            logs.getContent().stream()
                    .map(TrustScoreLog::getUserId)
                    .distinct()
                    .forEach(uid -> {
                        try {
                            userInfoMap.put(uid, userInfoClient.getUserInfo(uid));
                        } catch (Exception e) {
                            log.warn("Failed to pre-fetch user info for {}: {}", uid, e.getMessage());
                            userInfoMap.put(uid, UserInfoClient.UserInfo.empty());
                        }
                    });

            // Pre-fetch rule names
            Map<String, String> ruleNameMap = new java.util.HashMap<>();
            configRepository.findAll().forEach(config -> {
                if (config.getRuleKey() != null) {
                    ruleNameMap.put(config.getRuleKey(), config.getRuleName() != null ? config.getRuleName() : config.getRuleKey());
                }
            });

            Page<TrustScoreLogResponse> responsePage = logs.map(logEntry -> mapLogToResponse(logEntry, userInfoMap, ruleNameMap));
            log.info("Successfully fetched {} logs", responsePage.getNumberOfElements());
            return responsePage;
        } catch (Exception e) {
            log.error("CRITICAL ERROR in getLogs for userId={}: {}", userId, e.getMessage(), e);
            // Return empty page instead of failing
            return Page.empty(pageable);
        }
    }

    @Override
    public UserTrustScoreResponse getUserScore(Long userId) {
        log.info("Getting trust score for user: {}", userId);
        try {
            com.trustfund.model.response.UserInfoResponse userInfo = identityServiceClient.getUserInfo(userId);
            return UserTrustScoreResponse.builder()
                    .userId(userId)
                    .userFullName(userInfo != null ? userInfo.getFullName() : "N/A")
                    .userAvatarUrl(userInfo != null ? userInfo.getAvatarUrl() : null)
                    .totalScore(userInfo != null ? userInfo.getTrustScore() : 0)
                    .build();
        } catch (Exception e) {
            log.error("CRITICAL ERROR getting user score for userId={}: {}", userId, e.getMessage(), e);
            return UserTrustScoreResponse.builder()
                    .userId(userId)
                    .totalScore(0)
                    .build();
        }
    }

    @Override
    public List<LeaderboardResponse> getLeaderboard(Pageable pageable) {
        log.info("Fetching leaderboard: page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());
        try {
            List<com.trustfund.model.response.UserInfoResponse> users = 
                    identityServiceClient.getLeaderboard(pageable.getPageNumber(), pageable.getPageSize());
            
            int[] rank = {pageable.getPageNumber() * pageable.getPageSize() + 1};
            return users.stream()
                    .map(u -> LeaderboardResponse.builder()
                            .userId(u.getId())
                            .userFullName(u.getFullName())
                            .userAvatarUrl(u.getAvatarUrl())
                            .totalScore(u.getTrustScore() != null ? u.getTrustScore() : 0)
                            .rank(rank[0]++)
                            .build())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error fetching leaderboard from Identity Service: {}", e.getMessage(), e);
            return java.util.Collections.emptyList();
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private TrustScoreConfigResponse mapConfigToResponse(TrustScoreConfig config) {
        return TrustScoreConfigResponse.builder()
                .id(config.getId())
                .ruleKey(config.getRuleKey())
                .ruleName(config.getRuleName())
                .points(config.getPoints())
                .description(config.getDescription())
                .isActive(config.getIsActive())
                .createdAt(config.getCreatedAt())
                .updatedAt(config.getUpdatedAt())
                .build();
    }

    private TrustScoreLogResponse mapLogToResponse(TrustScoreLog log,
                                                   Map<Long, UserInfoClient.UserInfo> userInfoMap,
                                                   Map<String, String> ruleNameMap) {
        UserInfoClient.UserInfo info = userInfoMap.getOrDefault(
                log.getUserId(), UserInfoClient.UserInfo.empty());
        return TrustScoreLogResponse.builder()
                .id(log.getId())
                .userId(log.getUserId())
                .userFullName(info.fullName())
                .userAvatarUrl(info.avatarUrl())
                .ruleKey(log.getRuleKey())
                .ruleName(ruleNameMap.getOrDefault(log.getRuleKey(), log.getRuleKey()))
                .pointsChange(log.getPointsChange())
                .referenceId(log.getReferenceId())
                .referenceType(log.getReferenceType())
                .description(log.getDescription())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
