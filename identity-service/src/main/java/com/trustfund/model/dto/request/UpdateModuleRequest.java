package com.trustfund.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateModuleRequest {

    @NotBlank(message = "Module name is required")
    private String title;

    @NotBlank(message = "URL is required")
    private String url;

    private String icon;

    private String description;

    @NotNull(message = "Module group is required")
    private Long moduleGroupId;

    private Integer displayOrder;

    private Boolean isActive;


}
