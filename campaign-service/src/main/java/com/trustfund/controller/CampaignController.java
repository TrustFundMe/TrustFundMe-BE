package com.trustfund.controller;

import com.trustfund.model.request.CreateCampaignRequest;
import com.trustfund.model.request.UpdateCampaignRequest;
import com.trustfund.model.response.CampaignResponse; // Added import
import com.trustfund.service.CampaignService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/campaigns")
@RequiredArgsConstructor
@Tag(name = "Campaigns", description = "API quản lý chiến dịch")
public class CampaignController {

    private final CampaignService campaignService;

    @GetMapping
    @Operation(summary = "Get all campaigns", description = "Retrieve a list of all campaigns (Public - no authentication required)")
    public List<CampaignResponse> getAll() {
        return campaignService.getAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get campaign by ID", description = "Retrieve a campaign by its ID (Public - no authentication required)")
    public ResponseEntity<CampaignResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(campaignService.getById(id));
    }

    @GetMapping("/fund-owner/{fundOwnerId}")
    @Operation(summary = "Get campaigns by fund owner", description = "Retrieve all campaigns created by a specific fund owner (Public - no authentication required)")
    public List<CampaignResponse> getByFundOwnerId(@PathVariable Long fundOwnerId) {
        return campaignService.getByFundOwnerId(fundOwnerId);
    }

    @PostMapping
    @Operation(summary = "Create new campaign", description = "Create a new campaign (Authentication required - any authenticated user)")
    public ResponseEntity<CampaignResponse> create(@Valid @RequestBody CreateCampaignRequest request) {
        CampaignResponse created = campaignService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update campaign", description = "Update an existing campaign (Fund Owner, Staff and Admin only)")
    @PreAuthorize("hasAnyRole('FUND_OWNER', 'STAFF', 'ADMIN')")
    public ResponseEntity<CampaignResponse> update(@PathVariable Long id,
            @Valid @RequestBody UpdateCampaignRequest request) {
        return ResponseEntity.ok(campaignService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Mark campaign as deleted", description = "Delete a campaign (Staff and Admin only)")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<CampaignResponse> markAsDeleted(@PathVariable Long id) {
        return ResponseEntity.ok(campaignService.markAsDeleted(id));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get campaigns by status", description = "Retrieve all campaigns with a specific status (Staff and Admin only)")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public List<CampaignResponse> getByStatus(@PathVariable String status) {
        return campaignService.getByStatus(status);
    }

    @PutMapping("/{id}/review")
    @Operation(summary = "Review campaign", description = "Staff or Admin reviews a campaign and provides feedback (APPROVED/REJECTED)")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<CampaignResponse> reviewCampaign(
            @PathVariable Long id,
            @Valid @RequestBody com.trustfund.model.request.ReviewCampaignRequest request,
            org.springframework.security.core.Authentication authentication) {

        Long staffId = Long.parseLong(authentication.getName());
        return ResponseEntity
                .ok(campaignService.reviewCampaign(id, staffId, request.getStatus(), request.getRejectionReason()));
    }
}
