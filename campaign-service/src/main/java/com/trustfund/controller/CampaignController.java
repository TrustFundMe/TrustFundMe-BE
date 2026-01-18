package com.trustfund.controller;

import com.trustfund.model.Campaign;
import com.trustfund.model.request.CreateCampaignRequest;
import com.trustfund.model.request.UpdateCampaignRequest;
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
    @Operation(
            summary = "Get all campaigns",
            description = "Retrieve a list of all campaigns (Public - no authentication required)"
    )
    public List<Campaign> getAll() {
        return campaignService.getAll();
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get campaign by ID",
            description = "Retrieve a campaign by its ID (Public - no authentication required)"
    )
    public ResponseEntity<Campaign> getById(@PathVariable Long id) {
        return ResponseEntity.ok(campaignService.getById(id));
    }

    @GetMapping("/fund-owner/{fundOwnerId}")
    @Operation(
            summary = "Get campaigns by fund owner",
            description = "Retrieve all campaigns created by a specific fund owner (Public - no authentication required)"
    )
    public List<Campaign> getByFundOwnerId(@PathVariable Long fundOwnerId) {
        return campaignService.getByFundOwnerId(fundOwnerId);
    }

    @PostMapping
    @Operation(
            summary = "Create new campaign",
            description = "Create a new campaign (Authentication required - any authenticated user)"
    )
    public ResponseEntity<Campaign> create(@Valid @RequestBody CreateCampaignRequest request) {
        Campaign created = campaignService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Update campaign",
            description = "Update an existing campaign (Fund Owner, Staff and Admin only)"
    )
    @PreAuthorize("hasAnyRole('FUND_OWNER', 'STAFF', 'ADMIN')")
    public ResponseEntity<Campaign> update(@PathVariable Long id, @Valid @RequestBody UpdateCampaignRequest request) {
        return ResponseEntity.ok(campaignService.update(id, request));
    }

    @PutMapping("/{id}/mark-deleted")
    @Operation(
            summary = "Mark campaign as deleted",
            description = "Soft delete a campaign by setting status to DELETED (Staff and Admin only)"
    )
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<Campaign> markAsDeleted(@PathVariable Long id) {
        return ResponseEntity.ok(campaignService.markAsDeleted(id));
    }
}
