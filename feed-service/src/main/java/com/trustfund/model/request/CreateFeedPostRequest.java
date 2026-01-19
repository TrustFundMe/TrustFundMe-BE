package com.trustfund.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateFeedPostRequest {

    private Long budgetId;

    @NotBlank
    @Size(max = 50)
    private String type;

    @NotBlank
    @Pattern(regexp = "PUBLIC|PRIVATE|FOLLOWERS")
    private String visibility;

    @Size(max = 255)
    private String title;

    @NotBlank
    @Size(max = 2000)
    private String content;

    @Pattern(regexp = "DRAFT|ACTIVE")
    private String status;
}
