package com.trustfund.service.interfaceServices;

import com.trustfund.model.request.CreateFeedPostCommentRequest;
import com.trustfund.model.request.UpdateFeedPostCommentRequest;
import com.trustfund.model.response.FeedPostCommentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FeedPostCommentService {
    FeedPostCommentResponse create(Long postId, CreateFeedPostCommentRequest request, Long authorId);

    Page<FeedPostCommentResponse> getCommentsByPostId(Long postId, Long currentUserId, Pageable pageable);

    FeedPostCommentResponse update(Long commentId, Long currentUserId, UpdateFeedPostCommentRequest request);

    void delete(Long commentId, Long currentUserId);

    FeedPostCommentResponse toggleLike(Long commentId, Long currentUserId);

    Page<FeedPostCommentResponse> getByUserId(Long userId, Pageable pageable);
}
