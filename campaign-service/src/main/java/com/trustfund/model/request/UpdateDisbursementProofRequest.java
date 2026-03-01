package com.trustfund.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request body to update disbursement proof")
public class UpdateDisbursementProofRequest {
    
    @NotBlank(message = "Proof URL is required")
    @Schema(description = "URL of the disbursement proof (screenshot)", example = "https://cloudinary.com/...)")
    private String proofUrl;
}
