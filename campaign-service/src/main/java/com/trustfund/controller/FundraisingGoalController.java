package com.trustfund.controller;

import com.trustfund.model.FundraisingGoal;
import com.trustfund.model.request.CreateFundraisingGoalRequest;
import com.trustfund.model.request.UpdateFundraisingGoalRequest;
import com.trustfund.service.FundraisingGoalService;
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
@RequestMapping("/api/fundraising-goals")
@RequiredArgsConstructor
@Tag(name = "Fundraising Goals", description = "API quản lý mục tiêu gây quỹ")
public class FundraisingGoalController {

    private final FundraisingGoalService fundraisingGoalService;

    @GetMapping
    @Operation(summary = "Get all fundraising goals", description = "Retrieve a list of all fundraising goals (Public - no authentication required)")
    public List<FundraisingGoal> getAll() {
        return fundraisingGoalService.getAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get fundraising goal by ID", description = "Retrieve a fundraising goal by its ID (Public - no authentication required)")
    public ResponseEntity<FundraisingGoal> getById(@PathVariable Long id) {
        return ResponseEntity.ok(fundraisingGoalService.getById(id));
    }

    @GetMapping("/campaign/{campaignId}")
    @Operation(summary = "Get fundraising goals by campaign", description = "Retrieve all fundraising goals for a specific campaign (Public - no authentication required)")
    public List<FundraisingGoal> getByCampaignId(@PathVariable Long campaignId) {
        return fundraisingGoalService.getByCampaignId(campaignId);
    }

    @PostMapping
    @Operation(summary = "Create new fundraising goal", description = "Create a new fundraising goal (Authentication required - any authenticated user)")
    public ResponseEntity<FundraisingGoal> create(@Valid @RequestBody CreateFundraisingGoalRequest request) {
        FundraisingGoal created = fundraisingGoalService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update fundraising goal", description = "Update an existing fundraising goal (Fund Owner, Staff and Admin only)")
    @PreAuthorize("hasAnyRole('FUND_OWNER', 'STAFF', 'ADMIN')")
    public ResponseEntity<FundraisingGoal> update(@PathVariable Long id,
            @Valid @RequestBody UpdateFundraisingGoalRequest request) {
        return ResponseEntity.ok(fundraisingGoalService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete fundraising goal", description = "Delete a fundraising goal (Staff and Admin only)")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        fundraisingGoalService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
