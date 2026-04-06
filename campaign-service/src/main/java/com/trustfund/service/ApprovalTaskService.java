package com.trustfund.service;

import com.trustfund.model.ApprovalTask;
import java.util.List;

public interface ApprovalTaskService {
    ApprovalTask createAndAssignTask(String type, Long targetId);
    ApprovalTask reassignTask(Long taskId, Long newStaffId);
    List<ApprovalTask> getAllTasks();
    List<ApprovalTask> getTasksByStaff(Long staffId);
    ApprovalTask completeTask(String type, Long targetId);
    ApprovalTask getTaskByCampaignId(Long campaignId);
    ApprovalTask getTaskByTypeAndTargetId(String type, Long targetId);
}
