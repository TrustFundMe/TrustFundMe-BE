package com.trustfund.model.request;

import com.trustfund.model.enums.KYCStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateKYCStatusRequest {
    @NotNull(message = "Status is required")
    private KYCStatus status;

    private String rejectionReason;
}
