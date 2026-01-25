package com.trustfund.controller;

import com.trustfund.model.request.FlagRequest;
import com.trustfund.model.response.FlagResponse;
import com.trustfund.service.FlagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/flags")
@RequiredArgsConstructor
@Tag(name = "Moderation (Flag)", description = "Report campaigns or posts for review")
public class FlagController {

    private final FlagService flagService;

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Submit a report (Flag)", description = "User reports a postId or campaignId with a reason. Only ROLE_USER can report.")
    public ResponseEntity<FlagResponse> submitFlag(@Valid @RequestBody FlagRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(flagService.submitFlag(userId, request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Get flag by ID", description = "View details of a specific report")
    public ResponseEntity<FlagResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(flagService.getFlagById(id));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Get pending reports", description = "Admin/Staff list all reports with PENDING status")
    public ResponseEntity<Page<FlagResponse>> getPending(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(flagService.getPendingFlags(pageable));
    }

    @GetMapping("/posts/{postId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Get flags by Post ID", description = "Admin/Staff view all reports for a specific post")
    public ResponseEntity<Page<FlagResponse>> getByPostId(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(flagService.getFlagsByPostId(postId, pageable));
    }

    @GetMapping("/campaigns/{campaignId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Get flags by Campaign ID", description = "Admin/Staff view all reports for a specific campaign")
    public ResponseEntity<Page<FlagResponse>> getByCampaignId(
            @PathVariable Long campaignId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(flagService.getFlagsByCampaignId(campaignId, pageable));
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get my reports", description = "User views their own submitted reports")
    public ResponseEntity<Page<FlagResponse>> getMyFlags(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = Long.parseLong(authentication.getName());
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(flagService.getFlagsByUserId(userId, pageable));
    }

    @PatchMapping("/{id}/review")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Review a report", description = "Admin/Staff resolve or dismiss a report")
    public ResponseEntity<FlagResponse> review(
            @PathVariable Long id,
            @RequestParam String status // RESOLVED, DISMISSED, etc.
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long adminId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(flagService.reviewFlag(id, adminId, status));
    }
}
