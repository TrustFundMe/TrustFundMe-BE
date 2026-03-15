package com.trustfund.controller;

import com.trustfund.model.request.CreateFeedPostCommentRequest;
import com.trustfund.model.request.UpdateFeedPostCommentRequest;
import com.trustfund.model.response.FeedPostCommentResponse;
import com.trustfund.service.interfaceServices.FeedPostCommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/feed-posts")
@RequiredArgsConstructor
@Tag(name = "Feed Post Comments", description = "Feed post comment APIs")
public class FeedPostCommentController {

    private final FeedPostCommentService feedPostCommentService;

    @PostMapping("/{postId}/comments")
    @Operation(summary = "Create comment", description = "Create a new comment on a feed post")
    public ResponseEntity<FeedPostCommentResponse> create(
            @PathVariable("postId") Long postId,
            @Valid @RequestBody CreateFeedPostCommentRequest request) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long authorId = Long.parseLong(authentication.getName());

        FeedPostCommentResponse response = feedPostCommentService.create(postId, request, authorId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{postId}/comments")
    @Operation(summary = "Get comments", description = "Get root-level comments with nested replies for a feed post")
    public ResponseEntity<Page<FeedPostCommentResponse>> getComments(
            @PathVariable("postId") Long postId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "sort", defaultValue = "createdAt,desc") String sort) {

        Long currentUserId = null;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            try {
                currentUserId = Long.parseLong(authentication.getName());
            } catch (NumberFormatException ignored) {
            }
        }

        String[] sortParts = sort.split(",");
        String sortField = sortParts[0];
        Sort.Direction direction = (sortParts.length > 1 && sortParts[1].equalsIgnoreCase("asc"))
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortField));
        return ResponseEntity.ok(feedPostCommentService.getCommentsByPostId(postId, currentUserId, pageable));
    }

    @PutMapping("/comments/{commentId}")
    @Operation(summary = "Update comment", description = "Update an existing comment")
    public ResponseEntity<FeedPostCommentResponse> update(
            @PathVariable("commentId") Long commentId,
            @Valid @RequestBody UpdateFeedPostCommentRequest request) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long currentUserId = Long.parseLong(authentication.getName());

        FeedPostCommentResponse response = feedPostCommentService.update(commentId, currentUserId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/comments/{commentId}")
    @Operation(summary = "Delete comment", description = "Delete an existing comment")
    public ResponseEntity<Void> delete(@PathVariable("commentId") Long commentId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long currentUserId = Long.parseLong(authentication.getName());

        feedPostCommentService.delete(commentId, currentUserId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/comments/{commentId}/like")
    @Operation(summary = "Toggle comment like", description = "Like or unlike a comment")
    public ResponseEntity<FeedPostCommentResponse> toggleLike(@PathVariable("commentId") Long commentId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long currentUserId = Long.parseLong(authentication.getName());

        FeedPostCommentResponse response = feedPostCommentService.toggleLike(commentId, currentUserId);
        return ResponseEntity.ok(response);
    }
}
