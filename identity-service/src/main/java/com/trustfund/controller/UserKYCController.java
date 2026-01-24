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

    @PostMapping("/submit")
    @Operation(summary = "Submit KYC", description = "Submit KYC data for verification")
    public ResponseEntity<KYCResponse> submitKYC(@Valid @RequestBody SubmitKYCRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(userKYCService.submitKYC(userId, request));
    }

    @GetMapping("/me")
    @Operation(summary = "Get my KYC status", description = "Get KYC status of current user")
    public ResponseEntity<KYCResponse> getMyKYC() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(userKYCService.getMyKYC(userId));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update KYC status", description = "Approve or Reject KYC (Admin/Staff only)")
    public ResponseEntity<KYCResponse> updateKYCStatus(@PathVariable Long id,
            @Valid @RequestBody UpdateKYCStatusRequest request) {
        return ResponseEntity.ok(userKYCService.updateKYCStatus(id, request.getStatus(), request.getRejectionReason()));
    }
}
