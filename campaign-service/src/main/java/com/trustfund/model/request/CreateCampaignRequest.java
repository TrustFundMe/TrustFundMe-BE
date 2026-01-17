package com.trustfund.model.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateCampaignRequest {

    @NotNull(message = "fundOwnerId không được để trống")
    private Long fundOwnerId;

    @NotNull(message = "title không được để trống")
    @Size(max = 255)
    private String title;

    @Size(max = 5000)
    private String description;

    @Size(max = 500)
    private String coverImage;

    private LocalDateTime startDate;
    private LocalDateTime endDate;

    @Size(max = 50)
    private String status;

    @Size(max = 2000)
    private String thankMessage;

    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;
}
