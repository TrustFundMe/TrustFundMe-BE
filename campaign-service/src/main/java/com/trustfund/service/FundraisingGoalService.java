package com.trustfund.service;

import com.trustfund.model.FundraisingGoal;
import com.trustfund.model.request.CreateFundraisingGoalRequest;
import com.trustfund.model.request.UpdateFundraisingGoalRequest;

import java.util.List;

public interface FundraisingGoalService {

    List<FundraisingGoal> getAll();

    FundraisingGoal getById(Long id);

    List<FundraisingGoal> getByCampaignId(Long campaignId);

    FundraisingGoal create(CreateFundraisingGoalRequest request);

    FundraisingGoal update(Long id, UpdateFundraisingGoalRequest request);

    void delete(Long id);
}
