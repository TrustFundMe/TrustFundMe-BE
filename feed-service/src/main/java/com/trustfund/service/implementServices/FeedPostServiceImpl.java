package com.trustfund.service.implementServices;

import com.trustfund.model.FeedPost;
import com.trustfund.model.request.CreateFeedPostRequest;
import com.trustfund.model.response.FeedPostResponse;
import com.trustfund.repository.FeedPostRepository;
import com.trustfund.service.interfaceServices.FeedPostService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FeedPostServiceImpl implements FeedPostService {

    private final FeedPostRepository feedPostRepository;

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
        return toResponse(saved);
    }

    @Override
    public FeedPostResponse getById(Long id, Long currentUserId) {
        FeedPost post = feedPostRepository.findById(id)
                .orElseThrow(() -> new com.trustfund.exception.exceptions.NotFoundException("Feed post not found"));

        String visibility = post.getVisibility();
        if (visibility == null || visibility.isBlank()) {
            throw new com.trustfund.exception.exceptions.BadRequestException("Invalid visibility");
        }

        if (visibility.equals("PUBLIC")) {
            return toResponse(post);
        }

        if (currentUserId == null) {
            throw new com.trustfund.exception.exceptions.UnauthorizedException("Authentication required");
        }

        if (visibility.equals("PRIVATE")) {
            if (!currentUserId.equals(post.getAuthorId())) {
                throw new com.trustfund.exception.exceptions.ForbiddenException("Not allowed to view this feed post");
            }
            return toResponse(post);
        }

        if (visibility.equals("FOLLOWERS")) {
            // TODO: chưa có bảng follow trong feed-service, tạm thời yêu cầu login là đủ
            return toResponse(post);
        }

        throw new com.trustfund.exception.exceptions.BadRequestException("Invalid visibility");
    }

    @Override
    public org.springframework.data.domain.Page<FeedPostResponse> getActiveFeedPosts(Long currentUserId, org.springframework.data.domain.Pageable pageable) {
        return feedPostRepository.findVisibleActivePosts(currentUserId, pageable)
                .map(this::toResponse);
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
        return toResponse(saved);
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
        return toResponse(saved);
    }

    private FeedPostResponse toResponse(FeedPost entity) {
        return FeedPostResponse.builder()
                .id(entity.getId())
                .budgetId(entity.getBudgetId())
                .authorId(entity.getAuthorId())
                .type(entity.getType())
                .visibility(entity.getVisibility())
                .title(entity.getTitle())
                .content(entity.getContent())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
