package com.trustfund.mapper;

import com.trustfund.model.Module;
import com.trustfund.model.ModuleGroup;
import com.trustfund.model.dto.request.CreateModuleRequest;
import com.trustfund.model.dto.response.CreateModuleResponse;
import com.trustfund.model.dto.response.ModuleDetail;
import com.trustfund.model.dto.response.UpdateModuleResponse;
import org.springframework.stereotype.Component;

@Component
public class ModuleMapper {

    public ModuleDetail toDetailResponse(Module module) {
        return ModuleDetail.builder()
                .id(module.getId())
                .title(module.getTitle())
                .url(module.getUrl())
                .icon(module.getIcon())
                .description(module.getDescription() != null ? module.getDescription() : "No description provided")
                .moduleGroupId(module.getModuleGroup().getId())
                .moduleGroupName(module.getModuleGroup().getName())
                .displayOrder(module.getDisplayOrder())
                .isActive(module.getIsActive())

                .createdAt(module.getCreatedAt())
                .updatedAt(module.getUpdatedAt())
                .build();
    }

    public Module toEntity(CreateModuleRequest req, ModuleGroup group) {
        Module module = new Module();
        module.setTitle(req.getTitle());
        module.setUrl(req.getUrl());
        module.setIcon(req.getIcon());
        module.setDescription(req.getDescription());
        module.setModuleGroup(group);
        module.setDisplayOrder(req.getDisplayOrder() != null ? req.getDisplayOrder() : 0);
        module.setIsActive(req.getIsActive() != null ? req.getIsActive() : true);

        return module;
    }

    public CreateModuleResponse toCreateResponse(Module module) {
        CreateModuleResponse res = new CreateModuleResponse();
        res.setId(module.getId());
        res.setTitle(module.getTitle());
        res.setUrl(module.getUrl());
        res.setIcon(module.getIcon());
        res.setDescription(module.getDescription());
        res.setModuleGroupId(module.getModuleGroup().getId());
        res.setModuleGroupName(module.getModuleGroup().getName());
        res.setDisplayOrder(module.getDisplayOrder());
        res.setIsActive(module.getIsActive());

        res.setCreatedAt(module.getCreatedAt());
        return res;
    }

    public UpdateModuleResponse toUpdateResponse(Module module) {
        return UpdateModuleResponse.builder()
                .id(module.getId())
                .title(module.getTitle())
                .url(module.getUrl())
                .icon(module.getIcon())
                .description(module.getDescription())
                .moduleGroupId(module.getModuleGroup().getId())
                .moduleGroupName(module.getModuleGroup().getName())
                .displayOrder(module.getDisplayOrder())
                .isActive(module.getIsActive())

                .updatedAt(module.getUpdatedAt())
                .build();
    }
}
