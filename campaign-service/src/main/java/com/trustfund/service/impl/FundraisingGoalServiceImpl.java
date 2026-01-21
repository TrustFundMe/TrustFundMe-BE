package com.trustfund.service.impl;

import com.trustfund.model.FundraisingGoal;
import com.trustfund.model.request.CreateFundraisingGoalRequest;
import com.trustfund.model.request.UpdateFundraisingGoalRequest;
import com.trustfund.repository.CampaignRepository;
import com.trustfund.repository.FundraisingGoalRepository;
import com.trustfund.service.FundraisingGoalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FundraisingGoalServiceImpl implements FundraisingGoalService {

    private final FundraisingGoalRepository fundraisingGoalRepository;
    private final CampaignRepository campaignRepository;

    @Override
    public List<FundraisingGoal> getAll() {
        return fundraisingGoalRepository.findAll();
    }

    @Override
    public FundraisingGoal getById(Long id) {
        return fundraisingGoalRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Fundraising goal not found with id: " + id));
    }

    @Override
    public List<FundraisingGoal> getByCampaignId(Long campaignId) {
        return fundraisingGoalRepository.findByCampaignId(campaignId);
    }

    @Override
    @Transactional
    public FundraisingGoal create(CreateFundraisingGoalRequest request) {
        // Verify campaign exists
        campaignRepository.findById(request.getCampaignId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Campaign not found with id: " + request.getCampaignId()));

        boolean shouldBeActive = request.getIsActive() != null ? request.getIsActive() : true;

        // If new goal should be active, deactivate all other goals of this campaign
        if (shouldBeActive) {
            List<FundraisingGoal> existingGoals = fundraisingGoalRepository.findByCampaignIdAndIsActive(request.getCampaignId(), true);
            for (FundraisingGoal goal : existingGoals) {
                goal.setIsActive(false);
            }
            fundraisingGoalRepository.saveAll(existingGoals);
        }

        FundraisingGoal fundraisingGoal = FundraisingGoal.builder()
                .campaignId(request.getCampaignId())
                .targetAmount(request.getTargetAmount())
                .description(request.getDescription())
                .isActive(shouldBeActive)
                .build();

        return fundraisingGoalRepository.save(fundraisingGoal);
    }

    @Override
    @Transactional
    public FundraisingGoal update(Long id, UpdateFundraisingGoalRequest request) {
        FundraisingGoal fundraisingGoal = getById(id);

        if (request.getTargetAmount() != null) {
            fundraisingGoal.setTargetAmount(request.getTargetAmount());
        }

        if (request.getDescription() != null) {
            fundraisingGoal.setDescription(request.getDescription());
        }

        // If setting this goal to active, deactivate all other goals of this campaign
        if (request.getIsActive() != null && request.getIsActive() && !fundraisingGoal.getIsActive()) {
            List<FundraisingGoal> existingGoals = fundraisingGoalRepository.findByCampaignIdAndIsActive(fundraisingGoal.getCampaignId(), true);
            for (FundraisingGoal goal : existingGoals) {
                if (!goal.getId().equals(id)) {
                    goal.setIsActive(false);
                }
            }
            fundraisingGoalRepository.saveAll(existingGoals);
            fundraisingGoal.setIsActive(true);
        } else if (request.getIsActive() != null) {
            fundraisingGoal.setIsActive(request.getIsActive());
        }

        return fundraisingGoalRepository.save(fundraisingGoal);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        FundraisingGoal fundraisingGoal = getById(id);
        fundraisingGoalRepository.delete(fundraisingGoal);
    }
}
