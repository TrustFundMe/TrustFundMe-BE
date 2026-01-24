package com.trustfund.controller;

import com.trustfund.model.enums.KYCStatus;
import com.trustfund.model.request.SubmitKYCRequest;
import com.trustfund.model.request.UpdateKYCStatusRequest;
import com.trustfund.model.response.KYCResponse;
import com.trustfund.service.interfaceServices.UserKYCService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/kyc")
@RequiredArgsConstructor
@Tag(name = "User KYC", description = "User KYC APIs")
public class UserKYCController {

    private final UserKYCService userKYCService;

    @PostMapping("/users/{userId}")
    @Operation(summary = "Submit KYC for User (Staff Only)", description = "Staff inputs KYC data for a user to verify them")
    public ResponseEntity<KYCResponse> submitKYC(@PathVariable Long userId,
            @Valid @RequestBody SubmitKYCRequest request) {
        // Access control: Ensure logged-in user is STAFF or ADMIN handled by Security
        // Config or Gateway
        return ResponseEntity.ok(userKYCService.submitKYC(userId, request));
    }

    @PutMapping("/users/{userId}")
    @Operation(summary = "Update KYC for User (Staff Only)", description = "Staff updates/resubmits KYC data for a user")
    public ResponseEntity<KYCResponse> resubmitKYC(@PathVariable Long userId,
            @Valid @RequestBody SubmitKYCRequest request) {
        return ResponseEntity.ok(userKYCService.resubmitKYC(userId, request));
    }

    @GetMapping("/me")
    @Operation(summary = "Get my KYC status", description = "Get KYC status of current user")
    public ResponseEntity<KYCResponse> getMyKYC() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(userKYCService.getMyKYC(userId));

    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get KYC by user ID", description = "Get KYC details of a specific user (Admin/Staff only)")
    public ResponseEntity<KYCResponse> getKYCByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(userKYCService.getKYCByUserId(userId));
    }

    @GetMapping("/pending")
    @Operation(summary = "Get pending KYC requests", description = "Get list of pending KYC requests (Admin/Staff only)")
    public ResponseEntity<org.springframework.data.domain.Page<KYCResponse>> getPendingKYCRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        String[] sortParts = sort.split(",");
        String sortField = sortParts[0];
        org.springframework.data.domain.Sort.Direction direction = (sortParts.length > 1
                && sortParts[1].equalsIgnoreCase("asc"))
                        ? org.springframework.data.domain.Sort.Direction.ASC
                        : org.springframework.data.domain.Sort.Direction.DESC;

        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size,
                org.springframework.data.domain.Sort.by(direction, sortField));
        return ResponseEntity.ok(userKYCService.getPendingKYCRequests(pageable));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update KYC status", description = "Approve or Reject KYC (Admin/Staff only)")
    public ResponseEntity<KYCResponse> updateKYCStatus(@PathVariable Long id,
            @Valid @RequestBody UpdateKYCStatusRequest request) {
        return ResponseEntity.ok(userKYCService.updateKYCStatus(id, request.getStatus(), request.getRejectionReason()));
    }
}
