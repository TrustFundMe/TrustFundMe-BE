package com.trustfund.service;

import com.trustfund.model.request.UpdateTrustScoreConfigRequest;
import com.trustfund.model.response.LeaderboardResponse;
import com.trustfund.model.response.TrustScoreConfigResponse;
import com.trustfund.model.response.TrustScoreLogResponse;
import com.trustfund.model.response.UserTrustScoreResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface TrustScoreService {

    // ─── Config (Phase 1) ─────────────────────────────────────────────────
    List<TrustScoreConfigResponse> getAllConfigs();

    TrustScoreConfigResponse updateConfig(String ruleKey, UpdateTrustScoreConfigRequest request);

    // ─── Trigger & Log (Phase 2) ────────────────────────────────────────
    void addScore(Long userId, String ruleKey, Long referenceId, String referenceType, String description);

    Page<TrustScoreLogResponse> getLogs(Long userId, String ruleKey,
                                        LocalDateTime startDate, LocalDateTime endDate,
                                        Pageable pageable);

    UserTrustScoreResponse getUserScore(Long userId);

    List<LeaderboardResponse> getLeaderboard(Pageable pageable);
}