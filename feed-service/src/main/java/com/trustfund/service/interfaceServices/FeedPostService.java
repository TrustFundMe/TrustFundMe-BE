package com.trustfund.service.interfaceServices;

import com.trustfund.model.request.CreateFeedPostRequest;
import com.trustfund.model.request.UpdateFeedPostContentRequest;
import com.trustfund.model.request.UpdateFeedPostRequest;
import com.trustfund.model.response.FeedPostResponse;

public interface FeedPostService {
    FeedPostResponse create(CreateFeedPostRequest request, Long authorId);

    FeedPostResponse getById(Long id, Long currentUserId, String ipAddress);

    org.springframework.data.domain.Page<FeedPostResponse> getActiveFeedPosts(Long currentUserId, org.springframework.data.domain.Pageable pageable);

    FeedPostResponse updateStatus(Long id, Long currentUserId, String status);

    FeedPostResponse updateVisibility(Long id, Long currentUserId, String currentRole, String visibility);

    FeedPostResponse updateContent(Long id, Long currentUserId, UpdateFeedPostContentRequest request);

    FeedPostResponse update(Long id, Long currentUserId, UpdateFeedPostRequest request);

    void delete(Long id, Long currentUserId);
}
