package com.trustfund.service.implementServices;

import com.trustfund.client.UserInfoClient;
import com.trustfund.model.FeedPost;
import com.trustfund.model.ForumAttachment;
import com.trustfund.model.request.CreateFeedPostRequest;
import com.trustfund.model.request.UpdateFeedPostContentRequest;
import com.trustfund.model.request.UpdateFeedPostRequest;
import com.trustfund.model.response.FeedPostResponse;
import com.trustfund.model.response.ForumAttachmentResponse;
import com.trustfund.repository.FeedPostRepository;
import com.trustfund.repository.FeedPostLikeRepository;
import com.trustfund.repository.FeedPostCommentRepository;
import com.trustfund.repository.ForumAttachmentRepository;
import com.trustfund.service.interfaceServices.FeedPostService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class FeedPostServiceImpl implements FeedPostService {

    private final FeedPostRepository feedPostRepository;
    private final ForumAttachmentRepository forumAttachmentRepository;
    private final FeedPostLikeRepository feedPostLikeRepository;
    private final FeedPostCommentRepository feedPostCommentRepository;
    private final org.springframework.cache.CacheManager cacheManager;
    private final UserInfoClient userInfoClient;

    @Override
    public FeedPostResponse create(CreateFeedPostRequest request, Long authorId) {
        FeedPost feedPost = FeedPost.builder()
                .budgetId(request.getBudgetId())
                .authorId(authorId)
                .type(request.getType())
                .visibility(request.getVisibility())
                .title(request.getTitle())
                .content(request.getContent())
                .status(request.getStatus() == null || request.getStatus().isBlank() ? "DRAFT" : request.getStatus())
                .build();

        FeedPost saved = feedPostRepository.save(feedPost);
        if (request.getAttachments() != null && !request.getAttachments().isEmpty()) {
            int order = 0;
            for (com.trustfund.model.request.AttachmentInput att : request.getAttachments()) {
                ForumAttachment forumAtt = ForumAttachment.builder()
                        .postId(saved.getId())
                        .type(att.getType() != null && !att.getType().isBlank() ? att.getType() : "IMAGE")
                        .url(att.getUrl())
                        .displayOrder(order++)
                        .build();
                forumAttachmentRepository.save(forumAtt);
            }
        }
        return toResponse(saved, authorId);
    }

    @Override
    public FeedPostResponse getById(Long id, Long currentUserId, String ipAddress) {
        FeedPost post = feedPostRepository.findById(id)
                .orElseThrow(() -> new com.trustfund.exception.exceptions.NotFoundException("Feed post not found"));

        String visibility = post.getVisibility();
        if (visibility == null || visibility.isBlank()) {
            throw new com.trustfund.exception.exceptions.BadRequestException("Invalid visibility");
        }

        // View count logic
        incrementViewCountIfEligible(id, currentUserId, ipAddress);

        if (visibility.equals("PUBLIC")) {
            return toResponse(post, currentUserId);
        }

        if (currentUserId == null) {
            throw new com.trustfund.exception.exceptions.UnauthorizedException("Authentication required");
        }

        if (visibility.equals("PRIVATE")) {
            if (!currentUserId.equals(post.getAuthorId())) {
                throw new com.trustfund.exception.exceptions.ForbiddenException("Not allowed to view this feed post");
            }
            return toResponse(post, currentUserId);
        }

        if (visibility.equals("FOLLOWERS")) {
            // TODO: check follow status
            return toResponse(post, currentUserId);
        }

        throw new com.trustfund.exception.exceptions.BadRequestException("Invalid visibility");
    }

    private void incrementViewCountIfEligible(Long postId, Long userId, String ipAddress) {
        String key = "view:" + postId + ":" + (userId != null ? "u" + userId : "ip" + ipAddress);
        org.springframework.cache.Cache cache = cacheManager.getCache("postValidationViews");

        if (cache != null && cache.get(key) == null) {
            feedPostRepository.incrementViewCount(postId);
            cache.put(key, true);
        }
    }

    @Override
    public org.springframework.data.domain.Page<FeedPostResponse> getActiveFeedPosts(Long currentUserId,
            org.springframework.data.domain.Pageable pageable) {
        return feedPostRepository.findVisibleActivePosts(currentUserId, pageable)
                .map(post -> toResponse(post, currentUserId));
    }

    @Override
    public FeedPostResponse updateStatus(Long id, Long currentUserId, String status) {
        FeedPost post = feedPostRepository.findById(id)
                .orElseThrow(() -> new com.trustfund.exception.exceptions.NotFoundException("Feed post not found"));

        if (currentUserId == null) {
            throw new com.trustfund.exception.exceptions.UnauthorizedException("Authentication required");
        }

        if (!currentUserId.equals(post.getAuthorId())) {
            throw new com.trustfund.exception.exceptions.ForbiddenException("Not allowed to update this feed post");
        }

        if (status == null || status.isBlank()) {
            throw new com.trustfund.exception.exceptions.BadRequestException("Status is required");
        }

        if (!status.equals("DRAFT") && !status.equals("ACTIVE")) {
            throw new com.trustfund.exception.exceptions.BadRequestException("Invalid status");
        }

        post.setStatus(status);
        FeedPost saved = feedPostRepository.save(post);
        return toResponse(saved, currentUserId);
    }

    @Override
    public FeedPostResponse updateVisibility(Long id, Long currentUserId, String currentRole, String visibility) {
        FeedPost post = feedPostRepository.findById(id)
                .orElseThrow(() -> new com.trustfund.exception.exceptions.NotFoundException("Feed post not found"));

        if (currentUserId == null) {
            throw new com.trustfund.exception.exceptions.UnauthorizedException("Authentication required");
        }

        String role = currentRole;
        if (role != null && role.startsWith("ROLE_")) {
            role = role.substring("ROLE_".length());
        }

        boolean isAuthor = currentUserId.equals(post.getAuthorId());
        boolean isStaff = role != null && role.equals("STAFF");
        boolean isAdmin = role != null && role.equals("ADMIN");

        if (!isAuthor && !isStaff && !isAdmin) {
            throw new com.trustfund.exception.exceptions.ForbiddenException("Not allowed to update this feed post");
        }

        if (visibility == null || visibility.isBlank()) {
            throw new com.trustfund.exception.exceptions.BadRequestException("Visibility is required");
        }

        if (!visibility.equals("PUBLIC") && !visibility.equals("PRIVATE") && !visibility.equals("FOLLOWERS")) {
            throw new com.trustfund.exception.exceptions.BadRequestException("Invalid visibility");
        }

        post.setVisibility(visibility);
        FeedPost saved = feedPostRepository.save(post);
        return toResponse(saved, currentUserId);
    }

    @Override
    public FeedPostResponse updateContent(Long id, Long currentUserId, UpdateFeedPostContentRequest request) {
        FeedPost post = feedPostRepository.findById(id)
                .orElseThrow(() -> new com.trustfund.exception.exceptions.NotFoundException("Feed post not found"));

        if (currentUserId == null) {
            throw new com.trustfund.exception.exceptions.UnauthorizedException("Authentication required");
        }

        if (!currentUserId.equals(post.getAuthorId())) {
            throw new com.trustfund.exception.exceptions.ForbiddenException("Not allowed to update this feed post");
        }

        boolean hasTitle = request.getTitle() != null && !request.getTitle().isBlank();
        boolean hasContent = request.getContent() != null && !request.getContent().isBlank();

        if (!hasTitle && !hasContent) {
            throw new com.trustfund.exception.exceptions.BadRequestException("Nothing to update");
        }

        if (request.getTitle() != null) {
            post.setTitle(request.getTitle());
        }
        if (request.getContent() != null) {
            post.setContent(request.getContent());
        }

        FeedPost saved = feedPostRepository.save(post);
        return toResponse(saved, currentUserId);
    }

    @Override
    public FeedPostResponse update(Long id, Long currentUserId, UpdateFeedPostRequest request) {
        FeedPost post = feedPostRepository.findById(id)
                .orElseThrow(() -> new com.trustfund.exception.exceptions.NotFoundException("Feed post not found"));

        if (currentUserId == null) {
            throw new com.trustfund.exception.exceptions.UnauthorizedException("Authentication required");
        }

        if (!currentUserId.equals(post.getAuthorId())) {
            throw new com.trustfund.exception.exceptions.ForbiddenException("Not allowed to update this feed post");
        }

        if (request.getTitle() != null) {
            post.setTitle(request.getTitle());
        }
        if (request.getContent() != null) {
            post.setContent(request.getContent());
        }
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            post.setStatus(request.getStatus());
        }
        if (request.getBudgetId() != null) {
            post.setBudgetId(request.getBudgetId());
        } else {
            post.setBudgetId(null);
        }

        FeedPost saved = feedPostRepository.save(post);

        forumAttachmentRepository.deleteByPostId(id);

        if (request.getAttachments() != null && !request.getAttachments().isEmpty()) {
            Set<String> seenUrls = new HashSet<>();
            int order = 0;
            for (com.trustfund.model.request.AttachmentInput att : request.getAttachments()) {
                if (att.getUrl() == null || att.getUrl().isBlank()) continue;
                if (seenUrls.contains(att.getUrl())) continue;
                seenUrls.add(att.getUrl());
                ForumAttachment forumAtt = ForumAttachment.builder()
                        .postId(saved.getId())
                        .type(att.getType() != null && !att.getType().isBlank() ? att.getType() : "IMAGE")
                        .url(att.getUrl())
                        .displayOrder(order++)
                        .build();
                forumAttachmentRepository.save(forumAtt);
            }
        }

        return toResponse(saved, currentUserId);
    }

    @Override
    public void delete(Long id, Long currentUserId) {
        FeedPost post = feedPostRepository.findById(id)
                .orElseThrow(() -> new com.trustfund.exception.exceptions.NotFoundException("Feed post not found"));
        if (currentUserId == null) {
            throw new com.trustfund.exception.exceptions.UnauthorizedException("Authentication required");
        }
        if (!currentUserId.equals(post.getAuthorId())) {
            throw new com.trustfund.exception.exceptions.ForbiddenException("Not allowed to delete this feed post");
        }
        forumAttachmentRepository.deleteAll(forumAttachmentRepository.findByPostIdOrderByDisplayOrderAsc(id));
        feedPostRepository.delete(post);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public FeedPostResponse toggleLike(Long postId, Long currentUserId) {
        FeedPost post = feedPostRepository.findById(postId)
                .orElseThrow(() -> new com.trustfund.exception.exceptions.NotFoundException("Feed post not found"));
        
        if (currentUserId == null) {
            throw new com.trustfund.exception.exceptions.UnauthorizedException("Authentication required");
        }

        boolean exists = feedPostLikeRepository.existsByPostIdAndUserId(postId, currentUserId);
        if (exists) {
            feedPostLikeRepository.deleteByPostIdAndUserId(postId, currentUserId);
            if (post.getLikeCount() != null && post.getLikeCount() > 0) {
                post.setLikeCount(post.getLikeCount() - 1);
            } else {
                post.setLikeCount(0); // Ensure it doesn't go negative
            }
        } else {
            post.setLikeCount(post.getLikeCount() == null ? 1 : post.getLikeCount() + 1);
            com.trustfund.model.FeedPostLike newLike = com.trustfund.model.FeedPostLike.builder()
                    .postId(postId)
                    .userId(currentUserId)
                    .build();
            feedPostLikeRepository.save(newLike);
        }

        feedPostRepository.save(post);
        return toResponse(post, currentUserId);
    }

    @Override
    public org.springframework.data.domain.Page<FeedPostResponse> getAllFeedPosts(org.springframework.data.domain.Pageable pageable) {
        org.springframework.data.domain.Page<FeedPost> posts = feedPostRepository.findAll(pageable);
        return posts.map(post -> toResponse(post, null));
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void deleteByAdmin(Long id) {
        FeedPost post = feedPostRepository.findById(id)
                .orElseThrow(() -> new com.trustfund.exception.exceptions.NotFoundException("Feed post not found"));

        // Delete attachments
        forumAttachmentRepository.deleteByPostId(id);

        // Delete likes
        feedPostLikeRepository.deleteByPostId(id);

        // Delete comments
        feedPostCommentRepository.deleteByPostId(id);

        feedPostRepository.delete(post);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public int syncAllCommentCounts() {
        java.util.List<FeedPost> all = feedPostRepository.findAll();
        int fixed = 0;
        for (FeedPost post : all) {
            int actual = feedPostCommentRepository.countByPostId(post.getId());
            if (!Integer.valueOf(actual).equals(post.getCommentCount()) ||
                !Integer.valueOf(actual).equals(post.getReplyCount())) {
                post.setCommentCount(actual);
                post.setReplyCount(actual);
                feedPostRepository.save(post);
                fixed++;
            }
        }
        return fixed;
    }

    @Override
    public FeedPostResponse togglePin(Long id) {
        FeedPost post = feedPostRepository.findById(id)
                .orElseThrow(() -> new com.trustfund.exception.exceptions.NotFoundException("Feed post not found"));
        post.setIsPinned(post.getIsPinned() == null ? true : !post.getIsPinned());
        feedPostRepository.save(post);
        return toResponse(post, null);
    }

    @Override
    public FeedPostResponse toggleLock(Long id) {
        FeedPost post = feedPostRepository.findById(id)
                .orElseThrow(() -> new com.trustfund.exception.exceptions.NotFoundException("Feed post not found"));
        post.setIsLocked(post.getIsLocked() == null ? true : !post.getIsLocked());
        feedPostRepository.save(post);
        return toResponse(post, null);
    }

    @Override
    public FeedPostResponse updateStatusByAdmin(Long id, String status) {
        FeedPost post = feedPostRepository.findById(id)
                .orElseThrow(() -> new com.trustfund.exception.exceptions.NotFoundException("Feed post not found"));
        if (status == null || status.isBlank()) {
            throw new com.trustfund.exception.exceptions.BadRequestException("Status is required");
        }
        if (!status.equals("DRAFT") && !status.equals("ACTIVE")) {
            throw new com.trustfund.exception.exceptions.BadRequestException("Invalid status");
        }
        post.setStatus(status);
        feedPostRepository.save(post);
        return toResponse(post, null);
    }

    private FeedPostResponse toResponse(FeedPost entity, Long currentUserId) {
        java.util.List<ForumAttachment> attachments = forumAttachmentRepository
                .findByPostIdOrderByDisplayOrderAsc(entity.getId());
        Set<String> seenUrls = new HashSet<>();
        java.util.List<ForumAttachmentResponse> attachmentResponses = attachments.stream()
                .filter(att -> {
                    if (att.getUrl() == null) return false;
                    if (seenUrls.contains(att.getUrl())) return false;
                    seenUrls.add(att.getUrl());
                    return true;
                })
                .map(att -> ForumAttachmentResponse.builder()
                        .id(att.getId())
                        .type(att.getType())
                        .url(att.getUrl())
                        .fileName(att.getFileName())
                        .fileSize(att.getFileSize())
                        .mimeType(att.getMimeType())
                        .displayOrder(att.getDisplayOrder())
                        .build())
                .collect(java.util.stream.Collectors.toList());

        boolean isLiked = false;
        if (currentUserId != null) {
            isLiked = feedPostLikeRepository.existsByPostIdAndUserId(entity.getId(), currentUserId);
        }

        UserInfoClient.UserInfo authorInfo = userInfoClient.getUserInfo(entity.getAuthorId());

        return FeedPostResponse.builder()
                .id(entity.getId())
                .budgetId(entity.getBudgetId())
                .authorId(entity.getAuthorId())
                .authorName(authorInfo.fullName())
                .authorAvatar(authorInfo.avatarUrl())
                .type(entity.getType())
                .visibility(entity.getVisibility())
                .title(entity.getTitle())
                .content(entity.getContent())
                .status(entity.getStatus())
                .categoryId(entity.getCategoryId())
                .parentPostId(entity.getParentPostId())
                .replyCount(entity.getReplyCount())
                .viewCount(entity.getViewCount())
                .likeCount(entity.getLikeCount())
                .commentCount(entity.getCommentCount())
                .isLiked(isLiked)
                .isPinned(entity.getIsPinned())
                .isLocked(entity.getIsLocked())
                .attachments(attachmentResponses)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
