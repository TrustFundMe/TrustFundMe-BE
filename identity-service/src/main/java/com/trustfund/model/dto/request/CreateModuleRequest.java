package com.trustfund.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateModuleRequest {

    @NotBlank(message = "Module name is required")
    private String title;

    @NotBlank(message = "URL is required")
    private String url;

    private String icon;

    private String description;

    private Long moduleGroupId;

    private Integer displayOrder = 0;



    private Boolean isActive = true;
}
