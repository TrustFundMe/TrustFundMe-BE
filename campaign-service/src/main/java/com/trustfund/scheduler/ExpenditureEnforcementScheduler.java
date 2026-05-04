package com.trustfund.scheduler;

import com.trustfund.model.Expenditure;
import com.trustfund.model.ExpenditureEvidence;
import com.trustfund.model.response.CampaignResponse;
import com.trustfund.repository.ExpenditureEvidenceRepository;
import com.trustfund.repository.ExpenditureRepository;
import com.trustfund.service.CampaignService;
import com.trustfund.service.TrustScoreService;
import com.trustfund.client.NotificationServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExpenditureEnforcementScheduler {

    private final ExpenditureEvidenceRepository evidenceRepository;
    private final ExpenditureRepository expenditureRepository;
    private final CampaignService campaignService;
    private final TrustScoreService trustScoreService;
    private final NotificationServiceClient notificationServiceClient;
    private final com.trustfund.repository.SystemConfigRepository systemConfigRepository;

    private LocalDateTime lastRunAt;

    @Scheduled(fixedDelay = 60000) // Check every minute if it's time to run
    @Transactional
    public void checkOverdueEvidence() {
        int intervalMinutes = systemConfigRepository.findByConfigKey("ENFORCEMENT_INTERVAL_MINUTES")
                .map(s -> Integer.parseInt(s.getConfigValue()))
                .orElse(60);

        if (lastRunAt != null && lastRunAt.plusMinutes(intervalMinutes).isAfter(LocalDateTime.now())) {
            return; // Not enough time passed yet
        }

        lastRunAt = LocalDateTime.now();
        log.info("➔ [ENFORCEMENT] Starting scheduled check for overdue evidence (Interval: {} min)...",
                intervalMinutes);

        List<ExpenditureEvidence> overdueEvidences = evidenceRepository.findByStatusAndDueAtBefore("PENDING",
                LocalDateTime.now());

        if (overdueEvidences.isEmpty()) {
            log.info("➔ [ENFORCEMENT] No overdue evidence requirements found.");
            return;
        }

        log.info("➔ [ENFORCEMENT] Found {} overdue evidence requirements.", overdueEvidences.size());

        for (ExpenditureEvidence evidence : overdueEvidences) {
            Long campaignId = evidence.getExpenditure().getCampaignId();

            try {
                CampaignResponse campaign = campaignService.getById(campaignId);

                if (!"DISABLED".equals(campaign.getStatus()) && !"CLOSED".equals(campaign.getStatus())) {
                    log.warn("⚠️ [ENFORCEMENT] Campaign {} (Owner: {}) has overdue evidence {}. Locking fund.",
                            campaign.getTitle(), campaign.getFundOwnerId(), evidence.getId());

                    // 1. Disable Campaign
                    campaignService.closeCampaign(campaignId, 1L); // Using System Admin ID 1 to close

                    // 2. Penalize Trust Score
                    trustScoreService.addScore(campaign.getFundOwnerId(), "OVERDUE_EVIDENCE", evidence.getId(),
                            "EXPENDITURE_EVIDENCE", "Vi phạm thời hạn nộp minh chứng chi tiêu.");

                    // 3. Mark evidence as OVERDUE
                    evidence.setStatus("OVERDUE");
                    evidenceRepository.save(evidence);

                    // 3b. Also mark the parent expenditure's evidenceStatus as OVERDUE
                    Expenditure expenditure = evidence.getExpenditure();
                    if (expenditure != null && !"OVERDUE".equals(expenditure.getEvidenceStatus())) {
                        expenditure.setEvidenceStatus("OVERDUE");
                        expenditureRepository.save(expenditure);
                        log.info("➔ [ENFORCEMENT] Updated expenditure {} evidenceStatus to OVERDUE",
                                expenditure.getId());
                    }

                    // 4. Send Notification
                    com.trustfund.model.request.NotificationRequest notiReq = com.trustfund.model.request.NotificationRequest
                            .builder()
                            .userId(campaign.getFundOwnerId())
                            .type("CAMPAIGN_LOCKED_OVERDUE")
                            .targetId(campaign.getId())
                            .targetType("CAMPAIGN")
                            .title("Quỹ đã bị đóng do vi phạm minh bạch")
                            .content(String.format(
                                    "Chiến dịch '%s' đã bị tạm đóng do bạn quá hạn nộp minh chứng cho khoản chi %sđ.",
                                    campaign.getTitle(), evidence.getAmount().abs().toString()))
                            .build();
                    notificationServiceClient.sendNotification(notiReq);
                }
            } catch (Exception e) {
                log.error("❌ [ENFORCEMENT] Failed to process enforcement for evidence {}: {}", evidence.getId(),
                        e.getMessage());
            }
        }
    }
}
