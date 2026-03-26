package com.trustfund.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ModuleDetail {

    private Long id;
    private String title;
    private String url;
    private String icon;
    private String description;
    private Long moduleGroupId;
    private String moduleGroupName;
    private Integer displayOrder;
    private Boolean isActive;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
