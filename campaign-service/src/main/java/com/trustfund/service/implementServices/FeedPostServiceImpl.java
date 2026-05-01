package com.trustfund.service.implementServices;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trustfund.client.UserInfoClient;
import com.trustfund.client.MediaServiceClient;
import com.trustfund.model.Campaign;
import com.trustfund.model.Expenditure;
import com.trustfund.model.FeedPost;
import com.trustfund.model.FeedPostRevision;
import com.trustfund.model.request.CreateFeedPostRequest;
import com.trustfund.model.request.UpdateFeedPostContentRequest;
import com.trustfund.model.request.UpdateFeedPostRequest;
import com.trustfund.model.response.FeedPostResponse;
import com.trustfund.model.response.FeedPostRevisionResponse;
import com.trustfund.repository.FlagRepository;
import com.trustfund.repository.FeedPostRepository;
import com.trustfund.repository.FeedPostLikeRepository;
import com.trustfund.repository.FeedPostCommentRepository;
import com.trustfund.repository.FeedPostRevisionRepository;
import com.trustfund.repository.UserPostSeenRepository;
import com.trustfund.service.interfaceServices.FeedPostService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FeedPostServiceImpl implements FeedPostService {
    private static final java.util.Set<String> VALID_STATUSES = java.util.Set.of("DRAFT", "PUBLISHED", "REJECTED",
            "HIDDEN");

    private final FeedPostRepository feedPostRepository;
    private final MediaServiceClient mediaServiceClient;
    private final FlagRepository flagRepository;
    private final FeedPostLikeRepository feedPostLikeRepository;
    private final FeedPostCommentRepository feedPostCommentRepository;
    private final org.springframework.cache.CacheManager cacheManager;
    private final UserPostSeenRepository userPostSeenRepository;
    private final UserInfoClient userInfoClient;
    private final com.trustfund.repository.ExpenditureRepository expenditureRepository;
    private final com.trustfund.repository.CampaignRepository campaignRepository;
    private final FeedPostRevisionRepository feedPostRevisionRepository;
    private final ObjectMapper objectMapper;

    @Override
    public FeedPostResponse create(CreateFeedPostRequest request, Long authorId) {
        FeedPost feedPost = FeedPost.builder()
                .targetId(request.getTargetId())
                .targetType(request.getTargetType())
                .targetName(request.getTargetName())
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

        // Locked posts are hidden from public users (staff/admin can still view)
        if (Boolean.TRUE.equals(post.getIsLocked())) {
            boolean isStaffOrAdmin = false;
            // Note: isStaffOrAdmin requires currentUserId to check roles from
            // identity-service
            // For simplicity, locked posts are only accessible to the post author or via
            // admin endpoints
            if (currentUserId == null || !currentUserId.equals(post.getAuthorId())) {
                throw new com.trustfund.exception.exceptions.ForbiddenException("Bài viết này đã bị khóa");
            }
        }

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
        // Nếu user đã có record post_seen rồi thì không tăng
        if (userId != null && userPostSeenRepository.existsByUserIdAndPostId(userId, postId)) {
            return;
        }

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
    public org.springframework.data.domain.Page<FeedPostResponse> getMyFeedPosts(Long currentUserId, String status,
            org.springframework.data.domain.Pageable pageable) {
        if (currentUserId == null) {
            throw new com.trustfund.exception.exceptions.UnauthorizedException("Authentication required");
        }

        String normalizedStatus = null;
        if (status != null && !status.isBlank()) {
            if (!status.equalsIgnoreCase("ALL")) {
                normalizedStatus = status.toUpperCase();
                if (!VALID_STATUSES.contains(normalizedStatus)) {
                    throw new com.trustfund.exception.exceptions.BadRequestException("Invalid status");
                }
            }
        }

        return feedPostRepository.findMyPosts(currentUserId, normalizedStatus, pageable)
                .map(post -> toResponse(post, currentUserId, null));
    }

    @Override
    public org.springframework.data.domain.Page<FeedPostResponse> getPublicPostsByAuthorId(Long authorId,
            org.springframework.data.domain.Pageable pageable) {
        return feedPostRepository.findPublicPostsByAuthorId(authorId, pageable)
                .map(post -> toResponse(post, null, null));
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

        String normalizedStatus = status.toUpperCase();
        if (!VALID_STATUSES.contains(normalizedStatus)) {
            throw new com.trustfund.exception.exceptions.BadRequestException("Invalid status");
        }

        post.setStatus(normalizedStatus);
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
    @org.springframework.transaction.annotation.Transactional
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

        // Save snapshot of current state before mutating
        snapshotRevision(post, currentUserId, null);

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
    @org.springframework.transaction.annotation.Transactional
    public FeedPostResponse update(Long id, Long currentUserId, UpdateFeedPostRequest request) {
        FeedPost post = feedPostRepository.findById(id)
                .orElseThrow(() -> new com.trustfund.exception.exceptions.NotFoundException("Feed post not found"));

        if (currentUserId == null) {
            throw new com.trustfund.exception.exceptions.UnauthorizedException("Authentication required");
        }

        if (!currentUserId.equals(post.getAuthorId())) {
            throw new com.trustfund.exception.exceptions.ForbiddenException("Not allowed to update this feed post");
        }

        // Detect target link change (not stored in revision, so must be checked here)
        Long newTargetId = request.getTargetId();
        String newTargetType = request.getTargetType();
        boolean targetChanged = !java.util.Objects.equals(newTargetId, post.getTargetId())
                || !java.util.Objects.equals(newTargetType, post.getTargetType());

        // Save snapshot of current state before mutating
        // snapshotRevision internally skips if identical to the last revision,
        // but a non-null editNote forces it through
        snapshotRevision(post, currentUserId, targetChanged ? "Thay đổi liên kết" : null);

        if (request.getTitle() != null) {
            post.setTitle(request.getTitle());
        }
        if (request.getContent() != null) {
            post.setContent(request.getContent());
        }
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            post.setStatus(request.getStatus());
        }
        if (newTargetId != null) {
            post.setTargetId(newTargetId);
        } else {
            post.setTargetId(null);
        }
        if (newTargetType != null) {
            post.setTargetType(newTargetType);
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

        if (Boolean.TRUE.equals(post.getIsLocked())) {
            throw new com.trustfund.exception.exceptions.ForbiddenException("Bài viết đang khóa tương tác");
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
    public org.springframework.data.domain.Page<FeedPostResponse> getAllFeedPosts(
            org.springframework.data.domain.Pageable pageable) {
        org.springframework.data.domain.Page<FeedPost> posts = feedPostRepository.findAll(pageable);
        java.util.List<Long> postIds = posts.getContent().stream()
                .map(FeedPost::getId)
                .collect(java.util.stream.Collectors.toList());

        java.util.Map<Long, Integer> flagCountByPostId = new java.util.HashMap<>();
        if (!postIds.isEmpty()) {
            java.util.List<Object[]> rows = flagRepository.countPendingFlagsByPostIds(postIds, "PENDING");
            for (Object[] row : rows) {
                if (row == null || row.length < 2)
                    continue;
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
        String normalizedStatus = status.toUpperCase();
        if (!VALID_STATUSES.contains(normalizedStatus)) {
            throw new com.trustfund.exception.exceptions.BadRequestException("Invalid status");
        }
        post.setStatus(normalizedStatus);
        feedPostRepository.save(post);
        return toResponse(post, null, null);
    }

    @Override
    public FeedPostResponse approveByAdmin(Long id) {
        return updateStatusByAdmin(id, "PUBLISHED");
    }

    @Override
    public FeedPostResponse rejectByAdmin(Long id) {
        return updateStatusByAdmin(id, "REJECTED");
    }

    @Override
    public FeedPostResponse hideByAdmin(Long id) {
        FeedPost post = feedPostRepository.findById(id)
                .orElseThrow(() -> new com.trustfund.exception.exceptions.NotFoundException("Feed post not found"));
        post.setStatus("HIDDEN");
        post.setIsLocked(true);
        feedPostRepository.save(post);
        return toResponse(post, null, null);
    }

    @Override
    public FeedPostResponse updateContentByAdmin(Long id, UpdateFeedPostContentRequest request) {
        FeedPost post = feedPostRepository.findById(id)
                .orElseThrow(() -> new com.trustfund.exception.exceptions.NotFoundException("Feed post not found"));

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
        return toResponse(saved, null, null);
    }

    private FeedPostResponse toResponse(FeedPost entity, Long currentUserId, Integer flagCount) {
        // Resolve targetName based on targetType if not explicitly set (e.g. not
        // 'evidence')
        String targetName = entity.getTargetName();
        if (entity.getTargetId() != null && entity.getTargetType() != null) {
            if (entity.getTargetType().equals("EXPENDITURE")) {
                String planName = expenditureRepository.findById(entity.getTargetId())
                        .map(Expenditure::getPlan)
                        .orElse(null);
                if ("evidence".equalsIgnoreCase(targetName)) {
                    targetName = "evidence|" + (planName != null ? planName : "");
                } else if (targetName == null || targetName.isBlank()) {
                    targetName = planName;
                }
            } else if (entity.getTargetType().equals("CAMPAIGN")) {
                if (targetName == null || targetName.isBlank()) {
                    targetName = campaignRepository.findById(entity.getTargetId())
                            .map(Campaign::getTitle)
                            .orElse(null);
                }
            }
        }

        boolean isLiked = false;
        if (currentUserId != null) {
            isLiked = feedPostLikeRepository.existsByPostIdAndUserId(entity.getId(), currentUserId);
        }

        UserInfoClient.UserInfo authorInfo = null;
        if (entity.getAuthorId() != null) {
            try {
                authorInfo = userInfoClient.getUserInfo(entity.getAuthorId());
            } catch (Exception e) {
                // authorInfo remains null, handled by builders below
            }
        }

        // Fetch media attachments for this post from media-service
        java.util.List<java.util.Map<String, Object>> attachments = java.util.Collections.emptyList();
        try {
            attachments = mediaServiceClient.getMediaByPostId(entity.getId());
            if (attachments == null) attachments = java.util.Collections.emptyList();
        } catch (Exception e) {
            // Log warning and continue with empty attachments
        }

        return FeedPostResponse.builder()
                .id(entity.getId())
                .targetId(entity.getTargetId())
                .targetType(entity.getTargetType())
                .targetName(targetName)
                .authorId(entity.getAuthorId())
                .authorName(authorInfo != null ? authorInfo.fullName() : null)
                .authorAvatar(authorInfo != null ? authorInfo.avatarUrl() : null)
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
                .flagCount(flagCount)
                .isLiked(isLiked)
                .isPinned(entity.getIsPinned())
                .isLocked(entity.getIsLocked())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .hasRevisions(entity.getId() != null && feedPostRevisionRepository.existsByPostId(entity.getId()))
                .attachments(attachments)
                .build();
    }

    @Override
    public List<FeedPostResponse> getByTarget(Long targetId, String targetType) {
        try {
            return feedPostRepository.findByTargetIdAndTargetTypeOrderByCreatedAtDesc(targetId, targetType)
                    .stream()
                    .filter(p -> !Boolean.TRUE.equals(p.getIsLocked()))
                    .map(p -> toResponse(p, null, 0))
                    .collect(java.util.stream.Collectors.toList());
        } catch (Exception e) {
            return java.util.Collections.emptyList();
        }
    }

    // =========================================================================
    // Revision History
    // =========================================================================

    @Override
    public org.springframework.data.domain.Page<FeedPostRevisionResponse> getRevisions(
            Long postId, Long currentUserId, String currentRole, org.springframework.data.domain.Pageable pageable) {
        FeedPost post = feedPostRepository.findById(postId)
                .orElseThrow(() -> new com.trustfund.exception.exceptions.NotFoundException("Feed post not found"));

        if (!"PUBLISHED".equals(post.getStatus())) {
            boolean isOwner = currentUserId != null && currentUserId.equals(post.getAuthorId());
            boolean isPrivileged = isStaffOrAdmin(currentRole);
            if (!isOwner && !isPrivileged) {
                throw new com.trustfund.exception.exceptions.ForbiddenException(
                        "Lịch sử chỉnh sửa chỉ hiển thị với bài đã đăng");
            }
        }

        return feedPostRevisionRepository.findByPostIdOrderByRevisionNoDesc(postId, pageable)
                .map(this::toRevisionResponse);
    }

    @Override
    public FeedPostRevisionResponse getRevisionById(Long postId, Long revisionId, Long currentUserId,
            String currentRole) {
        FeedPost post = feedPostRepository.findById(postId)
                .orElseThrow(() -> new com.trustfund.exception.exceptions.NotFoundException("Feed post not found"));

        if (!"PUBLISHED".equals(post.getStatus())) {
            boolean isOwner = currentUserId != null && currentUserId.equals(post.getAuthorId());
            boolean isPrivileged = isStaffOrAdmin(currentRole);
            if (!isOwner && !isPrivileged) {
                throw new com.trustfund.exception.exceptions.ForbiddenException(
                        "Lịch sử chỉnh sửa chỉ hiển thị với bài đã đăng");
            }
        }

        FeedPostRevision revision = feedPostRevisionRepository.findByPostIdAndId(postId, revisionId)
                .orElseThrow(() -> new com.trustfund.exception.exceptions.NotFoundException("Revision not found"));

        return toRevisionResponse(revision);
    }

    /**
     * Returns true if the role string represents STAFF or ADMIN (handles ROLE_
     * prefix).
     */
    private boolean isStaffOrAdmin(String role) {
        if (role == null)
            return false;
        String r = role.startsWith("ROLE_") ? role.substring(5) : role;
        return "STAFF".equals(r) || "ADMIN".equals(r);
    }

    /**
     * Creates a revision snapshot of the current post state BEFORE it is mutated.
     * Called inside update transactions.
     */
    private void snapshotRevision(FeedPost post, Long editedBy, String editNote) {
        if (post.getId() == null)
            return;
        try {
            // Fetch media currently attached to the post
            List<Map<String, Object>> mediaList = mediaServiceClient.getMediaByPostId(post.getId());
            String mediaJson = serializeMediaSnapshot(mediaList);

            // Guard: truncate content if longer than column limit
            String contentSnapshot = post.getContent();
            if (contentSnapshot != null && contentSnapshot.length() > 3000) {
                org.slf4j.LoggerFactory.getLogger(FeedPostServiceImpl.class)
                        .warn("Content truncated for revision snapshot of post {}: original length {}", post.getId(),
                                contentSnapshot.length());
                contentSnapshot = contentSnapshot.substring(0, 3000);
            }

            // Guard: title truncation
            String titleSnapshot = post.getTitle();
            if (titleSnapshot != null && titleSnapshot.length() > 255) {
                titleSnapshot = titleSnapshot.substring(0, 255);
            }

            // Skip if this snapshot is identical to the most recent revision (no real
            // change)
            // Exception: if editNote is provided, always save (e.g. target link changed)
            if (editNote == null || editNote.isBlank()) {
                FeedPostRevision lastRev = feedPostRevisionRepository
                        .findTopByPostIdOrderByRevisionNoDesc(post.getId());
                if (lastRev != null) {
                    boolean sameTitle = java.util.Objects.equals(titleSnapshot, lastRev.getTitle());
                    boolean sameContent = java.util.Objects.equals(
                            contentSnapshot != null ? contentSnapshot : "", lastRev.getContent());
                    boolean sameMedia = java.util.Objects.equals(mediaJson, lastRev.getMediaSnapshotJson());
                    if (sameTitle && sameContent && sameMedia) {
                        return;
                    }
                }
            }

            int nextRevNo = feedPostRevisionRepository.findMaxRevisionNoByPostId(post.getId()) + 1;

            // Resolve editor name
            String editorName = null;
            try {
                UserInfoClient.UserInfo info = userInfoClient.getUserInfo(editedBy);
                if (info != null)
                    editorName = info.fullName();
            } catch (Exception ignored) {
            }

            FeedPostRevision rev = FeedPostRevision.builder()
                    .postId(post.getId())
                    .revisionNo(nextRevNo)
                    .title(titleSnapshot)
                    .content(contentSnapshot != null ? contentSnapshot : "")
                    .status(post.getStatus())
                    .mediaSnapshotJson(mediaJson)
                    .editedBy(editedBy)
                    .editedByName(editorName)
                    .editNote(editNote)
                    .build();

            feedPostRevisionRepository.save(rev);
        } catch (Exception e) {
            // Never block the main edit if snapshot fails
            org.slf4j.LoggerFactory.getLogger(FeedPostServiceImpl.class)
                    .error("Failed to snapshot revision for post {}: {}", post.getId(), e.getMessage());
        }
    }

    private String serializeMediaSnapshot(List<Map<String, Object>> mediaList) {
        if (mediaList == null || mediaList.isEmpty())
            return "[]";
        try {
            return objectMapper.writeValueAsString(mediaList);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> deserializeMediaSnapshot(String json) {
        if (json == null || json.isBlank() || json.equals("[]"))
            return Collections.emptyList();
        try {
            return objectMapper.readValue(json, List.class);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private FeedPostRevisionResponse toRevisionResponse(FeedPostRevision rev) {
        return FeedPostRevisionResponse.builder()
                .id(rev.getId())
                .postId(rev.getPostId())
                .revisionNo(rev.getRevisionNo())
                .title(rev.getTitle())
                .content(rev.getContent())
                .status(rev.getStatus())
                .mediaSnapshot(deserializeMediaSnapshot(rev.getMediaSnapshotJson()))
                .editedBy(rev.getEditedBy())
                .editedByName(rev.getEditedByName())
                .editNote(rev.getEditNote())
                .createdAt(rev.getCreatedAt())
                .build();
    }
}
