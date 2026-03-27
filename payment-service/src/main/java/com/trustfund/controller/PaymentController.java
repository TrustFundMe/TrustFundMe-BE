package com.trustfund.controller;

import com.trustfund.dto.request.CreatePaymentRequest;
import com.trustfund.dto.response.PaymentResponse;
import com.trustfund.service.DonationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final DonationService donationService;

    @PostMapping("/create")
    public ResponseEntity<?> createPayment(@RequestBody CreatePaymentRequest request) {
        try {
            PaymentResponse response = donationService.createPayment(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Payment creation failed", e);
            return ResponseEntity.internalServerError().body(
                    java.util.Map.of(
                            "error", e.getClass().getSimpleName(),
                            "message", e.getMessage() != null ? e.getMessage() : "Unknown error",
                            "cause", e.getCause() != null ? e.getCause().getMessage() : "No cause"));
        }
    }

    @PostMapping("/webhook")
    public ResponseEntity<?> handleWebhook(@RequestBody Map<String, Object> webhookBody) {
        log.info("====== PayOS WEBHOOK RECEIVED ======");
        log.info("Payload: {}", webhookBody);
        try {
            donationService.handleWebhook(webhookBody);
            log.info("Outcome: SUCCESS");
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Outcome: ERROR - {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/donation/{id}")
    public ResponseEntity<?> getDonation(@PathVariable("id") Long id) {
        try {
            return ResponseEntity.ok(donationService.getDonation(id));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/donation/{id}/verify")
    public ResponseEntity<?> verifyPayment(@PathVariable("id") Long id) {
        try {
            donationService.verifyPayment(id);
            return ResponseEntity.ok(Map.of("message", "Payment verified and synchronized"));
        } catch (Exception e) {
            log.error("Payment verification failed", e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/expenditure-item/{id}/check")
    public ResponseEntity<?> checkExpenditureItem(@PathVariable("id") Long id,
            @RequestParam(name = "quantity", required = false) Integer quantity) {
        try {
            return ResponseEntity.ok(donationService.checkExpenditureItemLimit(id, quantity));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/campaign/{campaignId}/progress")
    public ResponseEntity<?> getCampaignProgress(@PathVariable("campaignId") Long campaignId) {
        try {
            return ResponseEntity.ok(donationService.getCampaignProgress(campaignId));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/campaign/{campaignId}/recent-donations")
    public ResponseEntity<?> getRecentDonors(@PathVariable("campaignId") Long campaignId,
            @RequestParam(name = "limit", defaultValue = "3") int limit) {
        try {
            return ResponseEntity.ok(donationService.getRecentDonors(campaignId, limit));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/my-donations")
    public ResponseEntity<?> getMyDonations(
            @RequestParam(name = "limit", defaultValue = "50") int limit) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || authentication.getName() == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Authentication required"));
            }
            Long donorId = Long.parseLong(authentication.getName());
            return ResponseEntity.ok(donationService.getMyPaidDonations(donorId, limit));
        } catch (NumberFormatException e) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid authentication principal"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}
