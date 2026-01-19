package com.trustfund.controller;

import com.trustfund.model.request.CreateFeedPostRequest;
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
}
