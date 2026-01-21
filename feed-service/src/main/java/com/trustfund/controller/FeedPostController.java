package com.trustfund.controller;

import com.trustfund.model.request.CreateFeedPostRequest;
import com.trustfund.model.request.UpdateFeedPostContentRequest;
import com.trustfund.model.request.UpdateFeedPostStatusRequest;
import com.trustfund.model.request.UpdateFeedPostVisibilityRequest;
import com.trustfund.model.response.FeedPostResponse;
import com.trustfund.service.interfaceServices.FeedPostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/feed-posts")
@RequiredArgsConstructor
@Tag(name = "Feed Posts", description = "Feed post APIs")
public class FeedPostController {

    private final FeedPostService feedPostService;

    @PostMapping
    @Operation(summary = "Create feed post", description = "Create a new feed post for the authenticated user")
    public ResponseEntity<FeedPostResponse> create(@Valid @RequestBody CreateFeedPostRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long authorId = Long.parseLong(authentication.getName());

        FeedPostResponse response = feedPostService.create(request, authorId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Get active feed posts", description = "Get list of feed posts with status=ACTIVE and visibility rules")
    public ResponseEntity<Page<FeedPostResponse>> getActiveFeedPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long currentUserId = Long.parseLong(authentication.getName());

        String[] sortParts = sort.split(",");
        String sortField = sortParts[0];
        Sort.Direction direction = (sortParts.length > 1 && sortParts[1].equalsIgnoreCase("asc"))
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortField));
        return ResponseEntity.ok(feedPostService.getActiveFeedPosts(currentUserId, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get feed post detail", description = "Get detail of a feed post by id")
    public ResponseEntity<FeedPostResponse> getById(@PathVariable("id") Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long currentUserId = null;
        if (authentication != null) {
            try {
                currentUserId = Long.parseLong(authentication.getName());
            } catch (Exception ignored) {
                currentUserId = null;
            }
        }

        FeedPostResponse response = feedPostService.getById(id, currentUserId);
        return ResponseEntity.ok(response);
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

    @PatchMapping("/{id}")
    @Operation(summary = "Update feed post content", description = "Update title/content of a feed post (author only)")
    public ResponseEntity<FeedPostResponse> updateContent(@PathVariable("id") Long id,
                                                         @Valid @RequestBody UpdateFeedPostContentRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long currentUserId = Long.parseLong(authentication.getName());

        FeedPostResponse response = feedPostService.updateContent(id, currentUserId, request);
        return ResponseEntity.ok(response);
    }
}
