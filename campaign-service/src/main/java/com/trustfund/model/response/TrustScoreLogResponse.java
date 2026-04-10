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
public class TrustScoreLogResponse {

    private Long id;
    private Long userId;
    private String userFullName;
    private String userAvatarUrl;
    private String ruleKey;
    private String ruleName;
    private Integer pointsChange;
    private Long referenceId;
    private String referenceType;
    private String description;
    private LocalDateTime createdAt;
}
