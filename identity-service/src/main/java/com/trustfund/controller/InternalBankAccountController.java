package com.trustfund.controller;

import com.trustfund.model.response.BankAccountResponse;
import com.trustfund.service.interfaceServices.BankAccountService;
import com.trustfund.utils.BankCodeNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/internal/bank-accounts")
@RequiredArgsConstructor
public class InternalBankAccountController {

    private final BankAccountService bankAccountService;

    @GetMapping("/by-account-number")
    public ResponseEntity<BankAccountResponse> getByAccountNumber(
            @RequestParam("accountNumber") String accountNumber,
            @RequestParam(value = "bankCode", required = false) String bankCode) {

        String trimmedAccount = accountNumber.trim();

        if (bankCode != null && !bankCode.isBlank()) {
            String normalizedCode = BankCodeNormalizer.normalize(bankCode.trim());
            Optional<BankAccountResponse> result = bankAccountService.findByAccountNumberAndBankCode(trimmedAccount,
                    normalizedCode);
            if (result.isPresent()) {
                return ResponseEntity.ok(result.get());
            }
            // Fallback: try lookup by accountNumber only (handles legacy data with
            // mismatched bankCode)
            return bankAccountService.findByAccountNumber(trimmedAccount)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        }

        return bankAccountService.findByAccountNumber(trimmedAccount)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/by-campaign-id")
    public ResponseEntity<BankAccountResponse> getByCampaignId(@RequestParam("campaignId") Long campaignId) {
        return bankAccountService.findByCampaignId(campaignId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
