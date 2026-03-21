package com.trustfund.service;

import com.trustfund.model.Flag;
import com.trustfund.model.request.FlagRequest;
import com.trustfund.model.response.FlagResponse;
import com.trustfund.repository.FlagRepository;
import com.trustfund.repository.CampaignFollowRepository;
import com.trustfund.repository.CampaignRepository;
import com.trustfund.client.NotificationServiceClient;
import com.trustfund.model.Campaign;
import com.trustfund.model.CampaignFollow;
import com.trustfund.model.request.NotificationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class FlagServiceImpl implements FlagService {

    private final FlagRepository flagRepository;
    private final ApprovalTaskService approvalTaskService;
    private final CampaignFollowRepository campaignFollowRepository;
    private final CampaignRepository campaignRepository;
    private final NotificationServiceClient notificationServiceClient;

    @Override
    @Transactional
    public FlagResponse submitFlag(Long userId, FlagRequest request) {
        if (request.getPostId() == null && request.getCampaignId() == null) {
            throw new RuntimeException("Either postId or campaignId must be provided");
        }

        // ── Duplicate guard ──────────────────────────────────────────────────
        if (request.getCampaignId() != null &&
                flagRepository.existsByUserIdAndCampaignId(userId, request.getCampaignId())) {
            throw new IllegalStateException("Bạn đã tố cáo chiến dịch này rồi.");
        }
        if (request.getPostId() != null &&
                flagRepository.existsByUserIdAndPostId(userId, request.getPostId())) {
            throw new IllegalStateException("Bạn đã tố cáo bài viết này rồi.");
        }
        // ────────────────────────────────────────────────────────────────────

        Flag flag = Flag.builder()
                .userId(userId)
                .postId(request.getPostId())
                .campaignId(request.getCampaignId())
                .reason(request.getReason())
                .status("PENDING")
                .build();

        Flag savedFlag = flagRepository.save(flag);
        approvalTaskService.createAndAssignTask("FLAG", savedFlag.getId());

        // Gửi thông báo cho những người đang follow campaign này
        if (savedFlag.getCampaignId() != null) {
            sendFlagNotification(savedFlag);
        }

        return mapToResponse(savedFlag);
    }

    @Override
    public FlagResponse getFlagById(Long id) {
        Flag flag = flagRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Flag not found with id: " + id));
        return mapToResponse(flag);
    }

    @Override
    public Page<FlagResponse> getPendingFlags(Pageable pageable) {
        return flagRepository.findByStatus("PENDING", pageable)
                .map(this::mapToResponse);
    }

    @Override
    public Page<FlagResponse> getFlagsByPostId(Long postId, Pageable pageable) {
        return flagRepository.findByPostId(postId, pageable)
                .map(this::mapToResponse);
    }

    @Override
    public Page<FlagResponse> getFlagsByCampaignId(Long campaignId, Pageable pageable) {
        return flagRepository.findByCampaignId(campaignId, pageable)
                .map(this::mapToResponse);
    }

    @Override
    public Page<FlagResponse> getFlagsByUserId(Long userId, Pageable pageable) {
        return flagRepository.findByUserId(userId, pageable)
                .map(this::mapToResponse);
    }

    @Override
    public Page<FlagResponse> getAllFlags(String status, Pageable pageable) {
        if (status == null || status.equalsIgnoreCase("ALL")) {
            return flagRepository.findAll(pageable).map(this::mapToResponse);
        }
        return flagRepository.findByStatus(status, pageable).map(this::mapToResponse);
    }

    @Override
    @Transactional
    public FlagResponse reviewFlag(Long flagId, Long adminId, String status) {
        Flag flag = flagRepository.findById(flagId)
                .orElseThrow(() -> new RuntimeException("Flag not found with id: " + flagId));

        flag.setReviewedBy(adminId);
        flag.setStatus(status);

        Flag updatedFlag = flagRepository.save(flag);
        approvalTaskService.completeTask("FLAG", updatedFlag.getId());

        // Gửi thông báo cho người đã báo cáo về kết quả xử lý
        sendFlagReviewNotification(updatedFlag);

        return mapToResponse(updatedFlag);
    }

    private void sendFlagNotification(Flag flag) {
        try {
            Long campaignId = flag.getCampaignId();
            Campaign campaign = campaignRepository.findById(campaignId).orElse(null);
            if (campaign == null)
                return;

            // Lấy danh sách những người follow
            List<CampaignFollow> followers = campaignFollowRepository
                    .findById_CampaignIdOrderByFollowedAtDesc(campaignId);

            String title = "Cảnh báo: Chiến dịch bạn theo dõi bị báo cáo";
            String content = "Chiến dịch '" + campaign.getTitle()
                    + "' mà bạn đang theo dõi vừa nhận được một báo cáo vi phạm với lý do: " + flag.getReason();

            for (CampaignFollow follow : followers) {
                Long followerId = follow.getId().getUserId();

                // Không gửi cho chính người báo cáo
                if (followerId.equals(flag.getUserId()))
                    continue;

                Map<String, Object> data = new HashMap<>();
                data.put("campaignId", campaignId);
                data.put("flagId", flag.getId());
                data.put("reason", flag.getReason());

                NotificationRequest request = NotificationRequest.builder()
                        .userId(followerId)
                        .type("CAMPAIGN_FLAGGED")
                        .targetId(campaignId)
                        .targetType("CAMPAIGN")
                        .title(title)
                        .content(content)
                        .data(data)
                        .build();

                log.info("[FlagService] Sending flag notification to follower {} for campaign {}", followerId,
                        campaignId);
                notificationServiceClient.sendNotification(request);
            }
        } catch (Exception e) {
            log.error("Error sending flag notification for campaign {}: {}", flag.getCampaignId(), e.getMessage());
        }
    }

    private void sendFlagReviewNotification(Flag flag) {
        try {
            boolean isResolved = "RESOLVED".equalsIgnoreCase(flag.getStatus());
            String title = isResolved ? "Báo cáo của bạn đã được chấp nhận" : "Báo cáo của bạn đã bị từ chối";

            String targetName = "";
            if (flag.getCampaignId() != null) {
                Campaign campaign = campaignRepository.findById(flag.getCampaignId()).orElse(null);
                targetName = (campaign != null) ? "chiến dịch '" + campaign.getTitle() + "'" : "chiến dịch";
            } else if (flag.getPostId() != null) {
                targetName = "bài viết";
            }

            String content = isResolved
                    ? "Cảm ơn bạn! Báo cáo về " + targetName + " của bạn đã được quản trị viên phê duyệt và xử lý."
                    : "Báo cáo về " + targetName + " của bạn đã bị từ chối sau khi quản trị viên xem xét.";

            Map<String, Object> data = new HashMap<>();
            data.put("flagId", flag.getId());
            data.put("status", flag.getStatus());
            if (flag.getCampaignId() != null)
                data.put("campaignId", flag.getCampaignId());
            if (flag.getPostId() != null)
                data.put("postId", flag.getPostId());

            NotificationRequest request = NotificationRequest.builder()
                    .userId(flag.getUserId())
                    .type("FLAG_REVIEWED")
                    .targetId(flag.getId())
                    .targetType("FLAG")
                    .title(title)
                    .content(content)
                    .data(data)
                    .build();

            log.info("[FlagService] Sending flag review notification to user {} for flag {}", flag.getUserId(),
                    flag.getId());
            notificationServiceClient.sendNotification(request);
        } catch (Exception e) {
            log.error("Error sending flag review notification for user {}: {}", flag.getUserId(), e.getMessage());
        }
    }

    private FlagResponse mapToResponse(Flag flag) {
        return FlagResponse.builder()
                .id(flag.getId())
                .postId(flag.getPostId())
                .campaignId(flag.getCampaignId())
                .userId(flag.getUserId())
                .reviewedBy(flag.getReviewedBy())
                .reason(flag.getReason())
                .status(flag.getStatus())
                .createdAt(flag.getCreatedAt())
                .build();
    }
}
