package com.trustfund.controller;

import com.trustfund.service.interfaceServices.UserPostSeenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/user-post-seen")
@Tag(name = "User Post Seen", description = "Track which posts a user has seen")
@RequiredArgsConstructor
public class UserPostSeenController {

    private final UserPostSeenService userPostSeenService;

    @PostMapping
    @Operation(summary = "Mark post(s) as seen", description = "Mark one or more posts as seen by the authenticated user")
    public ResponseEntity<Map<String, Object>> markSeen(@RequestBody Map<String, Object> body) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.isAuthenticated() == false || "anonymousUser".equals(auth.getPrincipal())) {
            return ResponseEntity.ok(Map.of("new", false)); // anonymous — no-op
        }
        Long userId = Long.parseLong(auth.getName());

        Object postIdObj = body.get("postId");
        Object postIdsObj = body.get("postIds");

        if (postIdsObj instanceof java.util.List<?> list) {
            java.util.List<Long> ids = list.stream()
                    .filter(Number.class::isInstance)
                    .map(id -> ((Number) id).longValue())
                    .toList();
            userPostSeenService.markSeenBatch(userId, ids);
            return ResponseEntity.ok(Map.of("new", true));
        } else if (postIdObj instanceof Number num) {
            boolean isNew = userPostSeenService.markSeen(userId, num.longValue());
            return ResponseEntity.ok(Map.of("new", isNew));
        }

        return ResponseEntity.ok(Map.of("new", false));
    }

    @GetMapping
    @Operation(summary = "Get seen post IDs", description = "Get all post IDs the authenticated user has seen")
    public ResponseEntity<Set<Long>> getSeenPostIds() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.isAuthenticated() == false || "anonymousUser".equals(auth.getPrincipal())) {
            return ResponseEntity.ok(Set.of());
        }
        Long userId = Long.parseLong(auth.getName());
        return ResponseEntity.ok(userPostSeenService.getSeenPostIds(userId));
    }

    @GetMapping("/{postId}")
    @Operation(summary = "Check if post is seen", description = "Check if a specific post is seen by the authenticated user")
    public ResponseEntity<Map<String, Boolean>> isSeen(@PathVariable Long postId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.isAuthenticated() == false || "anonymousUser".equals(auth.getPrincipal())) {
            return ResponseEntity.ok(Map.of("seen", false));
        }
        Long userId = Long.parseLong(auth.getName());
        return ResponseEntity.ok(Map.of("seen", userPostSeenService.isSeen(userId, postId)));
    }
}
