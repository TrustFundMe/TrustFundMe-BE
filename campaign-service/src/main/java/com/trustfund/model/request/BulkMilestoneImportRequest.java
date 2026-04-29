package com.trustfund.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkMilestoneImportRequest {
    private String milestoneTitle;
    private String description;
    private String startDate;
    private String endDate;
    private String releaseCondition;
    private List<CreateExpenditureCatologyRequest> categories;
}
