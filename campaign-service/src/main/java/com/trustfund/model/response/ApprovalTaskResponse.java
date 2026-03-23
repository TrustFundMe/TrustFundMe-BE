package com.trustfund.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalTaskResponse {
    private Long id;
    private String type;
    private Long targetId;
    private Long staffId;
    private String status;
}
