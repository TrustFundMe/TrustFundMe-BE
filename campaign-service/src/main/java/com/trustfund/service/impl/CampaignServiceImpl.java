package com.trustfund.service.impl;

import com.trustfund.client.IdentityServiceClient;
import com.trustfund.model.Campaign;
import com.trustfund.model.request.CreateCampaignRequest;
import com.trustfund.model.request.UpdateCampaignRequest;
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

    private final IdentityServiceClient identityServiceClient;

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
        return campaignRepository.findByFundOwnerId(fundOwnerId).stream()
                .map(this::toCampaignResponse)
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    @Transactional
    public CampaignResponse create(CreateCampaignRequest request) {
        identityServiceClient.validateUserExists(request.getFundOwnerId());

        Campaign campaign = Campaign.builder()
                .fundOwnerId(request.getFundOwnerId())
                .title(request.getTitle())
                .description(request.getDescription())
                .category(request.getCategory())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status(request.getStatus() != null ? request.getStatus() : "DRAFT")
                .thankMessage(request.getThankMessage())
                .balance(request.getBalance() != null ? request.getBalance() : java.math.BigDecimal.ZERO)
                .approvedByStaff(null)
                .approvedAt(null)
                .build();
        return toCampaignResponse(campaignRepository.save(campaign));
    }

    @Override
    @Transactional
    public CampaignResponse update(Long id, UpdateCampaignRequest request) {
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Campaign not found: " + id));

        if (request.getTitle() != null)
            campaign.setTitle(request.getTitle());
        if (request.getDescription() != null)
            campaign.setDescription(request.getDescription());
        if (request.getCategory() != null)
            campaign.setCategory(request.getCategory());
        if (request.getStartDate() != null)
            campaign.setStartDate(request.getStartDate());
        if (request.getEndDate() != null)
            campaign.setEndDate(request.getEndDate());
        if (request.getStatus() != null)
            campaign.setStatus(request.getStatus());
        if (request.getThankMessage() != null)
            campaign.setThankMessage(request.getThankMessage());
        if (request.getBalance() != null)
            campaign.setBalance(request.getBalance());
        if (request.getApprovedByStaff() != null)
            campaign.setApprovedByStaff(request.getApprovedByStaff());
        if (request.getApprovedAt() != null)
            campaign.setApprovedAt(request.getApprovedAt());

        return toCampaignResponse(campaignRepository.save(campaign));
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
    @Transactional
    public CampaignResponse reviewCampaign(Long id, Long staffId, String status, String rejectionReason) {
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Campaign not found: " + id));

        if ("APPROVED".equalsIgnoreCase(status)) {
            UserVerificationStatusResponse verificationStatus = identityServiceClient
                    .getVerificationStatus(campaign.getFundOwnerId());
            if (verificationStatus == null || !verificationStatus.isKycVerified()
                    || !verificationStatus.isBankVerified()) {
                String missing = "";
                if (verificationStatus == null)
                    missing = "Verification data";
                else if (!verificationStatus.isKycVerified() && !verificationStatus.isBankVerified())
                    missing = "KYC and Bank Account";
                else if (!verificationStatus.isKycVerified())
                    missing = "KYC";
                else
                    missing = "Bank Account";

                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Cannot approve campaign. Owner's " + missing + " is not verified.");
            }
        }

        if ("REJECTED".equalsIgnoreCase(status) && (rejectionReason == null || rejectionReason.isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Rejection reason is required when rejecting campaign");
        }

        campaign.setStatus(status.toUpperCase());
        campaign.setRejectionReason(rejectionReason);
        campaign.setApprovedByStaff(staffId);
        campaign.setApprovedAt(java.time.LocalDateTime.now());

        return toCampaignResponse(campaignRepository.save(campaign));
    }

    private CampaignResponse toCampaignResponse(Campaign campaign) {
        UserVerificationStatusResponse verificationStatus = identityServiceClient
                .getVerificationStatus(campaign.getFundOwnerId());

        return CampaignResponse.builder()
                .id(campaign.getId())
                .fundOwnerId(campaign.getFundOwnerId())
                .title(campaign.getTitle())
                .description(campaign.getDescription())
                .category(campaign.getCategory())
                .startDate(campaign.getStartDate())
                .endDate(campaign.getEndDate())
                .status(campaign.getStatus())
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
