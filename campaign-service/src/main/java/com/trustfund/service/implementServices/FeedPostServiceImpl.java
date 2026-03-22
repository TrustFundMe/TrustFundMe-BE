package com.trustfund.service.implementServices;

import com.trustfund.client.UserInfoClient;
import com.trustfund.client.MediaServiceClient;
import com.trustfund.model.Campaign;
import com.trustfund.model.Expenditure;
import com.trustfund.model.FeedPost;
import com.trustfund.model.request.CreateFeedPostRequest;
import com.trustfund.model.request.UpdateFeedPostContentRequest;
import com.trustfund.model.request.UpdateFeedPostRequest;
import com.trustfund.model.response.FeedPostResponse;
import com.trustfund.model.response.ForumAttachmentResponse;
import com.trustfund.repository.FlagRepository;
import com.trustfund.repository.FeedPostRepository;
import com.trustfund.repository.FeedPostLikeRepository;
import com.trustfund.repository.FeedPostCommentRepository;
import com.trustfund.repository.ForumCategoryRepository;
import com.trustfund.service.interfaceServices.FeedPostService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class FeedPostServiceImpl implements FeedPostService {

    private final FeedPostRepository feedPostRepository;
    private final MediaServiceClient mediaServiceClient;
    private final ForumCategoryRepository forumCategoryRepository;
    private final FlagRepository flagRepository;
    private final FeedPostLikeRepository feedPostLikeRepository;
    private final FeedPostCommentRepository feedPostCommentRepository;
    private final org.springframework.cache.CacheManager cacheManager;
    private final UserInfoClient userInfoClient;
    private final com.trustfund.repository.ExpenditureRepository expenditureRepository;
    private final com.trustfund.repository.CampaignRepository campaignRepository;

    @Override
    public FeedPostResponse create(CreateFeedPostRequest request, Long authorId) {
        FeedPost feedPost = FeedPost.builder()
                .targetId(request.getTargetId())
                .targetType(request.getTargetType())
                .authorId(authorId)
                .visibility(request.getVisibility())
                .title(request.getTitle())
                .content(request.getContent())
                .status(request.getStatus() == null || request.getStatus().isBlank() ? "DRAFT" : request.getStatus())
                .categoryId(request.getCategoryId())
                .build();

        FeedPost saved = feedPostRepository.save(feedPost);
        return toResponse(saved, authorId, null);
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
            return toResponse(post, currentUserId, null);
        }

        if (currentUserId == null) {
            throw new com.trustfund.exception.exceptions.UnauthorizedException("Authentication required");
        }

        if (visibility.equals("PRIVATE")) {
            if (!currentUserId.equals(post.getAuthorId())) {
                throw new com.trustfund.exception.exceptions.ForbiddenException("Not allowed to view this feed post");
            }
            return toResponse(post, currentUserId, null);
        }

        if (visibility.equals("FOLLOWERS")) {
            // TODO: check follow status
            return toResponse(post, currentUserId, null);
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
                .map(post -> toResponse(post, currentUserId, null));
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

        if (!status.equals("DRAFT") && !status.equals("PUBLISHED")) {
            throw new com.trustfund.exception.exceptions.BadRequestException("Invalid status");
        }

        post.setStatus(status);
        FeedPost saved = feedPostRepository.save(post);
        return toResponse(saved, currentUserId, null);
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
        return toResponse(saved, currentUserId, null);
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
        return toResponse(saved, currentUserId, null);
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
        if (request.getTargetId() != null) {
            post.setTargetId(request.getTargetId());
        } else {
            post.setTargetId(null);
        }
        if (request.getTargetType() != null) {
            post.setTargetType(request.getTargetType());
        } else {
            post.setTargetType(null);
        }

        if (request.getCategoryId() != null) {
            post.setCategoryId(request.getCategoryId());
        } else {
            post.setCategoryId(null);
        }

        FeedPost saved = feedPostRepository.save(post);

        return toResponse(saved, currentUserId, null);
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
        return toResponse(post, currentUserId, null);
    }

    @Override
    public org.springframework.data.domain.Page<FeedPostResponse> getAllFeedPosts(org.springframework.data.domain.Pageable pageable) {
        org.springframework.data.domain.Page<FeedPost> posts = feedPostRepository.findAll(pageable);
        java.util.List<Long> postIds = posts.getContent().stream()
                .map(FeedPost::getId)
                .collect(java.util.stream.Collectors.toList());

        java.util.Map<Long, Integer> flagCountByPostId = new java.util.HashMap<>();
        if (!postIds.isEmpty()) {
            java.util.List<Object[]> rows = flagRepository.countPendingFlagsByPostIds(postIds, "PENDING");
            for (Object[] row : rows) {
                if (row == null || row.length < 2) continue;
                Long postId = (Long) row[0];
                Long count = (Long) row[1];
                flagCountByPostId.put(postId, count != null ? count.intValue() : 0);
            }
        }

        return posts.map(post -> toResponse(post, null, flagCountByPostId.get(post.getId())));
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void deleteByAdmin(Long id) {
        FeedPost post = feedPostRepository.findById(id)
                .orElseThrow(() -> new com.trustfund.exception.exceptions.NotFoundException("Feed post not found"));

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
        return toResponse(post, null, null);
    }

    @Override
    public FeedPostResponse toggleLock(Long id) {
        FeedPost post = feedPostRepository.findById(id)
                .orElseThrow(() -> new com.trustfund.exception.exceptions.NotFoundException("Feed post not found"));
        post.setIsLocked(post.getIsLocked() == null ? true : !post.getIsLocked());
        feedPostRepository.save(post);
        return toResponse(post, null, null);
    }

    @Override
    public FeedPostResponse updateStatusByAdmin(Long id, String status) {
        FeedPost post = feedPostRepository.findById(id)
                .orElseThrow(() -> new com.trustfund.exception.exceptions.NotFoundException("Feed post not found"));
        if (status == null || status.isBlank()) {
            throw new com.trustfund.exception.exceptions.BadRequestException("Status is required");
        }
        if (!status.equals("DRAFT") && !status.equals("PUBLISHED")) {
            throw new com.trustfund.exception.exceptions.BadRequestException("Invalid status");
        }
        post.setStatus(status);
        feedPostRepository.save(post);
        return toResponse(post, null, null);
    }

    private FeedPostResponse toResponse(FeedPost entity, Long currentUserId, Integer flagCount) {
        Set<String> seenUrls = new HashSet<>();

        // Resolve targetName based on targetType
        String targetName = null;
        if (entity.getTargetId() != null && entity.getTargetType() != null) {
            if (entity.getTargetType().equals("EXPENDITURE")) {
                targetName = expenditureRepository.findById(entity.getTargetId())
                        .map(Expenditure::getPlan)
                        .orElse(null);
            } else if (entity.getTargetType().equals("CAMPAIGN")) {
                targetName = campaignRepository.findById(entity.getTargetId())
                        .map(Campaign::getTitle)
                        .orElse(null);
            }
        }

        String categoryName = null;
        if (entity.getCategoryId() != null) {
            categoryName = forumCategoryRepository.findById(entity.getCategoryId())
                    .map(com.trustfund.model.ForumCategory::getName)
                    .orElse(null);
        }

        java.util.List<java.util.Map<String, Object>> mediaList =
                mediaServiceClient.getMediaByPostId(entity.getId());

        java.util.List<ForumAttachmentResponse> attachmentResponses = new java.util.ArrayList<>();
        int order = 0;
        for (java.util.Map<String, Object> media : mediaList) {
            Object urlObj = media.get("url");
            if (!(urlObj instanceof String) || ((String) urlObj).isBlank()) continue;
            String url = (String) urlObj;

            if (seenUrls.contains(url)) continue;
            seenUrls.add(url);

            Object idObj = media.get("id");
            Long mediaId = idObj instanceof Number ? ((Number) idObj).longValue() : null;

            Object typeObj = media.get("mediaType");
            String mediaType = typeObj instanceof String ? (String) typeObj : null;
            String attachmentType = "PHOTO".equalsIgnoreCase(mediaType) ? "IMAGE" : "FILE";

            Object fileNameObj = media.get("fileName");
            String fileName = fileNameObj instanceof String ? (String) fileNameObj : null;

            Object sizeObj = media.get("sizeBytes");
            Long fileSize = sizeObj instanceof Number ? ((Number) sizeObj).longValue() : null;

            Object contentTypeObj = media.get("contentType");
            String mimeType = contentTypeObj instanceof String ? (String) contentTypeObj : null;

            attachmentResponses.add(
                    ForumAttachmentResponse.builder()
                            .id(mediaId)
                            .type(attachmentType)
                            .url(url)
                            .fileName(fileName)
                            .fileSize(fileSize)
                            .mimeType(mimeType)
                            .displayOrder(order++)
                            .build()
            );
        }

        boolean isLiked = false;
        if (currentUserId != null) {
            isLiked = feedPostLikeRepository.existsByPostIdAndUserId(entity.getId(), currentUserId);
        }

        UserInfoClient.UserInfo authorInfo = userInfoClient.getUserInfo(entity.getAuthorId());

        return FeedPostResponse.builder()
                .id(entity.getId())
                .targetId(entity.getTargetId())
                .targetType(entity.getTargetType())
                .targetName(targetName)
                .authorId(entity.getAuthorId())
                .authorName(authorInfo.fullName())
                .authorAvatar(authorInfo.avatarUrl())
                .visibility(entity.getVisibility())
                .title(entity.getTitle())
                .content(entity.getContent())
                .status(entity.getStatus())
                .category(categoryName)
                .categoryId(entity.getCategoryId())
                .parentPostId(entity.getParentPostId())
                .replyCount(entity.getReplyCount())
                .viewCount(entity.getViewCount())
                .likeCount(entity.getLikeCount())
                .commentCount(entity.getCommentCount())
                .flagCount(flagCount)
                .isLiked(isLiked)
                .isPinned(entity.getIsPinned())
                .isLocked(entity.getIsLocked())
                .attachments(attachmentResponses)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
