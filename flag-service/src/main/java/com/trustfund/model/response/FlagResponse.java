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
public class FlagResponse {
    private Long id;
    private Long postId;
    private Long campaignId;
    private Long userId;
    private Long reviewedBy;
    private String reason;
    private String status;
    private LocalDateTime createdAt;
}
