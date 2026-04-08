package com.trustfund.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalTaskResponse {
    private Long id;
    private String type;
    private Long targetId;
    private Long staffId;
    private String staffName;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
