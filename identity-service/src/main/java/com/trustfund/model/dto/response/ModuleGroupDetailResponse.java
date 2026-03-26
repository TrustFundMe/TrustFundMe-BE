package com.trustfund.model.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ModuleGroupDetailResponse {

    private Long id;
    private String name;
    private String description;
    private Boolean isActive;
    private Integer displayOrder;
    private Integer totalModules;
    private List<ModuleDetail> modules;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
