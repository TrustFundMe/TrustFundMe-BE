package com.trustfund.service.interfaceServices;

import com.trustfund.model.request.CreateFeedPostRequest;
import com.trustfund.model.request.UpdateFeedPostContentRequest;
import com.trustfund.model.request.UpdateFeedPostRequest;
import com.trustfund.model.response.FeedPostResponse;
import com.trustfund.model.response.FeedPostRevisionResponse;

import java.util.List;

public interface FeedPostService {
    FeedPostResponse create(CreateFeedPostRequest request, Long authorId);

    FeedPostResponse getById(Long id, Long currentUserId, String ipAddress);

    org.springframework.data.domain.Page<FeedPostResponse> getActiveFeedPosts(Long currentUserId,
            org.springframework.data.domain.Pageable pageable);

    org.springframework.data.domain.Page<FeedPostResponse> getMyFeedPosts(Long currentUserId, String status,
            org.springframework.data.domain.Pageable pageable);

    org.springframework.data.domain.Page<FeedPostResponse> getPublicPostsByAuthorId(Long authorId,
            org.springframework.data.domain.Pageable pageable);

    FeedPostResponse updateStatus(Long id, Long currentUserId, String status);

    FeedPostResponse updateVisibility(Long id, Long currentUserId, String currentRole, String visibility);

    FeedPostResponse updateContent(Long id, Long currentUserId, UpdateFeedPostContentRequest request);

    FeedPostResponse update(Long id, Long currentUserId, UpdateFeedPostRequest request);

    void delete(Long id, Long currentUserId);

    FeedPostResponse toggleLike(Long postId, Long currentUserId);

    // Admin APIs
    org.springframework.data.domain.Page<FeedPostResponse> getAllFeedPosts(
            org.springframework.data.domain.Pageable pageable);

    void deleteByAdmin(Long id);

    FeedPostResponse togglePin(Long id);

    FeedPostResponse toggleLock(Long id);

    FeedPostResponse updateStatusByAdmin(Long id, String status);

    FeedPostResponse approveByAdmin(Long id);

    FeedPostResponse rejectByAdmin(Long id);

    FeedPostResponse hideByAdmin(Long id);

    FeedPostResponse updateContentByAdmin(Long id, UpdateFeedPostContentRequest request);

    int syncAllCommentCounts();

    List<FeedPostResponse> getByTarget(Long targetId, String targetType);

    // Revision History APIs
    org.springframework.data.domain.Page<FeedPostRevisionResponse> getRevisions(Long postId, Long currentUserId,
            String currentRole, org.springframework.data.domain.Pageable pageable);

    FeedPostRevisionResponse getRevisionById(Long postId, Long revisionId, Long currentUserId, String currentRole);
}
