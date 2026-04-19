package com.trustfund.service;

import com.trustfund.client.IdentityServiceClient;
import com.trustfund.model.Campaign;
import com.trustfund.model.CampaignFollow;
import com.trustfund.model.CampaignFollowId;
import com.trustfund.model.response.CampaignFollowerResponse;
import com.trustfund.model.response.UserInfoResponse;
import com.trustfund.repository.CampaignFollowRepository;
import com.trustfund.repository.CampaignRepository;
import com.trustfund.service.impl.CampaignFollowServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CampaignFollowServiceImplTest {

    @Mock
    private CampaignFollowRepository campaignFollowRepository;

    @Mock
    private CampaignRepository campaignRepository;

    @Mock
    private IdentityServiceClient identityServiceClient;

    private CampaignFollowServiceImpl campaignFollowService;

    @BeforeEach
    void setUp() {
        campaignFollowService = new CampaignFollowServiceImpl(
                campaignFollowRepository,
                campaignRepository,
                identityServiceClient
        );
    }

    // ─── follow ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("follow()")
    class Follow {

        @Test
        @DisplayName("saves CampaignFollow when not already following")
        void savesCampaignFollowWhenNotAlreadyFollowing() {
            Long campaignId = 10L;
            Long userId = 20L;
            CampaignFollowId followId = new CampaignFollowId(campaignId, userId);

            Campaign campaign = Campaign.builder().id(campaignId).title("Help Needy").build();

            when(campaignRepository.findById(campaignId)).thenReturn(Optional.of(campaign));
            when(campaignFollowRepository.existsById(followId)).thenReturn(false);

            campaignFollowService.follow(campaignId, userId);

            ArgumentCaptor<CampaignFollow> captor = ArgumentCaptor.forClass(CampaignFollow.class);
            verify(campaignFollowRepository).save(captor.capture());
            assertThat(captor.getValue().getId().getCampaignId()).isEqualTo(campaignId);
            assertThat(captor.getValue().getId().getUserId()).isEqualTo(userId);
        }

        @Test
        @DisplayName("handles idempotently when already following")
        void handlesIdempotentlyWhenAlreadyFollowing() {
            Long campaignId = 10L;
            Long userId = 20L;
            CampaignFollowId followId = new CampaignFollowId(campaignId, userId);

            Campaign campaign = Campaign.builder().id(campaignId).build();

            when(campaignRepository.findById(campaignId)).thenReturn(Optional.of(campaign));
            when(campaignFollowRepository.existsById(followId)).thenReturn(true);

            // Should not throw, should be idempotent
            campaignFollowService.follow(campaignId, userId);

            verify(campaignFollowRepository, never()).save(any(CampaignFollow.class));
        }

        @Test
        @DisplayName("throws ResponseStatusException 404 when campaign not found")
        void throwsNotFoundWhenCampaignNotFound() {
            Long campaignId = 99L;
            Long userId = 20L;

            when(campaignRepository.findById(campaignId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> campaignFollowService.follow(campaignId, userId))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode().value()).isEqualTo(404));

            verify(campaignFollowRepository, never()).save(any(CampaignFollow.class));
        }
    }

    // ─── unfollow ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("unfollow()")
    class Unfollow {

        @Test
        @DisplayName("removes follow when following")
        void removesFollowWhenFollowing() {
            Long campaignId = 10L;
            Long userId = 20L;
            CampaignFollowId followId = new CampaignFollowId(campaignId, userId);

            when(campaignFollowRepository.existsById(followId)).thenReturn(true);

            campaignFollowService.unfollow(campaignId, userId);

            verify(campaignFollowRepository).deleteById(followId);
        }

        @Test
        @DisplayName("handles idempotently when not following")
        void handlesIdempotentlyWhenNotFollowing() {
            Long campaignId = 10L;
            Long userId = 20L;
            CampaignFollowId followId = new CampaignFollowId(campaignId, userId);

            when(campaignFollowRepository.existsById(followId)).thenReturn(false);

            // Should not throw, should be idempotent
            campaignFollowService.unfollow(campaignId, userId);

            verify(campaignFollowRepository, never()).deleteById(any(CampaignFollowId.class));
        }
    }

    // ─── isFollowing ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("isFollowing()")
    class IsFollowing {

        @Test
        @DisplayName("returns true when user is following campaign")
        void returnsTrueWhenFollowing() {
            Long campaignId = 5L;
            Long userId = 15L;

            when(campaignFollowRepository.existsById_CampaignIdAndId_UserId(campaignId, userId))
                    .thenReturn(true);

            boolean result = campaignFollowService.isFollowing(campaignId, userId);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("returns false when user is not following campaign")
        void returnsFalseWhenNotFollowing() {
            Long campaignId = 5L;
            Long userId = 15L;

            when(campaignFollowRepository.existsById_CampaignIdAndId_UserId(campaignId, userId))
                    .thenReturn(false);

            boolean result = campaignFollowService.isFollowing(campaignId, userId);

            assertThat(result).isFalse();
        }
    }

    // ─── countFollowers ────────────────────────────────────────────────────

    @Nested
    @DisplayName("countFollowers()")
    class CountFollowers {

        @Test
        @DisplayName("returns correct count")
        void returnsCorrectCount() {
            Long campaignId = 7L;
            when(campaignFollowRepository.countById_CampaignId(campaignId)).thenReturn(42L);

            long result = campaignFollowService.countFollowers(campaignId);

            assertThat(result).isEqualTo(42L);
            verify(campaignFollowRepository).countById_CampaignId(campaignId);
        }

        @Test
        @DisplayName("returns zero when no followers")
        void returnsZeroWhenNoFollowers() {
            Long campaignId = 99L;
            when(campaignFollowRepository.countById_CampaignId(campaignId)).thenReturn(0L);

            long result = campaignFollowService.countFollowers(campaignId);

            assertThat(result).isZero();
        }
    }

    // ─── getMyFollowedCampaignIds ─────────────────────────────────────────

    @Nested
    @DisplayName("getMyFollowedCampaignIds()")
    class GetMyFollowedCampaignIds {

        @Test
        @DisplayName("returns list of followed campaign ids")
        void returnsListOfFollowedCampaignIds() {
            Long userId = 30L;
            List<Long> campaignIds = List.of(1L, 5L, 12L);

            when(campaignFollowRepository.findCampaignIdsFollowedByUser(userId))
                    .thenReturn(campaignIds);

            List<Long> result = campaignFollowService.getMyFollowedCampaignIds(userId);

            assertThat(result).hasSize(3);
            assertThat(result).containsExactly(1L, 5L, 12L);
            verify(campaignFollowRepository).findCampaignIdsFollowedByUser(userId);
        }

        @Test
        @DisplayName("returns empty list when user follows no campaigns")
        void returnsEmptyListWhenNoFollows() {
            Long userId = 99L;
            when(campaignFollowRepository.findCampaignIdsFollowedByUser(userId))
                    .thenReturn(Collections.emptyList());

            List<Long> result = campaignFollowService.getMyFollowedCampaignIds(userId);

            assertThat(result).isEmpty();
        }
    }

    // ─── getFollowersByCampaignId ─────────────────────────────────────────

    @Nested
    @DisplayName("getFollowersByCampaignId()")
    class GetFollowersByCampaignId {

        @Test
        @DisplayName("returns list of followers with user info")
        void returnsListOfFollowersWithUserInfo() {
            Long campaignId = 10L;
            LocalDateTime now = LocalDateTime.now();

            CampaignFollowId id1 = new CampaignFollowId(campaignId, 20L);
            CampaignFollow follow1 = CampaignFollow.builder()
                    .id(id1)
                    .followedAt(now)
                    .build();

            CampaignFollowId id2 = new CampaignFollowId(campaignId, 30L);
            CampaignFollow follow2 = CampaignFollow.builder()
                    .id(id2)
                    .followedAt(now.minusHours(1))
                    .build();

            UserInfoResponse userInfo1 = UserInfoResponse.builder()
                    .id(20L)
                    .fullName("Alice")
                    .avatarUrl("http://avatar/alice.png")
                    .build();
            UserInfoResponse userInfo2 = UserInfoResponse.builder()
                    .id(30L)
                    .fullName("Bob")
                    .avatarUrl("http://avatar/bob.png")
                    .build();

            when(campaignFollowRepository.findById_CampaignIdOrderByFollowedAtDesc(campaignId))
                    .thenReturn(List.of(follow1, follow2));
            when(identityServiceClient.getUserInfo(20L)).thenReturn(userInfo1);
            when(identityServiceClient.getUserInfo(30L)).thenReturn(userInfo2);

            List<CampaignFollowerResponse> result = campaignFollowService.getFollowersByCampaignId(campaignId);

            assertThat(result).hasSize(2);

            assertThat(result.get(0).getUserId()).isEqualTo(20L);
            assertThat(result.get(0).getUserName()).isEqualTo("Alice");
            assertThat(result.get(0).getAvatarUrl()).isEqualTo("http://avatar/alice.png");
            assertThat(result.get(0).getFollowedAt()).isEqualTo(now);

            assertThat(result.get(1).getUserId()).isEqualTo(30L);
            assertThat(result.get(1).getUserName()).isEqualTo("Bob");
            assertThat(result.get(1).getAvatarUrl()).isEqualTo("http://avatar/bob.png");
        }

        @Test
        @DisplayName("returns list with default name when userInfo is null")
        void returnsListWithDefaultNameWhenUserInfoNull() {
            Long campaignId = 10L;
            LocalDateTime now = LocalDateTime.now();

            CampaignFollowId id1 = new CampaignFollowId(campaignId, 40L);
            CampaignFollow follow1 = CampaignFollow.builder()
                    .id(id1)
                    .followedAt(now)
                    .build();

            when(campaignFollowRepository.findById_CampaignIdOrderByFollowedAtDesc(campaignId))
                    .thenReturn(List.of(follow1));
            when(identityServiceClient.getUserInfo(40L)).thenReturn(null);

            List<CampaignFollowerResponse> result = campaignFollowService.getFollowersByCampaignId(campaignId);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getUserId()).isEqualTo(40L);
            assertThat(result.get(0).getUserName()).isEqualTo("Người dùng");
            assertThat(result.get(0).getAvatarUrl()).isNull();
        }

        @Test
        @DisplayName("returns empty list when no followers")
        void returnsEmptyListWhenNoFollowers() {
            Long campaignId = 99L;
            when(campaignFollowRepository.findById_CampaignIdOrderByFollowedAtDesc(campaignId))
                    .thenReturn(Collections.emptyList());

            List<CampaignFollowerResponse> result = campaignFollowService.getFollowersByCampaignId(campaignId);

            assertThat(result).isEmpty();
            verify(identityServiceClient, never()).getUserInfo(any());
        }
    }
}
