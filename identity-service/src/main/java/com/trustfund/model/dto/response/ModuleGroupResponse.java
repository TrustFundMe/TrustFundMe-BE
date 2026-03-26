package com.trustfund.model.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ModuleGroupResponse {

    private Long id;
    private String name;
    private String description;
    private Boolean isActive;
    private Integer displayOrder;
    private Integer totalModules;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
