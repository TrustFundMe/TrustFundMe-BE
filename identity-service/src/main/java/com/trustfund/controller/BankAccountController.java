package com.trustfund.controller;

import com.trustfund.model.request.CreateBankAccountRequest;
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

@RestController
@RequestMapping("/api/bank-accounts")
@RequiredArgsConstructor
@Tag(name = "Bank Accounts", description = "Bank account APIs")
public class BankAccountController {

    private final BankAccountService bankAccountService;

    @GetMapping
    @Operation(summary = "Get my bank accounts", description = "Get all bank accounts linked by current authenticated user")
    public ResponseEntity<java.util.List<BankAccountResponse>> getMyBankAccounts() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userIdStr = authentication.getName();
        Long userId = Long.parseLong(userIdStr);

        return ResponseEntity.ok(bankAccountService.getMyBankAccounts(userId));
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    @Operation(summary = "Get all bank accounts", description = "Get all bank accounts in database (STAFF/ADMIN only)")
    public ResponseEntity<java.util.List<BankAccountResponse>> getAllBankAccounts() {
        return ResponseEntity.ok(bankAccountService.getAllBankAccounts());
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update bank account status", description = "Update status and verification for a bank account")
    public ResponseEntity<BankAccountResponse> updateStatus(@PathVariable("id") Long id,
                                                           @Valid @RequestBody UpdateBankAccountStatusRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long currentUserId = Long.parseLong(authentication.getName());

        String currentRole = authentication.getAuthorities().stream()
                .findFirst()
                .map(grantedAuthority -> grantedAuthority.getAuthority())
                .orElse(null);

        BankAccountResponse response = bankAccountService.updateStatus(id, request, currentUserId, currentRole);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @Operation(summary = "Create bank account", description = "Create a new bank account for the authenticated user")
    public ResponseEntity<BankAccountResponse> create(@Valid @RequestBody CreateBankAccountRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        BankAccountResponse response = bankAccountService.create(request, email);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
