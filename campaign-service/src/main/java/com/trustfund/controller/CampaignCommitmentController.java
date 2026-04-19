package com.trustfund.controller;

import com.trustfund.client.EmailServiceClient;
import com.trustfund.client.IdentityServiceClient;
import com.trustfund.client.NotificationServiceClient;
import com.trustfund.model.CampaignCommitment;
import com.trustfund.model.request.NotificationRequest;
import com.trustfund.model.response.CampaignResponse;
import com.trustfund.model.response.UserKYCResponse;
import com.trustfund.repository.CampaignCommitmentRepository;
import com.trustfund.service.CampaignService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;

@RestController
@RequestMapping("/api/campaigns/commitments")
@RequiredArgsConstructor
@Slf4j
public class CampaignCommitmentController {

    private final CampaignCommitmentRepository commitmentRepository;
    private final CampaignService campaignService;
    private final IdentityServiceClient identityServiceClient;
    private final EmailServiceClient emailServiceClient;
    private final NotificationServiceClient notificationServiceClient;

    @PostMapping("/send-email/{campaignId}")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<?> sendCommitmentEmail(@PathVariable Long campaignId) {
        log.info("➔ [EMAIL_REQUEST] sendCommitmentEmail called for campaignId={}", campaignId);
        try {
            // 1. Lấy thông tin campaign
            CampaignResponse campaign = campaignService.getById(campaignId);
            if (campaign == null) {
                log.error("❌ Campaign {} not found", campaignId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Không tìm thấy chiến dịch ID: " + campaignId);
            }

            Long fundOwnerId = campaign.getFundOwnerId();
            log.info("➔ Found campaign '{}', Owner ID: {}", campaign.getTitle(), fundOwnerId);

            if (fundOwnerId == null) {
                log.error("❌ Campaign {} has no fundOwnerId", campaignId);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Chiến dịch không có thông tin chủ quỹ");
            }

            // 2. Lấy thông tin owner
            var ownerInfo = identityServiceClient.getUserById(fundOwnerId);
            if (ownerInfo == null) {
                log.error("❌ Cannot get owner info for fundOwnerId={}", fundOwnerId);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Không thể truy xuất thông tin chủ chiến dịch");
            }

            String toEmail = ownerInfo.getEmail();
            if (toEmail == null || toEmail.isBlank()) {
                log.error("❌ Owner {} has no email registered", fundOwnerId);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Chủ chiến dịch chưa đăng ký email");
            }

            String ownerName = ownerInfo.getFullName() != null ? ownerInfo.getFullName() : "Quý thành viên";

            // 3. Lấy KYC data (OCR fields)
            UserKYCResponse kycData = identityServiceClient.getUserKYC(fundOwnerId);
            if (kycData == null) {
                log.warn("⚠️ No KYC data found for user {}. Email will be sent without full details.", fundOwnerId);
            } else {
                log.info("➔ KYC found for user {}: fullName='{}', address='{}', workplace='{}', idNumber='{}...'",
                        fundOwnerId, kycData.getFullName(), kycData.getAddress(),
                        kycData.getWorkplace(),
                        kycData.getIdNumber() != null
                                ? kycData.getIdNumber().substring(0, Math.min(4, kycData.getIdNumber().length()))
                                : "N/A");
            }

            // 4. Gửi email với KYC data (OCR)
            String campaignTitle = campaign.getTitle() != null ? campaign.getTitle() : "Chiến dịch #" + campaignId;
            emailServiceClient.sendCommitmentRequestEmail(toEmail, ownerName, campaignTitle, campaignId, kycData);

            // 5. Gửi Notification trong App nhắc nhở kí cam kết
            try {
                notificationServiceClient.sendNotification(NotificationRequest.builder()
                        .userId(fundOwnerId)
                        .type("COMMITMENT_REQUIRED")
                        .targetId(campaignId)
                        .targetType("CAMPAIGN")
                        .title("Yêu cầu ký cam kết")
                        .content(
                                "Vui lòng kiểm tra email và hoàn tất ký bản cam kết trách nhiệm để được duyệt chiến dịch: "
                                        + campaignTitle)
                        .build());
            } catch (Exception e) {
                log.warn("⚠️ Failed to send in-app notification for campaign {}: {}", campaignId, e.getMessage());
            }

            log.info("✅ SUCCESS: Commitment email sent for campaign {}", campaignId);
            return ResponseEntity.ok().body("Đã gửi email thành công");
        } catch (Exception e) {
            log.error("❌ INTERNAL_ERROR during sendCommitmentEmail for campaign {}: {}", campaignId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi hệ thống khi gửi email: " + e.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<CampaignCommitment> signCommitment(
            @RequestBody CampaignCommitment commitment,
            HttpServletRequest request) {
        Optional<CampaignCommitment> existing = commitmentRepository.findByCampaignId(commitment.getCampaignId());
        if (existing.isPresent()) {
            commitment.setId(existing.get().getId());
            commitment.setCreatedAt(existing.get().getCreatedAt());
        }
        if (commitment.getIpAddress() == null || commitment.getIpAddress().isEmpty()) {
            commitment.setIpAddress(request.getRemoteAddr());
        }

        CampaignCommitment saved = commitmentRepository.save(commitment);

        // Gửi email xác nhận sau khi ký thành công (try-catch để không block main flow)
        try {
            CampaignResponse campaign = campaignService.getById(commitment.getCampaignId());
            var ownerInfo = identityServiceClient.getUserById(commitment.getUserId());
            if (campaign != null && ownerInfo != null && ownerInfo.getEmail() != null) {
                emailServiceClient.sendCommitmentSuccessEmail(
                        ownerInfo.getEmail(),
                        ownerInfo.getFullName(),
                        campaign.getTitle());
            }
        } catch (Exception e) {
            log.error("➔ Warning: Could not send confirmation email after signing: {}", e.getMessage());
        }

        return ResponseEntity.ok(saved);
    }

    @GetMapping("/{campaignId}")
    public ResponseEntity<?> getCommitment(@PathVariable Long campaignId) {
        return commitmentRepository.findByCampaignId(campaignId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/check/{campaignId}")
    public ResponseEntity<Boolean> isSigned(@PathVariable Long campaignId) {
        boolean signed = commitmentRepository.existsByCampaignIdAndStatus(campaignId, "SIGNED");
        return ResponseEntity.ok(signed);
    }
}
