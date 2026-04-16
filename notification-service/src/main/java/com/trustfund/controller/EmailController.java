package com.trustfund.controller;

import com.trustfund.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/emails")
@RequiredArgsConstructor
public class EmailController {

    private final EmailService emailService;

    @PostMapping("/commitment-email")
    public ResponseEntity<Void> sendCommitmentEmail(@RequestBody Map<String, Object> request) {
        String toEmail = (String) request.get("toEmail");
        String ownerName = (String) request.get("ownerName");
        String campaignTitle = (String) request.get("campaignTitle");
        Long campaignId = Long.valueOf(request.get("campaignId").toString());

        emailService.sendCommitmentRequestEmail(toEmail, ownerName, campaignTitle, campaignId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/commitment-success-email")
    public ResponseEntity<Void> sendCommitmentSuccessEmail(@RequestBody Map<String, Object> request) {
        String toEmail = (String) request.get("toEmail");
        String ownerName = (String) request.get("ownerName");
        String campaignTitle = (String) request.get("campaignTitle");

        emailService.sendCommitmentSuccessEmail(toEmail, ownerName, campaignTitle);
        return ResponseEntity.ok().build();
    }
}
