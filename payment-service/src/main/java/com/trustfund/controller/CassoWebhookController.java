package com.trustfund.controller;

import com.trustfund.service.CassoWebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class CassoWebhookController {

    private final CassoWebhookService cassoWebhookService;

    @GetMapping("/casso-webhook/ping")
    public ResponseEntity<?> pingWebhook() {
        return ResponseEntity.ok(Map.of("error", 0, "success", true, "message", "TrustFundMe webhook endpoint is reachable"));
    }

    @PostMapping("/casso-webhook/verify-key")
    public ResponseEntity<?> verifyWebhookKey(
            @RequestBody Map<String, String> body) {
        String accountNumber = body.get("accountNumber");
        String bankCode = body.get("bankCode");
        String webhookKey = body.get("webhookKey");
        if (accountNumber == null || webhookKey == null) {
            return ResponseEntity.badRequest().body(Map.of("valid", false, "message", "Thiếu accountNumber hoặc webhookKey"));
        }
        boolean valid = cassoWebhookService.verifyWebhookKeyPublic(accountNumber, bankCode, webhookKey);
        if (valid) {
            return ResponseEntity.ok(Map.of("valid", true, "message", "Mã kết nối Casso hợp lệ"));
        } else {
            return ResponseEntity.ok(Map.of("valid", false, "message", "Mã kết nối Casso không đúng hoặc tài khoản chưa đăng ký"));
        }
    }

    @PostMapping("/casso-webhook")
    public ResponseEntity<?> handleCassoWebhook(
            @RequestBody Map<String, Object> payload,
            @RequestHeader(value = "Secure-Token", required = false) String secureTokenHeader,
            @RequestHeader(value = "X-Casso-Signature", required = false) String cassoSignature,
            @RequestParam(value = "secure-token", required = false) String secureTokenParam) {
        
        log.info("Received Casso Webhook. HeaderToken: {}, ParamToken: {}, Signature: {}", 
                secureTokenHeader, secureTokenParam, cassoSignature);
        
        try {
            String verifyToken = (secureTokenHeader != null) ? secureTokenHeader : 
                               (cassoSignature != null ? cassoSignature : secureTokenParam);
            cassoWebhookService.handleCassoWebhook(payload, verifyToken);
            
            // Casso requires 200 OK and {"error": 0, "success": true} for Strict Mode
            return ResponseEntity.ok(Map.of("error", 0, "success", true));
        } catch (Exception e) {
            log.error("Error handling Casso webhook", e);
            return ResponseEntity.internalServerError().body(Map.of("error", 1, "message", e.getMessage()));
        }
    }
}
