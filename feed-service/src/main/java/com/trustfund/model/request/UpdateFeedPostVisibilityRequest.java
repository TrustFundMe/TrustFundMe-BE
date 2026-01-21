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
public class UpdateFeedPostVisibilityRequest {

    @NotBlank
    @Pattern(regexp = "PUBLIC|PRIVATE|FOLLOWERS")
    private String visibility;
}
