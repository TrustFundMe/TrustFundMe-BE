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
    @Operation(summary = "Submit a report (Flag)", description = "User reports a postId or campaignId with a reason")
    public ResponseEntity<FlagResponse> submitFlag(@Valid @RequestBody FlagRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(flagService.submitFlag(userId, request));
    }

    @GetMapping("/pending")
    @Operation(summary = "Get pending reports", description = "Admin/Staff list all reports with PENDING status")
    public ResponseEntity<Page<FlagResponse>> getPending(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(flagService.getPendingFlags(pageable));
    }

    @PatchMapping("/{id}/review")
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
