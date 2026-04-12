package com.trustfund.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserTrustScoreResponse {

    private Long userId;
    private String userFullName;
    private String userAvatarUrl;
    private Integer totalScore;
}
