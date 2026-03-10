package com.trustfund.service.implementServices;

import com.trustfund.client.FlagServiceClient;
import com.trustfund.client.UserInfoClient;
import com.trustfund.model.FeedPost;
import com.trustfund.model.request.CreateFeedPostRequest;
import com.trustfund.model.request.UpdateFeedPostContentRequest;
import com.trustfund.model.request.UpdateFeedPostRequest;
import com.trustfund.model.response.FeedPostResponse;
import com.trustfund.repository.FeedPostRepository;
import com.trustfund.repository.FeedPostLikeRepository;
import com.trustfund.repository.FeedPostCommentRepository;
import com.trustfund.service.interfaceServices.FeedPostService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FeedPostServiceImpl implements FeedPostService {

    private final FeedPostRepository feedPostRepository;
    private final FeedPostLikeRepository feedPostLikeRepository;
    private final FeedPostCommentRepository feedPostCommentRepository;
    private final org.springframework.cache.CacheManager cacheManager;
    private final UserInfoClient userInfoClient;
    private final FlagServiceClient flagServiceClient;

    // ── CRUD ──────────────────────────────────────────────────────────────────

    @Override
    public FeedPostResponse create(CreateFeedPostRequest request, Long authorId) {
        FeedPost feedPost = FeedPost.builder()
                .campaignId(request.getCampaignId())
                .expenditureId(request.getExpenditureId())
                .authorId(authorId)
                .category(request.getCategory())
                .type(request.getType())
                .visibility(request.getVisibility())
                .title(request.getTitle())
                .content(request.getContent())
                .status(request.getStatus() == null || request.getStatus().isBlank() ? "DRAFT" : request.getStatus())
                .build();

        return toResponse(feedPostRepository.save(feedPost), authorId);
    }

    @Override
    public FeedPostResponse getById(Long id, Long currentUserId, String ipAddress) {
        FeedPost post = feedPostRepository.findById(id)
                .orElseThrow(() -> new com.trustfund.exception.exceptions.NotFoundException("Feed post not found"));

        String visibility = post.getVisibility();
        if (visibility == null || visibility.isBlank())
            throw new com.trustfund.exception.exceptions.BadRequestException("Invalid visibility");

        incrementViewCountIfEligible(id, currentUserId, ipAddress);

        if (visibility.equals("PUBLIC")) return toResponse(post, currentUserId);

        if (currentUserId == null)
            throw new com.trustfund.exception.exceptions.UnauthorizedException("Authentication required");

        if (visibility.equals("PRIVATE")) {
            if (!currentUserId.equals(post.getAuthorId()))
                throw new com.trustfund.exception.exceptions.ForbiddenException("Not allowed to view this feed post");
            return toResponse(post, currentUserId);
        }

        if (visibility.equals("FOLLOWERS")) return toResponse(post, currentUserId);

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
    public org.springframework.data.domain.Page<FeedPostResponse> getActiveFeedPostsByCampaignId(
            Long campaignId, Long currentUserId,
            org.springframework.data.domain.Pageable pageable) {
        if (campaignId == null)
            throw new com.trustfund.exception.exceptions.BadRequestException("campaignId is required");
        return feedPostRepository.findVisibleActivePostsByCampaignId(campaignId, currentUserId, pageable)
                .map(post -> toResponse(post, currentUserId));
    }

    @Override
    public FeedPostResponse updateStatus(Long id, Long currentUserId, String status) {
        FeedPost post = feedPostRepository.findById(id)
                .orElseThrow(() -> new com.trustfund.exception.exceptions.NotFoundException("Feed post not found"));
        if (currentUserId == null)
            throw new com.trustfund.exception.exceptions.UnauthorizedException("Authentication required");
        if (!currentUserId.equals(post.getAuthorId()))
            throw new com.trustfund.exception.exceptions.ForbiddenException("Not allowed to update this feed post");
        if (status == null || status.isBlank())
            throw new com.trustfund.exception.exceptions.BadRequestException("Status is required");
        if (!status.equals("DRAFT") && !status.equals("ACTIVE"))
            throw new com.trustfund.exception.exceptions.BadRequestException("Invalid status");
        post.setStatus(status);
        return toResponse(feedPostRepository.save(post), currentUserId);
    }

    @Override
    public FeedPostResponse updateVisibility(Long id, Long currentUserId, String currentRole, String visibility) {
        FeedPost post = feedPostRepository.findById(id)
                .orElseThrow(() -> new com.trustfund.exception.exceptions.NotFoundException("Feed post not found"));
        if (currentUserId == null)
            throw new com.trustfund.exception.exceptions.UnauthorizedException("Authentication required");

        String role = currentRole;
        if (role != null && role.startsWith("ROLE_")) role = role.substring("ROLE_".length());

        boolean isAuthor = currentUserId.equals(post.getAuthorId());
        boolean isStaff  = "STAFF".equals(role);
        boolean isAdmin  = "ADMIN".equals(role);

        if (!isAuthor && !isStaff && !isAdmin)
            throw new com.trustfund.exception.exceptions.ForbiddenException("Not allowed to update this feed post");
        if (visibility == null || visibility.isBlank())
            throw new com.trustfund.exception.exceptions.BadRequestException("Visibility is required");
        if (!visibility.equals("PUBLIC") && !visibility.equals("PRIVATE") && !visibility.equals("FOLLOWERS"))
            throw new com.trustfund.exception.exceptions.BadRequestException("Invalid visibility");

        post.setVisibility(visibility);
        return toResponse(feedPostRepository.save(post), currentUserId);
    }

    @Override
    public FeedPostResponse updateContent(Long id, Long currentUserId, UpdateFeedPostContentRequest request) {
        FeedPost post = feedPostRepository.findById(id)
                .orElseThrow(() -> new com.trustfund.exception.exceptions.NotFoundException("Feed post not found"));
        if (currentUserId == null)
            throw new com.trustfund.exception.exceptions.UnauthorizedException("Authentication required");
        if (!currentUserId.equals(post.getAuthorId()))
            throw new com.trustfund.exception.exceptions.ForbiddenException("Not allowed to update this feed post");

        boolean hasTitle   = request.getTitle() != null && !request.getTitle().isBlank();
        boolean hasContent = request.getContent() != null && !request.getContent().isBlank();
        if (!hasTitle && !hasContent)
            throw new com.trustfund.exception.exceptions.BadRequestException("Nothing to update");

        if (request.getTitle() != null) post.setTitle(request.getTitle());
        if (request.getContent() != null) post.setContent(request.getContent());

        return toResponse(feedPostRepository.save(post), currentUserId);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public FeedPostResponse update(Long id, Long currentUserId, UpdateFeedPostRequest request) {
        FeedPost post = feedPostRepository.findById(id)
                .orElseThrow(() -> new com.trustfund.exception.exceptions.NotFoundException("Feed post not found"));
        if (currentUserId == null)
            throw new com.trustfund.exception.exceptions.UnauthorizedException("Authentication required");
        if (!currentUserId.equals(post.getAuthorId()))
            throw new com.trustfund.exception.exceptions.ForbiddenException("Not allowed to update this feed post");

        if (request.getTitle() != null) post.setTitle(request.getTitle());
        if (request.getContent() != null) post.setContent(request.getContent());
        if (request.getStatus() != null && !request.getStatus().isBlank()) post.setStatus(request.getStatus());
        if (request.getCategory() != null) post.setCategory(request.getCategory());
        post.setCampaignId(request.getCampaignId());
        post.setExpenditureId(request.getExpenditureId());

        return toResponse(feedPostRepository.save(post), currentUserId);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void delete(Long id, Long currentUserId) {
        FeedPost post = feedPostRepository.findById(id)
                .orElseThrow(() -> new com.trustfund.exception.exceptions.NotFoundException("Feed post not found"));
        if (currentUserId == null)
            throw new com.trustfund.exception.exceptions.UnauthorizedException("Authentication required");
        if (!currentUserId.equals(post.getAuthorId()))
            throw new com.trustfund.exception.exceptions.ForbiddenException("Not allowed to delete this feed post");
        feedPostRepository.delete(post);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public FeedPostResponse toggleLike(Long postId, Long currentUserId) {
        FeedPost post = feedPostRepository.findById(postId)
                .orElseThrow(() -> new com.trustfund.exception.exceptions.NotFoundException("Feed post not found"));
        if (currentUserId == null)
            throw new com.trustfund.exception.exceptions.UnauthorizedException("Authentication required");

        boolean exists = feedPostLikeRepository.existsByPostIdAndUserId(postId, currentUserId);
        if (exists) {
            feedPostLikeRepository.deleteByPostIdAndUserId(postId, currentUserId);
            post.setLikeCount(post.getLikeCount() != null && post.getLikeCount() > 0 ? post.getLikeCount() - 1 : 0);
        } else {
            post.setLikeCount(post.getLikeCount() == null ? 1 : post.getLikeCount() + 1);
            feedPostLikeRepository.save(com.trustfund.model.FeedPostLike.builder()
                    .postId(postId).userId(currentUserId).build());
        }

        feedPostRepository.save(post);
        return toResponse(post, currentUserId);
    }

    @Override
    public org.springframework.data.domain.Page<FeedPostResponse> getAllFeedPosts(
            String status, String type, String keyword,
            org.springframework.data.domain.Pageable pageable) {
        String s = (status  != null && !status.isBlank())  ? status  : null;
        String t = (type    != null && !type.isBlank())    ? type    : null;
        String k = (keyword != null && !keyword.isBlank()) ? keyword : null;
        return feedPostRepository.findAllWithFilters(s, t, k, pageable).map(post -> {
            FeedPostResponse r = toResponse(post, null);
            r.setFlagCount(flagServiceClient.getFlagCountForPost(post.getId()));
            return r;
        });
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void deleteByAdmin(Long id) {
        FeedPost post = feedPostRepository.findById(id)
                .orElseThrow(() -> new com.trustfund.exception.exceptions.NotFoundException("Feed post not found"));
        feedPostLikeRepository.deleteByPostId(id);
        feedPostCommentRepository.deleteByPostId(id);
        feedPostRepository.delete(post);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public int syncAllCommentCounts() {
        int fixed = 0;
        for (FeedPost post : feedPostRepository.findAll()) {
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
        post.setIsPinned(post.getIsPinned() == null || !post.getIsPinned());
        feedPostRepository.save(post);
        return toResponse(post, null);
    }

    @Override
    public FeedPostResponse toggleLock(Long id) {
        FeedPost post = feedPostRepository.findById(id)
                .orElseThrow(() -> new com.trustfund.exception.exceptions.NotFoundException("Feed post not found"));
        post.setIsLocked(post.getIsLocked() == null || !post.getIsLocked());
        feedPostRepository.save(post);
        return toResponse(post, null);
    }

    @Override
    public FeedPostResponse updateStatusByAdmin(Long id, String status) {
        FeedPost post = feedPostRepository.findById(id)
                .orElseThrow(() -> new com.trustfund.exception.exceptions.NotFoundException("Feed post not found"));
        if (status == null || status.isBlank())
            throw new com.trustfund.exception.exceptions.BadRequestException("Status is required");
        if (!status.equals("DRAFT") && !status.equals("ACTIVE"))
            throw new com.trustfund.exception.exceptions.BadRequestException("Invalid status");
        post.setStatus(status);
        feedPostRepository.save(post);
        return toResponse(post, null);
    }

    // ── Mapping ───────────────────────────────────────────────────────────────

    private FeedPostResponse toResponse(FeedPost entity, Long currentUserId) {
        boolean isLiked = currentUserId != null &&
                feedPostLikeRepository.existsByPostIdAndUserId(entity.getId(), currentUserId);

        UserInfoClient.UserInfo authorInfo = userInfoClient.getUserInfo(entity.getAuthorId());

        return FeedPostResponse.builder()
                .id(entity.getId())
                .campaignId(entity.getCampaignId())
                .expenditureId(entity.getExpenditureId())
                .authorId(entity.getAuthorId())
                .authorName(authorInfo.fullName())
                .authorAvatar(authorInfo.avatarUrl())
                .type(entity.getType())
                .visibility(entity.getVisibility())
                .title(entity.getTitle())
                .content(entity.getContent())
                .status(entity.getStatus())
                .category(entity.getCategory())
                .parentPostId(entity.getParentPostId())
                .replyCount(entity.getReplyCount())
                .viewCount(entity.getViewCount())
                .likeCount(entity.getLikeCount())
                .commentCount(entity.getCommentCount())
                .isLiked(isLiked)
                .isPinned(entity.getIsPinned())
                .isLocked(entity.getIsLocked())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
