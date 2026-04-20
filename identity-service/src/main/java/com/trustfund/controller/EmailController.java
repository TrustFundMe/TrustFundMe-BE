package com.trustfund.controller;

import com.trustfund.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/emails")
@RequiredArgsConstructor
@Slf4j
public class EmailController {

    private final EmailService emailService;

    @PostMapping("/commitment-email")
    public ResponseEntity<Void> sendCommitmentEmail(@RequestBody CommitmentEmailRequest request) {
        log.info("Received commitment email request for campaign {} to {}",
                request.getCampaignId(), request.getToEmail());
        emailService.sendCommitmentRequestEmail(
                request.getToEmail(),
                request.getOwnerName(),
                request.getCampaignTitle(),
                request.getCampaignId(),
                request.getFullName(),
                request.getAddress(),
                request.getWorkplace(),
                request.getTaxId(),
                request.getIdNumber(),
                request.getIssueDate(),
                request.getIssuePlace(),
                request.getPhoneNumber(),
                request.getFrontendUrl()
        );
        return ResponseEntity.ok().build();
    }

    @PostMapping("/evidence-reminder")
    public ResponseEntity<Void> sendEvidenceReminder(@RequestBody EvidenceReminderRequest request) {
        log.info("Received evidence reminder request for email: {}", request.getToEmail());
        emailService.sendEvidenceReminder(
                request.getToEmail(),
                request.getOwnerName(),
                request.getCampaignTitle(),
                request.getExpenditurePlan(),
                request.getAmount(),
                request.getDueDate()
        );
        return ResponseEntity.ok().build();
    }

    @PostMapping("/fraud-report")
    public ResponseEntity<Void> sendFraudReport(@RequestBody FraudReportRequest request) {
        log.info("Received fraud report request for email: {}", request.getToEmail());
        emailService.sendFraudReport(
                request.getToEmail(),
                request.getOwnerName(),
                request.getCampaignTitle(),
                request.getReason(),
                request.getEvidence()
        );
        return ResponseEntity.ok().build();
    }

    @PostMapping("/commitment-success-email")
    public ResponseEntity<Void> sendCommitmentSuccessEmail(@RequestBody CommitmentSuccessRequest request) {
        log.info("Received commitment success email request for {}", request.getToEmail());
        emailService.sendCommitmentSuccessEmail(
                request.getToEmail(),
                request.getOwnerName(),
                request.getCampaignTitle()
        );
        return ResponseEntity.ok().build();
    }

    @lombok.Data
    public static class CommitmentEmailRequest {
        private String toEmail;
        private String ownerName;
        private String campaignTitle;
        private Long campaignId;
        // ── OCR/KYC fields ──
        private String fullName;     // họ tên trên CCCD
        private String address;      // địa chỉ thường trú
        private String workplace;    // nơi làm việc
        private String taxId;        // mã số thuế
        private String idNumber;     // số CCCD/CMND
        private String issueDate;    // ngày cấp
        private String issuePlace;   // nơi cấp
        private String phoneNumber;  // số điện thoại
        private String frontendUrl;  // Dynamic frontend URL
    }

    @lombok.Data
    public static class EvidenceReminderRequest {
        private String toEmail;
        private String ownerName;
        private String campaignTitle;
        private String expenditurePlan;
        private BigDecimal amount;
        private LocalDateTime dueDate;
    }

    @lombok.Data
    public static class FraudReportRequest {
        private String toEmail;
        private String ownerName;
        private String campaignTitle;
        private String reason;
        private String evidence;
    }

    @lombok.Data
    public static class CommitmentSuccessRequest {
        private String toEmail;
        private String ownerName;
        private String campaignTitle;
    }
}
