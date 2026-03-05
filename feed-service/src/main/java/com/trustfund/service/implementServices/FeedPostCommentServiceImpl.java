package com.trustfund.service.implementServices;

import com.trustfund.client.IdentityServiceClient;
import com.trustfund.exception.exceptions.ForbiddenException;
import com.trustfund.exception.exceptions.NotFoundException;
import com.trustfund.model.FeedPost;
import com.trustfund.model.FeedPostComment;
import com.trustfund.model.FeedPostCommentLike;
import com.trustfund.model.request.CreateFeedPostCommentRequest;
import com.trustfund.model.request.UpdateFeedPostCommentRequest;
import com.trustfund.model.response.FeedPostCommentResponse;
import com.trustfund.model.response.UserInfoResponse;
import com.trustfund.repository.FeedPostCommentLikeRepository;
import com.trustfund.repository.FeedPostCommentRepository;
import com.trustfund.repository.FeedPostRepository;
import com.trustfund.service.interfaceServices.FeedPostCommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FeedPostCommentServiceImpl implements FeedPostCommentService {

    private final FeedPostCommentRepository feedPostCommentRepository;
    private final FeedPostRepository feedPostRepository;
    private final FeedPostCommentLikeRepository feedPostCommentLikeRepository;
    private final IdentityServiceClient identityServiceClient;

    @Override
    @Transactional
    public FeedPostCommentResponse create(Long postId, CreateFeedPostCommentRequest request, Long authorId) {
        FeedPost post = feedPostRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("Feed post not found"));

        if (request.getParentCommentId() != null) {
            feedPostCommentRepository.findById(request.getParentCommentId())
                    .orElseThrow(() -> new NotFoundException("Parent comment not found"));
        }

        FeedPostComment comment = FeedPostComment.builder()
                .postId(postId)
                .userId(authorId)
                .content(request.getContent())
                .parentCommentId(request.getParentCommentId())
                .likeCount(0)
                .build();

        feedPostCommentRepository.save(comment);

        int newCount = (post.getCommentCount() == null ? 0 : post.getCommentCount()) + 1;
        post.setCommentCount(newCount);
        post.setReplyCount(newCount);
        feedPostRepository.save(post);

        return toResponse(comment, authorId);
    }

    @Override
    public Page<FeedPostCommentResponse> getCommentsByPostId(Long postId, Long currentUserId, Pageable pageable) {
        if (!feedPostRepository.existsById(postId)) {
            throw new NotFoundException("Feed post not found");
        }
        Page<FeedPostComment> rootComments = feedPostCommentRepository
                .findByPostIdAndParentCommentIdIsNullOrderByCreatedAtDesc(postId, pageable);

        return rootComments.map(comment -> {
            FeedPostCommentResponse response = toResponse(comment, currentUserId);
            List<FeedPostComment> replyEntities = feedPostCommentRepository
                    .findByParentCommentIdOrderByCreatedAtAsc(comment.getId());
            List<FeedPostCommentResponse> replies = replyEntities.stream()
                    .map(r -> toResponse(r, currentUserId))
                    .collect(Collectors.toList());
            response.setReplies(replies);
            return response;
        });
    }

    @Override
    @Transactional
    public FeedPostCommentResponse update(Long commentId, Long currentUserId, UpdateFeedPostCommentRequest request) {
        FeedPostComment comment = feedPostCommentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Comment not found"));

        if (!comment.getUserId().equals(currentUserId)) {
            throw new ForbiddenException("Not allowed to edit this comment");
        }

        comment.setContent(request.getContent());
        feedPostCommentRepository.save(comment);

        return toResponse(comment, currentUserId);
    }

    @Override
    @Transactional
    public void delete(Long commentId, Long currentUserId) {
        FeedPostComment comment = feedPostCommentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Comment not found"));

        if (!comment.getUserId().equals(currentUserId)) {
            FeedPost post = feedPostRepository.findById(comment.getPostId())
                    .orElseThrow(() -> new NotFoundException("Feed post not found"));
            if (!post.getAuthorId().equals(currentUserId)) {
                throw new ForbiddenException("Not allowed to delete this comment");
            }
        }

        FeedPost post = feedPostRepository.findById(comment.getPostId()).orElse(null);

        long replyCount = feedPostCommentRepository.findByParentCommentIdOrderByCreatedAtAsc(commentId).size();
        feedPostCommentLikeRepository.deleteByCommentId(commentId);
        feedPostCommentRepository.delete(comment);

        if (post != null) {
            int totalDeleted = (int) (1 + replyCount);
            int newCount = Math.max(0, (post.getCommentCount() != null ? post.getCommentCount() : 0) - totalDeleted);
            post.setCommentCount(newCount);
            post.setReplyCount(newCount);
            feedPostRepository.save(post);
        }
    }

    @Override
    @Transactional
    public FeedPostCommentResponse toggleLike(Long commentId, Long currentUserId) {
        FeedPostComment comment = feedPostCommentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Comment not found"));

        boolean alreadyLiked = feedPostCommentLikeRepository.existsByCommentIdAndUserId(commentId, currentUserId);

        if (alreadyLiked) {
            feedPostCommentLikeRepository.findByCommentIdAndUserId(commentId, currentUserId)
                    .ifPresent(feedPostCommentLikeRepository::delete);
            comment.setLikeCount(Math.max(0, comment.getLikeCount() - 1));
        } else {
            feedPostCommentLikeRepository.save(FeedPostCommentLike.builder()
                    .commentId(commentId)
                    .userId(currentUserId)
                    .build());
            comment.setLikeCount(comment.getLikeCount() + 1);
        }
        feedPostCommentRepository.save(comment);

        FeedPostCommentResponse response = toResponse(comment, currentUserId);
        response.setIsLiked(!alreadyLiked);
        return response;
    }

    private FeedPostCommentResponse toResponse(FeedPostComment comment, Long currentUserId) {
        String authorName = "Thành viên #" + comment.getUserId();
        String authorAvatar = null;
        try {
            UserInfoResponse userInfo = identityServiceClient.getUserInfo(comment.getUserId());
            if (userInfo != null) {
                if (userInfo.getFullName() != null && !userInfo.getFullName().isBlank()) {
                    authorName = userInfo.getFullName();
                }
                authorAvatar = userInfo.getAvatarUrl();
            }
        } catch (Exception e) {
            // Keep default values if identity-service is unavailable
        }

        boolean isLiked = currentUserId != null &&
                feedPostCommentLikeRepository.existsByCommentIdAndUserId(comment.getId(), currentUserId);

        return FeedPostCommentResponse.builder()
                .id(comment.getId())
                .postId(comment.getPostId())
                .userId(comment.getUserId())
                .parentCommentId(comment.getParentCommentId())
                .content(comment.getContent())
                .likeCount(comment.getLikeCount() != null ? comment.getLikeCount() : 0)
                .isLiked(isLiked)
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .authorName(authorName)
                .authorAvatar(authorAvatar)
                .build();
    }
}
