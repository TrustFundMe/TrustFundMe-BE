package com.trustfund.controller;

import com.trustfund.model.request.CreateBankAccountRequest;
import com.trustfund.model.request.UpdateBankAccountRequest;
import com.trustfund.model.request.UpdateBankAccountStatusRequest;
import com.trustfund.model.response.BankAccountResponse;
import com.trustfund.service.interfaceServices.BankAccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bank-accounts")
@RequiredArgsConstructor
@Tag(name = "Bank Accounts", description = "Bank account APIs")
public class BankAccountController {

    private final BankAccountService bankAccountService;

    @GetMapping
    @Operation(summary = "Get my bank accounts", description = "Get all bank accounts linked by current authenticated user")
    public ResponseEntity<List<BankAccountResponse>> getMyBankAccounts() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = Long.parseLong(authentication.getName());

        return ResponseEntity.ok(bankAccountService.getMyBankAccounts(userId));
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    @Operation(summary = "Get all bank accounts", description = "Get all bank accounts in database (STAFF/ADMIN only)")
    public ResponseEntity<List<BankAccountResponse>> getAllBankAccounts() {
        return ResponseEntity.ok(bankAccountService.getAllBankAccounts());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get bank account by ID", description = "Get details of a specific bank account")
    public ResponseEntity<BankAccountResponse> getById(@PathVariable("id") Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long currentUserId = Long.parseLong(authentication.getName());
        String currentRole = getRole(authentication);

        return ResponseEntity.ok(bankAccountService.getById(id, currentUserId, currentRole));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get bank accounts by user ID", description = "Get all bank accounts of a specific user (own accounts or STAFF/ADMIN)")
    public ResponseEntity<List<BankAccountResponse>> getByUserId(@PathVariable("userId") Long userId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long currentUserId = Long.parseLong(authentication.getName());
        String currentRole = getRole(authentication);

        // Allow if viewing own accounts or if STAFF/ADMIN
        boolean isOwner = currentUserId.equals(userId);
        boolean isStaffOrAdmin = currentRole != null
                && (currentRole.contains("STAFF") || currentRole.contains("ADMIN"));

        if (!isOwner && !isStaffOrAdmin) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(bankAccountService.getByUserId(userId));
    }

    @PostMapping
    @Operation(summary = "Create bank account", description = "Create a new bank account for the authenticated user")
    public ResponseEntity<BankAccountResponse> create(@Valid @RequestBody CreateBankAccountRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userIdStr = authentication.getName();

        BankAccountResponse response = bankAccountService.create(request, userIdStr);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update bank account", description = "Update bank account details (bank code, account number, name)")
    public ResponseEntity<BankAccountResponse> update(@PathVariable("id") Long id,
            @Valid @RequestBody UpdateBankAccountRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long currentUserId = Long.parseLong(authentication.getName());
        String currentRole = getRole(authentication);

        BankAccountResponse response = bankAccountService.update(id, request, currentUserId, currentRole);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update bank account status", description = "Update status and verification for a bank account")
    public ResponseEntity<BankAccountResponse> updateStatus(@PathVariable("id") Long id,
            @Valid @RequestBody UpdateBankAccountStatusRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long currentUserId = Long.parseLong(authentication.getName());
        String currentRole = getRole(authentication);

        BankAccountResponse response = bankAccountService.updateStatus(id, request, currentUserId, currentRole);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete bank account", description = "Delete a bank account")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long currentUserId = Long.parseLong(authentication.getName());
        String currentRole = getRole(authentication);

        bankAccountService.delete(id, currentUserId, currentRole);
        return ResponseEntity.noContent().build();
    }

    private String getRole(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .findFirst()
                .map(grantedAuthority -> grantedAuthority.getAuthority())
                .orElse(null);
    }
}
