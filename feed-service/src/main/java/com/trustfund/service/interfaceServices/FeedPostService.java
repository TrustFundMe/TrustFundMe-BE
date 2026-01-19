package com.trustfund.service.interfaceServices;

import com.trustfund.model.request.CreateFeedPostRequest;
import com.trustfund.model.response.FeedPostResponse;

public interface FeedPostService {
    FeedPostResponse create(CreateFeedPostRequest request, Long authorId);

    FeedPostResponse getById(Long id, Long currentUserId);
}
