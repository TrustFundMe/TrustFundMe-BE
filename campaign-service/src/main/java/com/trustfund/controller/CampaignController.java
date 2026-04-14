package com.trustfund.controller;

import com.trustfund.model.request.CreateCampaignRequest;
import com.trustfund.model.request.UpdateCampaignRequest;
import com.trustfund.model.response.CampaignResponse;
import com.trustfund.service.CampaignService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    @Operation(summary = "Get all campaigns (paginated)", description = "Retrieve a paginated list of all campaigns (Public - no authentication required)")
    public Page<CampaignResponse> getAll(Pageable pageable) {
        return campaignService.getAll(pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get campaign by ID", description = "Retrieve a campaign by its ID (Public - no authentication required)")
    public ResponseEntity<CampaignResponse> getById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(campaignService.getById(id));
    }

    @GetMapping("/fund-owner/{fundOwnerId}")
    @Operation(summary = "Get campaigns by fund owner", description = "Retrieve all campaigns created by a specific fund owner (Public - no authentication required)")
    public List<CampaignResponse> getByFundOwnerId(@PathVariable("fundOwnerId") Long fundOwnerId) {
        return campaignService.getByFundOwnerId(fundOwnerId);
    }

    @GetMapping("/fund-owner/{fundOwnerId}/paginated")
    @Operation(summary = "Get paginated campaigns by fund owner", description = "Retrieve campaigns created by a specific fund owner with pagination (Public)")
    public Page<CampaignResponse> getByFundOwnerIdPaginated(
            @PathVariable("fundOwnerId") Long fundOwnerId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "6") int size) {
        return campaignService.getByFundOwnerIdPaginated(fundOwnerId,
                org.springframework.data.domain.PageRequest.of(page, size, org.springframework.data.domain.Sort
                        .by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt")));
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
    public ResponseEntity<CampaignResponse> update(@PathVariable("id") Long id,
            @Valid @RequestBody UpdateCampaignRequest request) {
        return ResponseEntity.ok(campaignService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Mark campaign as deleted", description = "Delete a campaign (Staff and Admin only)")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<CampaignResponse> markAsDeleted(@PathVariable("id") Long id) {
        return ResponseEntity.ok(campaignService.markAsDeleted(id));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get campaigns by status", description = "Retrieve all campaigns with a specific status (Staff and Admin only)")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public List<CampaignResponse> getByStatus(@PathVariable("status") String status) {
        return campaignService.getByStatus(status);
    }

    @GetMapping("/category/{categoryId}")
    @Operation(summary = "Get campaigns by category", description = "Retrieve all campaigns for a specific category (Public)")
    public List<CampaignResponse> getByCategoryId(@PathVariable("categoryId") Long categoryId) {
        return campaignService.getByCategoryId(categoryId);
    }

    @PutMapping("/{id}/review")
    @Operation(summary = "Review campaign", description = "Staff or Admin reviews a campaign and provides feedback (APPROVED/REJECTED)")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<CampaignResponse> reviewCampaign(
            @PathVariable("id") Long id,
            @Valid @RequestBody com.trustfund.model.request.ReviewCampaignRequest request,
            org.springframework.security.core.Authentication authentication) {

        Long staffId = Long.parseLong(authentication.getName());
        return ResponseEntity
                .ok(campaignService.reviewCampaign(id, staffId, request.getStatus(), request.getRejectionReason()));
    }

    @PutMapping("/{id}/update-balance")
    @Operation(summary = "Update campaign balance", description = "Adds an amount to the campaign's current balance (Internal use)")
    public ResponseEntity<Void> updateBalance(
            @PathVariable("id") Long id,
            @RequestParam("amount") java.math.BigDecimal amount) {
        campaignService.updateBalance(id, amount);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/pause")
    @Operation(summary = "Pause campaign", description = "Admin or Staff pauses an active campaign")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<CampaignResponse> pauseCampaign(@PathVariable("id") Long id) {
        return ResponseEntity.ok(campaignService.pauseCampaign(id));
    }

    @PutMapping("/{id}/close")
    @Operation(summary = "Close campaign", description = "Admin or Staff closes a campaign completely")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<CampaignResponse> closeCampaign(
            @PathVariable("id") Long id,
            org.springframework.security.core.Authentication authentication) {
        Long staffId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(campaignService.closeCampaign(id, staffId));
    }

    @GetMapping("/fund-owner/{fundOwnerId}/count")
    @Operation(summary = "Lấy tổng số chiến dịch của chủ sở hữu", description = "Trả về một số nguyên đơn giản")
    public ResponseEntity<Long> getCampaignCount(@PathVariable("fundOwnerId") Long fundOwnerId) {
        return ResponseEntity.ok(campaignService.getCampaignCountByFundOwner(fundOwnerId));
    }
}
