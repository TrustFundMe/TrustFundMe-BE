package com.trustfund.service.impl;

import com.trustfund.client.IdentityServiceClient;
import com.trustfund.model.Campaign;
import com.trustfund.model.request.CreateCampaignRequest;
import com.trustfund.model.request.UpdateCampaignRequest;
import com.trustfund.repository.CampaignRepository;
import com.trustfund.service.CampaignService;
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
    public List<Campaign> getAll() {
        return campaignRepository.findAll();
    }

    @Override
    public Campaign getById(Long id) {
        return campaignRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Campaign not found: " + id));
    }

    @Override
    public List<Campaign> getByFundOwnerId(Long fundOwnerId) {
        return campaignRepository.findByFundOwnerId(fundOwnerId);
    }

    @Override
    @Transactional
    public Campaign create(CreateCampaignRequest request) {
        identityServiceClient.validateUserExists(request.getFundOwnerId());

        Campaign campaign = Campaign.builder()
                .fundOwnerId(request.getFundOwnerId())
                .title(request.getTitle())
                .description(request.getDescription())
                .category(request.getCategory())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status(request.getStatus() != null ? request.getStatus() : "DRAFT")
                .type(request.getType())
                .thankMessage(request.getThankMessage())
                .balance(request.getBalance() != null ? request.getBalance() : java.math.BigDecimal.ZERO)
                .approvedByStaff(null)
                .approvedAt(null)
                .build();
        return campaignRepository.save(campaign);
    }

    @Override
    @Transactional
    public Campaign update(Long id, UpdateCampaignRequest request) {
        Campaign campaign = getById(id);

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
        if (request.getType() != null)
            campaign.setType(request.getType());
        if (request.getThankMessage() != null)
            campaign.setThankMessage(request.getThankMessage());
        if (request.getBalance() != null)
            campaign.setBalance(request.getBalance());
        if (request.getApprovedByStaff() != null)
            campaign.setApprovedByStaff(request.getApprovedByStaff());
        if (request.getApprovedAt() != null)
            campaign.setApprovedAt(request.getApprovedAt());

        return campaignRepository.save(campaign);
    }

    @Override
    @Transactional
    public Campaign markAsDeleted(Long id) {
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Campaign not found: " + id));
        campaign.setStatus(Campaign.STATUS_DELETED);
        return campaignRepository.save(campaign);
    }
}
