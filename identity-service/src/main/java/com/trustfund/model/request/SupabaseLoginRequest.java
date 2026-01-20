package com.trustfund.model.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SupabaseLoginRequest {
    @NotBlank(message = "accessToken is required")
    private String accessToken;
}
