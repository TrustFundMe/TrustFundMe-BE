package com.trustfund.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trustfund.client.MediaServiceClient;
import com.trustfund.client.UserInfoClient;
import com.trustfund.exception.exceptions.BadRequestException;
import com.trustfund.exception.exceptions.ForbiddenException;
import com.trustfund.exception.exceptions.NotFoundException;
import com.trustfund.exception.exceptions.UnauthorizedException;
import com.trustfund.model.FeedPost;
import com.trustfund.model.request.CreateFeedPostRequest;
import com.trustfund.model.request.UpdateFeedPostContentRequest;
import com.trustfund.model.response.FeedPostResponse;
import com.trustfund.repository.*;
import com.trustfund.service.implementServices.FeedPostServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeedPostServiceImplTest {

    @Mock private FeedPostRepository feedPostRepository;
    @Mock private MediaServiceClient mediaServiceClient;
    @Mock private FlagRepository flagRepository;
    @Mock private FeedPostLikeRepository feedPostLikeRepository;
    @Mock private FeedPostCommentRepository feedPostCommentRepository;
    @Mock private CacheManager cacheManager;
    @Mock private UserPostSeenRepository userPostSeenRepository;
    @Mock private UserInfoClient userInfoClient;
    @Mock private ExpenditureRepository expenditureRepository;
    @Mock private CampaignRepository campaignRepository;
    @Mock private FeedPostRevisionRepository feedPostRevisionRepository;
    @Mock private ObjectMapper objectMapper;

    @InjectMocks private FeedPostServiceImpl service;

    private FeedPost post;

    @BeforeEach
    void setUp() {
        post = FeedPost.builder().id(1L).authorId(10L).visibility("PUBLIC")
                .title("T").content("C").status("PUBLISHED").likeCount(0).build();
        lenient().when(mediaServiceClient.getMediaByPostId(any())).thenReturn(List.of());
        lenient().when(userInfoClient.getUserInfo(any())).thenReturn(new UserInfoClient.UserInfo("U", null));
    }

    @Test @DisplayName("create_setsDraftStatusByDefault")
    void create_default() {
        CreateFeedPostRequest req = new CreateFeedPostRequest();
        req.setVisibility("PUBLIC"); req.setTitle("T"); req.setContent("C");
        when(feedPostRepository.save(any())).thenAnswer(i -> { FeedPost p = i.getArgument(0); p.setId(1L); return p; });
        FeedPostResponse r = service.create(req, 10L);
        assertThat(r.getStatus()).isEqualTo("DRAFT");
    }

    @Test @DisplayName("getById_notFound_throws")
    void getById_notFound() {
        when(feedPostRepository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getById(1L, 10L, "ip")).isInstanceOf(NotFoundException.class);
    }

    @Test @DisplayName("getById_lockedNonAuthor_throws")
    void getById_locked() {
        post.setIsLocked(true);
        when(feedPostRepository.findById(1L)).thenReturn(Optional.of(post));
        assertThatThrownBy(() -> service.getById(1L, 99L, "ip")).isInstanceOf(ForbiddenException.class);
    }

    @Test @DisplayName("getById_publicPost_ok")
    void getById_public() {
        when(feedPostRepository.findById(1L)).thenReturn(Optional.of(post));
        when(userPostSeenRepository.existsByUserIdAndPostId(any(), any())).thenReturn(true);
        FeedPostResponse r = service.getById(1L, 10L, "ip");
        assertThat(r.getId()).isEqualTo(1L);
    }

    @Test @DisplayName("getById_privateNonAuthor_throws")
    void getById_privateNonAuthor() {
        post.setVisibility("PRIVATE");
        when(feedPostRepository.findById(1L)).thenReturn(Optional.of(post));
        when(userPostSeenRepository.existsByUserIdAndPostId(any(), any())).thenReturn(true);
        assertThatThrownBy(() -> service.getById(1L, 99L, "ip")).isInstanceOf(ForbiddenException.class);
    }

    @Test @DisplayName("updateStatus_notAuthor_throws")
    void updateStatus_notAuthor() {
        when(feedPostRepository.findById(1L)).thenReturn(Optional.of(post));
        assertThatThrownBy(() -> service.updateStatus(1L, 99L, "PUBLISHED")).isInstanceOf(ForbiddenException.class);
    }

    @Test @DisplayName("updateStatus_invalidStatus_throws")
    void updateStatus_invalid() {
        when(feedPostRepository.findById(1L)).thenReturn(Optional.of(post));
        assertThatThrownBy(() -> service.updateStatus(1L, 10L, "WEIRD")).isInstanceOf(BadRequestException.class);
    }

    @Test @DisplayName("updateStatus_unauth_throws")
    void updateStatus_unauth() {
        when(feedPostRepository.findById(1L)).thenReturn(Optional.of(post));
        assertThatThrownBy(() -> service.updateStatus(1L, null, "PUBLISHED")).isInstanceOf(UnauthorizedException.class);
    }

    @Test @DisplayName("updateStatus_ok")
    void updateStatus_ok() {
        when(feedPostRepository.findById(1L)).thenReturn(Optional.of(post));
        when(feedPostRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        FeedPostResponse r = service.updateStatus(1L, 10L, "HIDDEN");
        assertThat(r.getStatus()).isEqualTo("HIDDEN");
    }

    @Test @DisplayName("delete_notAuthor_throws")
    void delete_notAuthor() {
        when(feedPostRepository.findById(1L)).thenReturn(Optional.of(post));
        assertThatThrownBy(() -> service.delete(1L, 99L)).isInstanceOf(ForbiddenException.class);
    }

    @Test @DisplayName("delete_ok")
    void delete_ok() {
        when(feedPostRepository.findById(1L)).thenReturn(Optional.of(post));
        service.delete(1L, 10L);
        verify(feedPostRepository).delete(post);
    }

    @Test @DisplayName("toggleLike_lockedPost_throws")
    void toggleLike_locked() {
        post.setIsLocked(true);
        when(feedPostRepository.findById(1L)).thenReturn(Optional.of(post));
        assertThatThrownBy(() -> service.toggleLike(1L, 10L)).isInstanceOf(ForbiddenException.class);
    }

    @Test @DisplayName("toggleLike_addsLike")
    void toggleLike_add() {
        when(feedPostRepository.findById(1L)).thenReturn(Optional.of(post));
        when(feedPostLikeRepository.existsByPostIdAndUserId(1L, 10L)).thenReturn(false);
        when(feedPostRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        service.toggleLike(1L, 10L);
        verify(feedPostLikeRepository).save(any());
    }

    @Test @DisplayName("hideByAdmin_setsHiddenAndLocked")
    void hideByAdmin() {
        when(feedPostRepository.findById(1L)).thenReturn(Optional.of(post));
        when(feedPostRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        FeedPostResponse r = service.hideByAdmin(1L);
        assertThat(r.getStatus()).isEqualTo("HIDDEN");
        assertThat(post.getIsLocked()).isTrue();
    }

    @Test @DisplayName("updateStatusByAdmin_invalidStatus_throws")
    void updateStatusByAdmin_invalid() {
        when(feedPostRepository.findById(1L)).thenReturn(Optional.of(post));
        assertThatThrownBy(() -> service.updateStatusByAdmin(1L, "WHATEVER")).isInstanceOf(BadRequestException.class);
    }

    @Test @DisplayName("updateContent_nothingToUpdate_throws")
    void updateContent_empty() {
        when(feedPostRepository.findById(1L)).thenReturn(Optional.of(post));
        UpdateFeedPostContentRequest req = new UpdateFeedPostContentRequest();
        assertThatThrownBy(() -> service.updateContent(1L, 10L, req)).isInstanceOf(BadRequestException.class);
    }
}
