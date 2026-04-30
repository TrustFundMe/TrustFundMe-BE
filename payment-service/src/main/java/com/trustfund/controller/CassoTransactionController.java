package com.trustfund.controller;

import com.trustfund.model.CassoTransaction;
import com.trustfund.service.CassoWebhookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments/casso/transactions")
@RequiredArgsConstructor
public class CassoTransactionController {

    private final CassoWebhookService cassoWebhookService;

    @GetMapping
    public ResponseEntity<List<CassoTransaction>> getAllTransactions() {
        return ResponseEntity.ok(cassoWebhookService.getAllTransactions());
    }

    @GetMapping("/account/{accountNumber}")
    public ResponseEntity<List<CassoTransaction>> getTransactionsByAccount(
            @PathVariable("accountNumber") String accountNumber,
            @RequestParam(value = "bankCode", required = false) String bankCode) {
        return ResponseEntity.ok(cassoWebhookService.getTransactionsByAccount(accountNumber, bankCode));
    }

    @GetMapping("/campaign/{campaignId}")
    public ResponseEntity<List<CassoTransaction>> getTransactionsByCampaign(@PathVariable("campaignId") Long campaignId) {
        return ResponseEntity.ok(cassoWebhookService.getTransactionsByCampaign(campaignId));
    }

    @GetMapping("/debug/enrich/{donationId}")
    public ResponseEntity<?> debugEnrich(@PathVariable("donationId") Long donationId) {
        return ResponseEntity.ok(cassoWebhookService.debugEnrich(donationId));
    }

    @GetMapping("/account/{accountNumber}/since")
    public ResponseEntity<List<CassoTransaction>> getTransactionsSince(
            @PathVariable("accountNumber") String accountNumber,
            @RequestParam("date") @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime date) {
        return ResponseEntity.ok(cassoWebhookService.getTransactionsSince(accountNumber, date));
    }
}
