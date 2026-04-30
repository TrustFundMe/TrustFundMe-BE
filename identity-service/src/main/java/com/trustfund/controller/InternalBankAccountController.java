package com.trustfund.controller;

import com.trustfund.model.response.BankAccountResponse;
import com.trustfund.service.interfaceServices.BankAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/bank-accounts")
@RequiredArgsConstructor
public class InternalBankAccountController {

    private final BankAccountService bankAccountService;

    @GetMapping("/by-account-number")
    public ResponseEntity<BankAccountResponse> getByAccountNumber(
            @RequestParam("accountNumber") String accountNumber,
            @RequestParam(value = "bankCode", required = false) String bankCode) {
        
        if (bankCode != null && !bankCode.isBlank()) {
            return bankAccountService.findByAccountNumberAndBankCode(accountNumber.trim(), bankCode.trim())
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        }
        
        return bankAccountService.findByAccountNumber(accountNumber.trim())
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
