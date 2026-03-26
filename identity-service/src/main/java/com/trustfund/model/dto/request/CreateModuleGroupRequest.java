package com.trustfund.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateModuleGroupRequest {

    @NotBlank(message = "Module group name is required")
    private String name;

    private String description;

    private Integer displayOrder;

    private Boolean isActive;
}
