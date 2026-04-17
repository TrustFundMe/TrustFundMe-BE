package com.trustfund.controller;

import com.trustfund.dto.request.CreatePaymentRequest;
import com.trustfund.dto.response.CampaignAnalyticsResponse;
import com.trustfund.dto.response.PaymentResponse;
import com.trustfund.service.DonationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.List;
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

    @PostMapping("/donation/{id}/sync-balance")
    public ResponseEntity<?> syncBalance(@PathVariable("id") Long id) {
        try {
            donationService.syncBalanceForDonation(id);
            return ResponseEntity.ok(Map.of("message", "Balance synced successfully"));
        } catch (Exception e) {
            log.error("Sync balance failed for donation {}", id, e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/donation/{id}/sync-quantity")
    public ResponseEntity<?> syncQuantity(@PathVariable("id") Long id) {
        try {
            donationService.syncQuantityForDonation(id);
            return ResponseEntity.ok(Map.of("message", "Quantity synced successfully"));
        } catch (Exception e) {
            log.error("Sync quantity failed for donation {}", id, e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/expenditure-item/{id}/check")
    public ResponseEntity<?> checkExpenditureItem(@PathVariable("id") Long id,
            @RequestParam(name = "quantity", required = false) Integer quantity) {
        if (id == null || id <= 0) {
            log.error("Bad request for item {}: Invalid ID", id);
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid expenditure item ID: " + id));
        }
        try {
            return ResponseEntity.ok(donationService.checkExpenditureItemLimit(id, quantity));
        } catch (Exception e) {
            // Should not reach here anymore since service handles all exceptions internally
            // But keep as safety net for unexpected errors
            log.error("Unexpected error checking item {}: {}", id, e.getMessage(), e);
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

    @GetMapping("/campaign/{campaignId}/analytics")
    public ResponseEntity<?> getCampaignAnalytics(@PathVariable("campaignId") Long campaignId,
            @RequestParam(name = "period", defaultValue = "Tháng") String period) {
        try {
            CampaignAnalyticsResponse response = donationService.getCampaignAnalytics(campaignId, period);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to fetch campaign analytics for id: {}", campaignId, e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Internal Server Error",
                    "message", e.getMessage()));
        }
    }

    @GetMapping("/donations/summary")
    public ResponseEntity<?> getDonationSummary(
            @RequestParam("expenditureItemIds") String expenditureItemIdsStr) {
        try {
            log.info("getDonationSummary called with: {}", expenditureItemIdsStr);
            List<Long> ids = java.util.Arrays.stream(expenditureItemIdsStr.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Long::parseLong)
                    .collect(java.util.stream.Collectors.toList());
            log.info("Parsed ids: {}", ids);
            List<Map<String, Object>> result = donationService.getDonationSummaryByExpenditureItems(ids);
            log.info("Returning {} entries", result.size());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Failed to get donation summary: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<?> getDonationsByStatus(@PathVariable("status") String status) {
        try {
            return ResponseEntity.ok(donationService.getDonationsByStatus(status));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/status/{status}/paginated")
    public ResponseEntity<?> getDonationsByStatusPaginated(
            @PathVariable("status") String status,
            Pageable pageable) {
        try {
            return ResponseEntity.ok(donationService.getDonationsByStatusPaginated(status, pageable));
        } catch (Exception e) {
            log.error("Failed to fetch paginated donations", e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/donations/item/{expenditureItemId}/exists")
    public ResponseEntity<Boolean> existsDonationItem(@PathVariable("expenditureItemId") Long expenditureItemId) {
        return ResponseEntity.ok(donationService.existsDonationItem(expenditureItemId));
    }

    @GetMapping("/expenditure-item/{itemId}/donors")
    public ResponseEntity<?> getDonorsByItem(@PathVariable("itemId") Long itemId) {
        try {
            return ResponseEntity.ok(donationService.getDonorsByItem(itemId));
        } catch (Exception e) {
            log.error("Failed to fetch donors for item {}", itemId, e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/user/{userId}/donation-count")
    public ResponseEntity<Long> getUserDonationCount(@PathVariable("userId") Long userId) {
        return ResponseEntity.ok(donationService.getUserDonationCount(userId));
    }

    @GetMapping("/campaigns/total-raised")
    public ResponseEntity<java.math.BigDecimal> getTotalRaisedByCampaignIds(
            @RequestParam("campaignIds") String campaignIdsStr) {
        List<Long> ids = java.util.Arrays.stream(campaignIdsStr.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::parseLong)
                .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(donationService.getTotalRaisedByCampaignIds(ids));
    }
}
