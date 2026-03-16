package com.trustfund.service.impl;

import com.trustfund.client.IdentityServiceClient;
import com.trustfund.client.MediaServiceClient;
import com.trustfund.model.Campaign;
import com.trustfund.model.CampaignCategory;
import com.trustfund.model.request.CreateCampaignRequest;
import com.trustfund.model.request.UpdateCampaignRequest;
import com.trustfund.repository.CampaignCategoryRepository;
import com.trustfund.repository.CampaignRepository;
import com.trustfund.service.CampaignService;
import com.trustfund.model.response.CampaignResponse;
import com.trustfund.model.response.UserVerificationStatusResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CampaignServiceImpl implements CampaignService {

    private final CampaignRepository campaignRepository;
    private final CampaignCategoryRepository categoryRepository;
    private final IdentityServiceClient identityServiceClient;
    private final MediaServiceClient mediaServiceClient;
    private final com.trustfund.service.ApprovalTaskService approvalTaskService;

    @Override
    public List<CampaignResponse> getAll() {
        return campaignRepository.findAll().stream()
                .map(this::toCampaignResponse)
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public CampaignResponse getById(Long id) {
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Campaign not found: " + id));
        return toCampaignResponse(campaign);
    }

    @Override
    public List<CampaignResponse> getByFundOwnerId(Long fundOwnerId) {
        return campaignRepository.findByFundOwnerId(fundOwnerId, org.springframework.data.domain.Pageable.unpaged()).stream()
                .map(this::toCampaignResponse)
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public org.springframework.data.domain.Page<CampaignResponse> getByFundOwnerIdPaginated(Long fundOwnerId, org.springframework.data.domain.Pageable pageable) {
        return campaignRepository.findByFundOwnerId(fundOwnerId, pageable)
                .map(this::toCampaignResponse);
    }

    @Override
    @Transactional
    public CampaignResponse create(CreateCampaignRequest request) {
        // Validate category
        CampaignCategory category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid category id"));

        // Validate fund owner exists in identity-service
        identityServiceClient.validateUserExists(request.getFundOwnerId());

        Campaign campaign = Campaign.builder()
                .fundOwnerId(request.getFundOwnerId())
                .title(request.getTitle())
                .description(request.getDescription())
                .category(category)
                .coverImage(request.getCoverImage())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .balance(java.math.BigDecimal.ZERO)
                .status(request.getStatus() != null
                        ? request.getStatus()
                        : "PENDING_APPROVAL")
                .type(request.getType())
                .thankMessage(request.getThankMessage())
                .build();

        Campaign saved = campaignRepository.save(campaign);

        // Upgrade user role to FUND_OWNER
        identityServiceClient.upgradeUserRole(request.getFundOwnerId());

        // Create Approval Task if pending
        if ("PENDING_APPROVAL".equalsIgnoreCase(saved.getStatus())) {
            approvalTaskService.createAndAssignTask("CAMPAIGN", saved.getId());
        }

        return toCampaignResponse(saved);
    }

    @Override
    @Transactional
    public CampaignResponse update(Long id, UpdateCampaignRequest request) {
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Campaign not found"));

        if ("DISABLED".equalsIgnoreCase(campaign.getStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chiến dịch đã bị vô hiệu hóa, không thể chỉnh sửa.");
        }

        if (request.getTitle() != null)
            campaign.setTitle(request.getTitle());
        if (request.getDescription() != null)
            campaign.setDescription(request.getDescription());
        if (request.getCoverImage() != null)
            campaign.setCoverImage(request.getCoverImage());
        if (request.getCategoryId() != null) {
            CampaignCategory category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid category id"));
            campaign.setCategory(category);
        }
        if (request.getStatus() != null)
            campaign.setStatus(request.getStatus().toUpperCase());
        if (request.getStartDate() != null)
            campaign.setStartDate(request.getStartDate());
        if (request.getEndDate() != null)
            campaign.setEndDate(request.getEndDate());
        if (request.getThankMessage() != null)
            campaign.setThankMessage(request.getThankMessage());

        Campaign updated = campaignRepository.save(campaign);
        return toCampaignResponse(updated);
    }

    @Override
    @Transactional
    public CampaignResponse markAsDeleted(Long id) {
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Campaign not found: " + id));
        campaign.setStatus(Campaign.STATUS_DELETED);
        return toCampaignResponse(campaignRepository.save(campaign));
    }

    @Override
    public List<CampaignResponse> getByStatus(String status) {
        return campaignRepository.findByStatus(status).stream()
                .map(this::toCampaignResponse)
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public List<CampaignResponse> getByCategoryId(Long categoryId) {
        return campaignRepository.findByCategoryId(categoryId).stream()
                .map(this::toCampaignResponse)
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    @Transactional
    public CampaignResponse reviewCampaign(Long id, Long staffId, String status, String rejectionReason) {
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Campaign not found: " + id));

        if ("APPROVED".equalsIgnoreCase(status)) {
            UserVerificationStatusResponse verificationStatus = identityServiceClient
                    .getVerificationStatus(campaign.getFundOwnerId());
            if (verificationStatus == null || !verificationStatus.isKycVerified()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Cannot approve campaign. Owner's KYC is not verified.");
            }

            // Tự động nâng cấp role của user lên FUND_OWNER
            identityServiceClient.upgradeUserRole(campaign.getFundOwnerId());
        }

        if ("REJECTED".equalsIgnoreCase(status) && (rejectionReason == null || rejectionReason.isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Rejection reason is required when rejecting campaign");
        }

        campaign.setStatus(status.toUpperCase());
        campaign.setRejectionReason(rejectionReason);
        campaign.setApprovedByStaff(staffId);
        campaign.setApprovedAt(java.time.LocalDateTime.now());

        Campaign saved = campaignRepository.save(campaign);
        approvalTaskService.completeTask("CAMPAIGN", saved.getId());

        return toCampaignResponse(saved);
    }

    private CampaignResponse toCampaignResponse(Campaign campaign) {
        UserVerificationStatusResponse verificationStatus = identityServiceClient
                .getVerificationStatus(campaign.getFundOwnerId());

        // Resolve cover image URL
        String coverImageUrl = null;
        if (campaign.getCoverImage() != null) {
            coverImageUrl = mediaServiceClient.getMediaUrl(campaign.getCoverImage());
        }

        // Fallback: if coverImageUrl is still null, try to get the first image of the campaign
        if (coverImageUrl == null) {
            coverImageUrl = mediaServiceClient.getFirstImageByCampaignId(campaign.getId());
        }

        return CampaignResponse.builder()
                .id(campaign.getId())
                .fundOwnerId(campaign.getFundOwnerId())
                .title(campaign.getTitle())
                .coverImage(campaign.getCoverImage())
                .coverImageUrl(coverImageUrl)
                .description(campaign.getDescription())
                .categoryId(campaign.getCategory() != null ? campaign.getCategory().getId() : null)
                .categoryName(campaign.getCategory() != null ? campaign.getCategory().getName() : null)
                .startDate(campaign.getStartDate())
                .endDate(campaign.getEndDate())
                .status(campaign.getStatus())
                .type(campaign.getType())
                .balance(campaign.getBalance())
                .approvedByStaff(campaign.getApprovedByStaff())
                .approvedAt(campaign.getApprovedAt())
                .rejectionReason(campaign.getRejectionReason())
                .createdAt(campaign.getCreatedAt())
                .updatedAt(campaign.getUpdatedAt())
                .kycVerified(verificationStatus != null && verificationStatus.isKycVerified())
                .bankVerified(verificationStatus != null && verificationStatus.isBankVerified())
                .build();
    }
}
