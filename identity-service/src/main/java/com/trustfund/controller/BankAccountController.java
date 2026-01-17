package com.trustfund.controller;

import com.trustfund.model.request.CreateBankAccountRequest;
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

    @PostMapping
    @Operation(summary = "Create bank account", description = "Create a new bank account for the authenticated user")
    public ResponseEntity<BankAccountResponse> create(@Valid @RequestBody CreateBankAccountRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        BankAccountResponse response = bankAccountService.create(request, email);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
