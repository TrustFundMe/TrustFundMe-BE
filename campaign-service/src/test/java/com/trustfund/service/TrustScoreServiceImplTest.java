package com.trustfund.service;

import com.trustfund.client.IdentityServiceClient;
import com.trustfund.client.UserInfoClient;
import com.trustfund.model.TrustScoreConfig;
import com.trustfund.model.TrustScoreLog;
import com.trustfund.model.request.UpdateTrustScoreConfigRequest;
import com.trustfund.model.response.LeaderboardResponse;
import com.trustfund.model.response.TrustScoreConfigResponse;
import com.trustfund.model.response.TrustScoreLogResponse;
import com.trustfund.model.response.UserInfoResponse;
import com.trustfund.model.response.UserTrustScoreResponse;
import com.trustfund.repository.TrustScoreConfigRepository;
import com.trustfund.repository.TrustScoreLogRepository;
import com.trustfund.service.impl.TrustScoreServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrustScoreServiceImplTest {

    @Mock
    private TrustScoreConfigRepository configRepository;

    @Mock
    private TrustScoreLogRepository logRepository;

    @Mock
    private UserInfoClient userInfoClient;

    @Mock
    private IdentityServiceClient identityServiceClient;

    private TrustScoreServiceImpl trustScoreService;

    @BeforeEach
    void setUp() {
        trustScoreService = new TrustScoreServiceImpl(
                configRepository,
                logRepository,
                userInfoClient,
                identityServiceClient
        );
    }

    // ─── getAllConfigs ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("getAllConfigs()")
    class GetAllConfigs {

        @Test
        @DisplayName("returns list from repository")
        void returnsListFromRepo() {
            TrustScoreConfig config = TrustScoreConfig.builder()
                    .id(1L)
                    .ruleKey("DAILY_POST")
                    .ruleName("Daily Post")
                    .points(10)
                    .description("Post daily reward")
                    .isActive(true)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            when(configRepository.findAll()).thenReturn(List.of(config));

            List<TrustScoreConfigResponse> result = trustScoreService.getAllConfigs();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getRuleKey()).isEqualTo("DAILY_POST");
            assertThat(result.get(0).getPoints()).isEqualTo(10);
            verify(configRepository).findAll();
        }

        @Test
        @DisplayName("returns empty list when no configs exist")
        void returnsEmptyList() {
            when(configRepository.findAll()).thenReturn(Collections.emptyList());

            List<TrustScoreConfigResponse> result = trustScoreService.getAllConfigs();

            assertThat(result).isEmpty();
        }
    }

    // ─── getUserScore ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("getUserScore()")
    class GetUserScore {

        @Test
        @DisplayName("returns score with user info when identityServiceClient returns user")
        void returnsScoreWithUserInfo() {
            Long userId = 42L;
            UserInfoResponse userInfo = UserInfoResponse.builder()
                    .id(userId)
                    .fullName("Nguyen Van A")
                    .avatarUrl("http://avatar.url")
                    .trustScore(150)
                    .build();

            when(identityServiceClient.getUserInfo(userId)).thenReturn(userInfo);

            UserTrustScoreResponse result = trustScoreService.getUserScore(userId);

            assertThat(result.getUserId()).isEqualTo(userId);
            assertThat(result.getUserFullName()).isEqualTo("Nguyen Van A");
            assertThat(result.getUserAvatarUrl()).isEqualTo("http://avatar.url");
            assertThat(result.getTotalScore()).isEqualTo(150);
            verify(identityServiceClient).getUserInfo(userId);
        }

        @Test
        @DisplayName("handles null userInfo gracefully")
        void handlesNullUserInfoGracefully() {
            Long userId = 99L;
            when(identityServiceClient.getUserInfo(userId)).thenReturn(null);

            UserTrustScoreResponse result = trustScoreService.getUserScore(userId);

            assertThat(result.getUserId()).isEqualTo(userId);
            assertThat(result.getUserFullName()).isEqualTo("N/A");
            assertThat(result.getTotalScore()).isEqualTo(0);
        }

        @Test
        @DisplayName("handles exception from identityServiceClient gracefully")
        void handlesExceptionGracefully() {
            Long userId = 77L;
            when(identityServiceClient.getUserInfo(userId))
                    .thenThrow(new RuntimeException("Connection refused"));

            UserTrustScoreResponse result = trustScoreService.getUserScore(userId);

            assertThat(result.getUserId()).isEqualTo(userId);
            assertThat(result.getUserFullName()).isEqualTo("N/A");
            assertThat(result.getTotalScore()).isEqualTo(0);
        }
    }

    // ─── addScore ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("addScore()")
    class AddScore {

        @Test
        @DisplayName("saves TrustScoreLog entry and calls identityServiceClient")
        void savesTrustScoreLogEntry() {
            Long userId = 10L;
            String ruleKey = "CAMPAIGN_CREATED";
            Long referenceId = 5L;
            String referenceType = "CAMPAIGN";
            String description = "Campaign created successfully";

            TrustScoreConfig config = TrustScoreConfig.builder()
                    .id(1L)
                    .ruleKey(ruleKey)
                    .ruleName("Campaign Created")
                    .points(20)
                    .isActive(true)
                    .build();

            when(configRepository.findByRuleKey(ruleKey)).thenReturn(Optional.of(config));
            when(logRepository.findByUserIdAndRuleKeyAndReference(userId, ruleKey, referenceId))
                    .thenReturn(Collections.emptyList());

            trustScoreService.addScore(userId, ruleKey, referenceId, referenceType, description);

            ArgumentCaptor<TrustScoreLog> logCaptor = ArgumentCaptor.forClass(TrustScoreLog.class);
            verify(logRepository).save(logCaptor.capture());

            TrustScoreLog savedLog = logCaptor.getValue();
            assertThat(savedLog.getUserId()).isEqualTo(userId);
            assertThat(savedLog.getRuleKey()).isEqualTo(ruleKey);
            assertThat(savedLog.getPointsChange()).isEqualTo(20);
            assertThat(savedLog.getReferenceId()).isEqualTo(referenceId);
            assertThat(savedLog.getReferenceType()).isEqualTo(referenceType);
            assertThat(savedLog.getDescription()).isEqualTo(description);

            verify(identityServiceClient).updateTrustScore(userId, 20);
        }

        @Test
        @DisplayName("skips when config not found")
        void skipsWhenConfigNotFound() {
            Long userId = 10L;
            when(configRepository.findByRuleKey("UNKNOWN_RULE")).thenReturn(Optional.empty());

            trustScoreService.addScore(userId, "UNKNOWN_RULE", null, null, "test");

            verify(logRepository, never()).save(any(TrustScoreLog.class));
            verify(identityServiceClient, never()).updateTrustScore(anyLong(), anyInt());
        }

        @Test
        @DisplayName("skips when config is inactive")
        void skipsWhenConfigInactive() {
            Long userId = 10L;
            TrustScoreConfig config = TrustScoreConfig.builder()
                    .ruleKey("INACTIVE_RULE")
                    .isActive(false)
                    .build();
            when(configRepository.findByRuleKey("INACTIVE_RULE")).thenReturn(Optional.of(config));

            trustScoreService.addScore(userId, "INACTIVE_RULE", null, null, "test");

            verify(logRepository, never()).save(any(TrustScoreLog.class));
        }

        @Test
        @DisplayName("skips when userId is null")
        void skipsWhenUserIdNull() {
            trustScoreService.addScore(null, "RULE", 1L, "TYPE", "desc");

            verify(configRepository, never()).findByRuleKey(anyString());
            verify(logRepository, never()).save(any(TrustScoreLog.class));
        }

        @Test
        @DisplayName("skips duplicate score for same reference")
        void skipsDuplicateScore() {
            Long userId = 10L;
            Long referenceId = 5L;
            TrustScoreConfig config = TrustScoreConfig.builder()
                    .ruleKey("CAMPAIGN_APPROVED")
                    .points(30)
                    .isActive(true)
                    .build();

            when(configRepository.findByRuleKey("CAMPAIGN_APPROVED")).thenReturn(Optional.of(config));
            when(logRepository.findByUserIdAndRuleKeyAndReference(userId, "CAMPAIGN_APPROVED", referenceId))
                    .thenReturn(List.of(TrustScoreLog.builder().id(1L).build()));

            trustScoreService.addScore(userId, "CAMPAIGN_APPROVED", referenceId, "CAMPAIGN", "approved");

            verify(logRepository, never()).save(any(TrustScoreLog.class));
        }
    }

    // ─── getLogs ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getLogs()")
    class GetLogs {

        @Test
        @DisplayName("returns paginated results")
        void returnsPaginatedResults() {
            Long userId = 5L;
            Pageable pageable = PageRequest.of(0, 10);
            TrustScoreLog log = TrustScoreLog.builder()
                    .id(1L)
                    .userId(userId)
                    .ruleKey("DAILY_POST")
                    .pointsChange(10)
                    .createdAt(LocalDateTime.now())
                    .build();

            Page<TrustScoreLog> logPage = new PageImpl<>(List.of(log), pageable, 1);

            when(logRepository.findByFilters(any(), any(), any(), any(), any())).thenReturn(logPage);
            when(configRepository.findAll()).thenReturn(Collections.emptyList());
            when(userInfoClient.getUserInfo(userId))
                    .thenReturn(new UserInfoClient.UserInfo("Test User", "http://avatar.png"));

            Page<TrustScoreLogResponse> result = trustScoreService.getLogs(
                    userId, null, null, null, pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getUserId()).isEqualTo(userId);
            assertThat(result.getContent().get(0).getPointsChange()).isEqualTo(10);
        }
    }

    // ─── getLeaderboard ────────────────────────────────────────────────────

    @Nested
    @DisplayName("getLeaderboard()")
    class GetLeaderboard {

        @Test
        @DisplayName("returns list from identityServiceClient")
        void returnsListFromClient() {
            Pageable pageable = PageRequest.of(0, 10);
            UserInfoResponse user1 = UserInfoResponse.builder()
                    .id(1L)
                    .fullName("Alice")
                    .avatarUrl("http://alice.png")
                    .trustScore(200)
                    .build();
            UserInfoResponse user2 = UserInfoResponse.builder()
                    .id(2L)
                    .fullName("Bob")
                    .avatarUrl("http://bob.png")
                    .trustScore(150)
                    .build();

            when(identityServiceClient.getLeaderboard(0, 10)).thenReturn(List.of(user1, user2));

            List<LeaderboardResponse> result = trustScoreService.getLeaderboard(pageable);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getRank()).isEqualTo(1);
            assertThat(result.get(0).getUserFullName()).isEqualTo("Alice");
            assertThat(result.get(0).getTotalScore()).isEqualTo(200);
            assertThat(result.get(1).getRank()).isEqualTo(2);
            assertThat(result.get(1).getTotalScore()).isEqualTo(150);
        }

        @Test
        @DisplayName("returns empty list when client throws exception")
        void returnsEmptyListOnException() {
            Pageable pageable = PageRequest.of(0, 10);
            when(identityServiceClient.getLeaderboard(0, 10))
                    .thenThrow(new RuntimeException("Service unavailable"));

            List<LeaderboardResponse> result = trustScoreService.getLeaderboard(pageable);

            assertThat(result).isEmpty();
        }
    }

    // ─── updateConfig ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateConfig()")
    class UpdateConfig {

        @Test
        @DisplayName("updates config and returns response")
        void updatesConfigAndReturnsResponse() {
            String ruleKey = "DAILY_POST";
            TrustScoreConfig existingConfig = TrustScoreConfig.builder()
                    .id(1L)
                    .ruleKey(ruleKey)
                    .ruleName("Daily Post")
                    .points(10)
                    .description("Old description")
                    .isActive(true)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            UpdateTrustScoreConfigRequest request = UpdateTrustScoreConfigRequest.builder()
                    .points(15)
                    .isActive(true)
                    .ruleName("Updated Daily Post")
                    .description("New description")
                    .build();

            when(configRepository.findByRuleKey(ruleKey)).thenReturn(Optional.of(existingConfig));
            when(configRepository.save(any(TrustScoreConfig.class))).thenAnswer(inv -> inv.getArgument(0));

            TrustScoreConfigResponse result = trustScoreService.updateConfig(ruleKey, request);

            assertThat(result.getPoints()).isEqualTo(15);
            assertThat(result.getRuleName()).isEqualTo("Updated Daily Post");
            assertThat(result.getDescription()).isEqualTo("New description");
            assertThat(result.getIsActive()).isTrue();
            verify(configRepository).save(existingConfig);
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when config not found")
        void throwsNotFoundWhenConfigNotFound() {
            when(configRepository.findByRuleKey("UNKNOWN_KEY")).thenReturn(Optional.empty());

            UpdateTrustScoreConfigRequest request = UpdateTrustScoreConfigRequest.builder()
                    .points(5)
                    .build();

            org.junit.jupiter.api.Assertions.assertThrows(
                    com.trustfund.exception.ResourceNotFoundException.class,
                    () -> trustScoreService.updateConfig("UNKNOWN_KEY", request)
            );
        }
    }
}
