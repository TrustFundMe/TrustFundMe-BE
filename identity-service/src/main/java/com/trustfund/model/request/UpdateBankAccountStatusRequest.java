package com.trustfund.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateBankAccountStatusRequest {

    @NotBlank
    @Pattern(regexp = "ACTIVE|DISABLE")
    private String status;

    private Boolean isVerified;
}
