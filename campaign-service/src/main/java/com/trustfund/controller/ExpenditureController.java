package com.trustfund.controller;

import com.trustfund.model.Expenditure;
import com.trustfund.model.request.CreateExpenditureRequest;
import com.trustfund.model.request.UpdateExpenditureRequest;
import com.trustfund.service.ExpenditureService;
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
@RequestMapping("/api/expenditures")
@RequiredArgsConstructor
@Tag(name = "Expenditures", description = "API quản lý chi tiêu (Expenditures)")
public class ExpenditureController {

    private final ExpenditureService expenditureService;

    @GetMapping
    @Operation(
            summary = "Get all expenditures",
            description = "Retrieve a list of all expenditures (Staff and Admin only)"
    )
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public List<Expenditure> getAll() {
        return expenditureService.getAll();
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get expenditure by ID",
            description = "Retrieve an expenditure by its ID (Public - no authentication required)"
    )
    public ResponseEntity<Expenditure> getById(@PathVariable Long id) {
        return ResponseEntity.ok(expenditureService.getById(id));
    }

    @GetMapping("/campaign/{campaignId}")
    @Operation(
            summary = "Get expenditures by campaign ID",
            description = "Retrieve all expenditures for a specific campaign (Public - no authentication required)"
    )
    public List<Expenditure> getByCampaignId(@PathVariable Long campaignId) {
        return expenditureService.getByCampaignId(campaignId);
    }

    @PostMapping
    @Operation(summary = "Create new expenditure")
    @PreAuthorize("hasAnyRole('FUND_OWNER', 'ADMIN', 'STAFF')")
    public ResponseEntity<Expenditure> create(@Valid @RequestBody CreateExpenditureRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(expenditureService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update expenditure")
    @PreAuthorize("hasAnyRole('FUND_OWNER', 'ADMIN', 'STAFF')")
    public ResponseEntity<Expenditure> update(@PathVariable Long id, @Valid @RequestBody UpdateExpenditureRequest request) {
        return ResponseEntity.ok(expenditureService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete expenditure")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        expenditureService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
