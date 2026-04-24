package com.trustfund.service;

import com.trustfund.client.NotificationServiceClient;
import com.trustfund.client.UserInfoClient;
import com.trustfund.model.Campaign;
import com.trustfund.model.Flag;
import com.trustfund.model.request.FlagRequest;
import com.trustfund.model.response.FlagResponse;
import com.trustfund.repository.CampaignFollowRepository;
import com.trustfund.repository.CampaignRepository;
import com.trustfund.repository.FeedPostRepository;
import com.trustfund.repository.FlagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FlagServiceImplTest {

    @Mock private FlagRepository flagRepository;
    @Mock private ApprovalTaskService approvalTaskService;
    @Mock private CampaignFollowRepository campaignFollowRepository;
    @Mock private CampaignRepository campaignRepository;
    @Mock private FeedPostRepository feedPostRepository;
    @Mock private NotificationServiceClient notificationServiceClient;
    @Mock private UserInfoClient userInfoClient;

    @InjectMocks private FlagServiceImpl service;

    @BeforeEach
    void setUp() {
        lenient().when(userInfoClient.getUserInfo(any())).thenReturn(new UserInfoClient.UserInfo("User", null));
    }

    @Test @DisplayName("submitFlag_bothIdsNull_throws")
    void bothNull() {
        FlagRequest r = FlagRequest.builder().reason("x").build();
        assertThatThrownBy(() -> service.submitFlag(1L, r)).isInstanceOf(RuntimeException.class);
    }

    @Test @DisplayName("submitFlag_duplicateCampaign_throws")
    void dupCampaign() {
        FlagRequest r = FlagRequest.builder().campaignId(10L).reason("x").build();
        when(flagRepository.existsByUserIdAndCampaignId(1L, 10L)).thenReturn(true);
        assertThatThrownBy(() -> service.submitFlag(1L, r)).isInstanceOf(IllegalStateException.class);
    }

    @Test @DisplayName("submitFlag_duplicatePost_throws")
    void dupPost() {
        FlagRequest r = FlagRequest.builder().postId(20L).reason("x").build();
        when(flagRepository.existsByUserIdAndPostId(1L, 20L)).thenReturn(true);
        assertThatThrownBy(() -> service.submitFlag(1L, r)).isInstanceOf(IllegalStateException.class);
    }

    @Test @DisplayName("submitFlag_post_success")
    void submit_post_ok() {
        FlagRequest r = FlagRequest.builder().postId(20L).reason("x").build();
        when(flagRepository.existsByUserIdAndPostId(1L, 20L)).thenReturn(false);
        when(flagRepository.save(any())).thenAnswer(i -> { Flag f = i.getArgument(0); f.setId(1L); return f; });
        when(feedPostRepository.findById(20L)).thenReturn(Optional.empty());
        FlagResponse res = service.submitFlag(1L, r);
        assertThat(res.getId()).isEqualTo(1L);
    }

    @Test @DisplayName("getFlagById_notFound_throws")
    void getById_notFound() {
        when(flagRepository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getFlagById(1L)).isInstanceOf(RuntimeException.class);
    }

    @Test @DisplayName("getFlagById_ok")
    void getById_ok() {
        Flag f = Flag.builder().id(1L).userId(1L).reason("r").status("PENDING").build();
        when(flagRepository.findById(1L)).thenReturn(Optional.of(f));
        assertThat(service.getFlagById(1L).getId()).isEqualTo(1L);
    }

    @Test @DisplayName("getPendingFlags_returnsPage")
    void pending() {
        Flag f = Flag.builder().id(1L).userId(1L).reason("r").status("PENDING").build();
        when(flagRepository.findByStatus(eq("PENDING"), any()))
                .thenReturn(new PageImpl<>(List.of(f)));
        Page<FlagResponse> p = service.getPendingFlags(PageRequest.of(0, 10));
        assertThat(p.getTotalElements()).isEqualTo(1);
    }

    @Test @DisplayName("getAllFlags_allStatus_returnsFindAll")
    void allFlags() {
        Flag f = Flag.builder().id(1L).userId(1L).reason("r").status("PENDING").build();
        when(flagRepository.findAll(any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(f)));
        assertThat(service.getAllFlags("ALL", PageRequest.of(0, 10)).getTotalElements()).isEqualTo(1);
    }

    @Test @DisplayName("reviewFlag_invalidStatus_throws")
    void review_invalid() {
        Flag f = Flag.builder().id(1L).userId(1L).reason("r").status("PENDING").build();
        when(flagRepository.findById(1L)).thenReturn(Optional.of(f));
        assertThatThrownBy(() -> service.reviewFlag(1L, 2L, "INVALID"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test @DisplayName("reviewFlag_resolved_completesTask")
    void review_resolved() {
        Flag f = Flag.builder().id(1L).userId(1L).reason("r").status("PENDING").build();
        when(flagRepository.findById(1L)).thenReturn(Optional.of(f));
        when(flagRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        FlagResponse res = service.reviewFlag(1L, 2L, "RESOLVED");
        assertThat(res.getStatus()).isEqualTo("RESOLVED");
        verify(approvalTaskService).completeTask("FLAG", 1L);
    }

    @Test @DisplayName("getFlagsByPostId_returnsPage")
    void byPost() {
        when(flagRepository.findByPostId(eq(20L), any()))
                .thenReturn(new PageImpl<>(List.of()));
        assertThat(service.getFlagsByPostId(20L, PageRequest.of(0, 10)).getTotalElements()).isZero();
    }

    @Test @DisplayName("getFlagsByCampaignId_returnsPage")
    void byCampaign() {
        when(flagRepository.findByCampaignId(eq(10L), any()))
                .thenReturn(new PageImpl<>(List.of()));
        assertThat(service.getFlagsByCampaignId(10L, PageRequest.of(0, 10)).getTotalElements()).isZero();
    }
}
