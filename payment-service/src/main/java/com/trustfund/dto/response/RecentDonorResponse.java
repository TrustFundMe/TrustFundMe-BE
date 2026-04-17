package com.trustfund.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecentDonorResponse {
    private Long donationId;
    private Long donorId;
    private String donorName;
    private String donorAvatar;
    private BigDecimal amount;
    private Integer quantity;
    private LocalDateTime createdAt;
    private boolean anonymous;
}
