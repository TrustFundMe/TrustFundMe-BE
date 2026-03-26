package com.trustfund.mapper;

import com.trustfund.model.Module;
import com.trustfund.model.ModuleGroup;
import com.trustfund.model.dto.response.ModuleDetail;
import com.trustfund.model.dto.response.ModuleGroupDetailResponse;
import com.trustfund.model.dto.response.ModuleGroupResponse;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class ModuleGroupMapper {

    public ModuleGroupResponse toResponse(ModuleGroup entity) {
        ModuleGroupResponse res = new ModuleGroupResponse();
        res.setId(entity.getId());
        res.setName(entity.getName());
        res.setDescription(entity.getDescription());
        res.setIsActive(entity.getIsActive());
        res.setDisplayOrder(entity.getDisplayOrder());
        res.setTotalModules(entity.getModules() != null ? entity.getModules().size() : 0);
        res.setCreatedAt(entity.getCreatedAt());
        res.setUpdatedAt(entity.getUpdatedAt());
        return res;
    }

    public ModuleGroupDetailResponse toDetailResponse(ModuleGroup group) {
        ModuleGroupDetailResponse res = new ModuleGroupDetailResponse();
        res.setId(group.getId());
        res.setName(group.getName());
        res.setDescription(group.getDescription());
        res.setIsActive(group.getIsActive());
        res.setDisplayOrder(group.getDisplayOrder());
        res.setTotalModules(group.getModules() != null ? group.getModules().size() : 0);
        res.setCreatedAt(group.getCreatedAt());
        res.setUpdatedAt(group.getUpdatedAt());
        return res;
    }

    public ModuleGroupDetailResponse toResponse(ModuleGroup group, boolean includeModules) {
        ModuleGroupDetailResponse res = new ModuleGroupDetailResponse();
        res.setId(group.getId());
        res.setName(group.getName());
        res.setDescription(group.getDescription());
        res.setIsActive(group.getIsActive());
        res.setDisplayOrder(group.getDisplayOrder());
        res.setTotalModules(group.getModules() != null ? group.getModules().size() : 0);
        res.setCreatedAt(group.getCreatedAt());
        res.setUpdatedAt(group.getUpdatedAt());

        if (includeModules) {
            List<Module> sortedModules = group.getModules().stream()
                    .sorted(Comparator.comparingInt(m -> m.getDisplayOrder() != null ? m.getDisplayOrder() : 0))
                    .toList();
            res.setModules(sortedModules.stream().map(this::toModuleInGroup).toList());
        }

        return res;
    }

    private ModuleDetail toModuleInGroup(Module module) {
        return ModuleDetail.builder()
                .id(module.getId())
                .title(module.getTitle())
                .url(module.getUrl())
                .icon(module.getIcon())
                .description(module.getDescription())
                .moduleGroupId(module.getModuleGroup().getId())
                .moduleGroupName(module.getModuleGroup().getName())
                .displayOrder(module.getDisplayOrder())
                .isActive(module.getIsActive())
                .build();
    }

    private ModuleDetail toModuleDetail(Module module) {
        return ModuleDetail.builder()
                .id(module.getId())
                .title(module.getTitle())
                .url(module.getUrl())
                .icon(module.getIcon())
                .description(module.getDescription())
                .displayOrder(module.getDisplayOrder())
                .isActive(module.getIsActive())
                .createdAt(module.getCreatedAt())
                .updatedAt(module.getUpdatedAt())
                .build();
    }

    public ModuleGroupDetailResponse toActiveResponse(ModuleGroup group) {
        ModuleGroupDetailResponse res = new ModuleGroupDetailResponse();
        res.setId(group.getId());
        res.setName(group.getName());
        res.setDescription(group.getDescription());

        if (group.getModules() != null) {
            var activeModules = group.getModules().stream()
                    .filter(m -> Boolean.TRUE.equals(m.getIsActive()))
                    .sorted(Comparator.comparingInt(m -> m.getDisplayOrder() != null ? m.getDisplayOrder() : 0))
                    .map(this::toModuleDetail)
                    .toList();

            res.setModules(activeModules);
            res.setTotalModules(activeModules.size());
        } else {
            res.setTotalModules(0);
        }

        return res;
    }
}
