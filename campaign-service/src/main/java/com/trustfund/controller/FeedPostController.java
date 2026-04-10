package com.trustfund.controller;

import com.trustfund.model.request.CreateFeedPostRequest;
import com.trustfund.model.request.UpdateFeedPostContentRequest;
import com.trustfund.model.request.UpdateFeedPostRequest;
import com.trustfund.model.request.UpdateFeedPostStatusRequest;
import com.trustfund.model.request.UpdateFeedPostVisibilityRequest;
import com.trustfund.model.response.FeedPostResponse;
import com.trustfund.model.response.FeedPostRevisionResponse;
import com.trustfund.service.TrustScoreService;
import com.trustfund.service.interfaceServices.FeedPostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/feed-posts")
@Tag(name = "Feed Posts", description = "Feed post APIs")
public class FeedPostController {

    private final FeedPostService feedPostService;
    private final TrustScoreService trustScoreService;
    private final com.trustfund.client.UserInfoClient userInfoClient;

    public FeedPostController(FeedPostService feedPostService, TrustScoreService trustScoreService,
                              com.trustfund.client.UserInfoClient userInfoClient) {
        this.feedPostService = feedPostService;
        this.trustScoreService = trustScoreService;
        this.userInfoClient = userInfoClient;
    }

    @PostMapping
    @Operation(summary = "Create feed post", description = "Create a new feed post for the authenticated user")
    public ResponseEntity<FeedPostResponse> create(@Valid @RequestBody CreateFeedPostRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long authorId = Long.parseLong(authentication.getName());

        FeedPostResponse response = feedPostService.create(request, authorId);

        // Trust Score: cộng điểm DAILY_POST (chỉ 1 lần/ngày)
        try {
            trustScoreService.addScore(authorId, "DAILY_POST", response.getId(), "POST",
                    "Đăng bài viết hàng ngày");
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(FeedPostController.class)
                    .error("Error updating daily post trust score for user {}: {}", authorId, e.getMessage());
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Get active feed posts", description = "Get list of feed posts with status=ACTIVE and visibility rules")
    public ResponseEntity<Page<FeedPostResponse>> getActiveFeedPosts(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "sort", defaultValue = "createdAt,desc") String sort
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long currentUserId = null;
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            try {
                currentUserId = Long.parseLong(authentication.getName());
            } catch (NumberFormatException ignored) {
                currentUserId = null;
            }
        }

        String[] sortParts = sort.split(",");
        String sortField = sortParts[0];
        Sort.Direction direction = (sortParts.length > 1 && sortParts[1].equalsIgnoreCase("asc"))
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortField));
        return ResponseEntity.ok(feedPostService.getActiveFeedPosts(currentUserId, pageable));
    }

    @GetMapping("/my")
    @Operation(summary = "Get my feed posts", description = "Get feed posts created by the authenticated user with optional status filter")
    public ResponseEntity<Page<FeedPostResponse>> getMyFeedPosts(
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "sort", defaultValue = "updatedAt,desc") String sort) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new com.trustfund.exception.exceptions.UnauthorizedException("Authentication required");
        }

        Long currentUserId;
        try {
            currentUserId = Long.parseLong(authentication.getName());
        } catch (NumberFormatException ex) {
            throw new com.trustfund.exception.exceptions.UnauthorizedException("Invalid authentication principal");
        }

        String[] sortParts = sort.split(",");
        String sortField = sortParts[0];
        Sort.Direction direction = (sortParts.length > 1 && sortParts[1].equalsIgnoreCase("asc"))
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortField));
        return ResponseEntity.ok(feedPostService.getMyFeedPosts(currentUserId, status, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get feed post detail", description = "Get detail of a feed post by id")
    public ResponseEntity<FeedPostResponse> getById(@PathVariable("id") Long id, jakarta.servlet.http.HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long currentUserId = null;
        if (authentication != null) {
            try {
                currentUserId = Long.parseLong(authentication.getName());
            } catch (Exception ignored) {
                currentUserId = null;
            }
        }

        String ipAddress = request.getRemoteAddr();
        FeedPostResponse response = feedPostService.getById(id, currentUserId, ipAddress);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/by-target")
    @Operation(summary = "Get feed posts by target", description = "Get feed posts linked to a specific target (campaign, expenditure, etc.)")
    public ResponseEntity<List<FeedPostResponse>> getByTarget(
            @RequestParam("targetId") Long targetId,
            @RequestParam("targetType") String targetType) {
        return ResponseEntity.ok(feedPostService.getByTarget(targetId, targetType));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update feed post status", description = "Update feed post status between DRAFT and ACTIVE")
    public ResponseEntity<FeedPostResponse> updateStatus(@PathVariable("id") Long id,
                                                        @Valid @RequestBody UpdateFeedPostStatusRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long currentUserId = Long.parseLong(authentication.getName());

        FeedPostResponse response = feedPostService.updateStatus(id, currentUserId, request.getStatus());
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/visibility")
    @Operation(summary = "Update feed post visibility", description = "Update feed post visibility between PUBLIC, PRIVATE and FOLLOWERS")
    public ResponseEntity<FeedPostResponse> updateVisibility(@PathVariable("id") Long id,
                                                            @Valid @RequestBody UpdateFeedPostVisibilityRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long currentUserId = Long.parseLong(authentication.getName());

        String currentRole = authentication.getAuthorities().stream()
                .findFirst()
                .map(grantedAuthority -> grantedAuthority.getAuthority())
                .orElse(null);

        FeedPostResponse response = feedPostService.updateVisibility(id, currentUserId, currentRole, request.getVisibility());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update feed post (full)", description = "Update title, content, status, targetId, targetType, attachments (author only)")
    public ResponseEntity<FeedPostResponse> update(@PathVariable("id") Long id,
                                                   @Valid @RequestBody UpdateFeedPostRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long currentUserId = Long.parseLong(authentication.getName());

        FeedPostResponse response = feedPostService.update(id, currentUserId, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update feed post content", description = "Update title/content of a feed post (author only)")
    public ResponseEntity<FeedPostResponse> updateContent(@PathVariable("id") Long id,
                                                         @Valid @RequestBody UpdateFeedPostContentRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long currentUserId = Long.parseLong(authentication.getName());

        FeedPostResponse response = feedPostService.updateContent(id, currentUserId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete feed post", description = "Delete a feed post (author only)")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long currentUserId = Long.parseLong(authentication.getName());
        feedPostService.delete(id, currentUserId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/like")
    @Operation(summary = "Toggle like", description = "Like or unlike a feed post")
    public ResponseEntity<FeedPostResponse> toggleLike(@PathVariable("id") Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long currentUserId = Long.parseLong(authentication.getName());

        FeedPostResponse response = feedPostService.toggleLike(id, currentUserId);
        return ResponseEntity.ok(response);
    }

    // Admin APIs

    @GetMapping("/admin")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Get all feed posts (Admin)", description = "Get list of all feed posts without visibility filtering")
    public ResponseEntity<Page<FeedPostResponse>> getAllFeedPosts(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "sort", defaultValue = "createdAt,desc") String sort) {

        String[] sortParts = sort.split(",");
        String sortField = sortParts[0];
        Sort.Direction direction = (sortParts.length > 1 && sortParts[1].equalsIgnoreCase("asc"))
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortField));
        return ResponseEntity.ok(feedPostService.getAllFeedPosts(pageable));
    }

    @DeleteMapping("/admin/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Delete feed post (Admin)", description = "Force delete a feed post by admin")
    public ResponseEntity<Void> deleteByAdmin(@PathVariable("id") Long id) {
        feedPostService.deleteByAdmin(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/admin/{id}/pin")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Pin/Unpin feed post (Admin)", description = "Toggle pin status of a feed post")
    public ResponseEntity<FeedPostResponse> togglePin(@PathVariable("id") Long id) {
        return ResponseEntity.ok(feedPostService.togglePin(id));
    }

    @PatchMapping("/admin/{id}/lock")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Lock/Unlock feed post (Admin)", description = "Toggle lock status of a feed post")
    public ResponseEntity<FeedPostResponse> toggleLock(@PathVariable("id") Long id) {
        return ResponseEntity.ok(feedPostService.toggleLock(id));
    }

    @PatchMapping("/admin/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Update feed post status (Admin)", description = "Force update status of any feed post")
    public ResponseEntity<FeedPostResponse> updateStatusByAdmin(@PathVariable("id") Long id,
                                                                @Valid @RequestBody UpdateFeedPostStatusRequest request) {
        return ResponseEntity.ok(feedPostService.updateStatusByAdmin(id, request.getStatus()));
    }

    @PatchMapping("/admin/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Approve feed post (Admin)", description = "Approve and publish a feed post")
    public ResponseEntity<FeedPostResponse> approveByAdmin(@PathVariable("id") Long id) {
        return ResponseEntity.ok(feedPostService.approveByAdmin(id));
    }

    @PatchMapping("/admin/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Reject feed post (Admin)", description = "Reject a feed post")
    public ResponseEntity<FeedPostResponse> rejectByAdmin(@PathVariable("id") Long id) {
        return ResponseEntity.ok(feedPostService.rejectByAdmin(id));
    }

    @PatchMapping("/admin/{id}/hide")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Hide feed post (Admin)", description = "Hide and lock a feed post")
    public ResponseEntity<FeedPostResponse> hideByAdmin(@PathVariable("id") Long id) {
        return ResponseEntity.ok(feedPostService.hideByAdmin(id));
    }

    @PatchMapping("/admin/{id}/content")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Update feed post content (Admin)", description = "Update title/content without author check")
    public ResponseEntity<FeedPostResponse> updateContentByAdmin(@PathVariable("id") Long id,
                                                                 @Valid @RequestBody UpdateFeedPostContentRequest request) {
        return ResponseEntity.ok(feedPostService.updateContentByAdmin(id, request));
    }

    @PostMapping("/admin/sync-counts")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Sync comment counts (Admin)", description = "Recalculate and fix commentCount/replyCount for all posts")
    public ResponseEntity<java.util.Map<String, Object>> syncCommentCounts() {
        int fixed = feedPostService.syncAllCommentCounts();
        return ResponseEntity.ok(java.util.Map.of("fixed", fixed, "message", "Comment counts synced successfully"));
    }

    // =========================================================================
    // Revision History endpoints
    // =========================================================================

    @GetMapping("/{id}/revisions")
    @Operation(
        summary = "Get post revision history",
        description = "Returns paginated history of a post. " +
            "For PUBLISHED posts: accessible by anyone (no auth required). " +
            "For non-PUBLISHED posts: only the post author or STAFF/ADMIN can view."
    )
    public ResponseEntity<Page<FeedPostRevisionResponse>> getRevisions(
            @PathVariable("id") Long id,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {

        if (size < 1) {
            throw new com.trustfund.exception.exceptions.BadRequestException("size must be at least 1");
        }
        int cappedSize = Math.min(size, 50);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long currentUserId = null;
        String currentRole = null;
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            try {
                currentUserId = Long.parseLong(authentication.getName());
            } catch (NumberFormatException ignored) {}
            currentRole = authentication.getAuthorities().stream()
                    .findFirst()
                    .map(a -> a.getAuthority())
                    .orElse(null);
        }

        Pageable pageable = PageRequest.of(page, cappedSize);
        return ResponseEntity.ok(feedPostService.getRevisions(id, currentUserId, currentRole, pageable));
    }

    @GetMapping("/{id}/revisions/{revisionId}")
    @Operation(
        summary = "Get post revision detail",
        description = "Returns a single revision snapshot. Same visibility rules as list."
    )
    public ResponseEntity<FeedPostRevisionResponse> getRevisionById(
            @PathVariable("id") Long id,
            @PathVariable("revisionId") Long revisionId) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long currentUserId = null;
        String currentRole = null;
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            try {
                currentUserId = Long.parseLong(authentication.getName());
            } catch (NumberFormatException ignored) {}
            currentRole = authentication.getAuthorities().stream()
                    .findFirst()
                    .map(a -> a.getAuthority())
                    .orElse(null);
        }

        return ResponseEntity.ok(feedPostService.getRevisionById(id, revisionId, currentUserId, currentRole));
    }

    // =========================================================================
    // Internal cache-eviction endpoint (called by identity-service on profile update)
    // =========================================================================

    @PostMapping("/internal/evict-user-cache/{userId}")
    @Operation(
        summary = "Evict user info cache (Internal)",
        description = "Clears the cached author name/avatar for a user so subsequent post fetches use fresh data. Called by identity-service after a user updates their profile."
    )
    public ResponseEntity<Void> evictUserCache(@PathVariable("userId") Long userId) {
        userInfoClient.evict(userId);
        return ResponseEntity.noContent().build();
    }
}
