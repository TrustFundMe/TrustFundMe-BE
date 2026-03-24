package com.trustfund.service.interfaceServices;

import com.trustfund.model.dto.request.CreateModuleGroupRequest;
import com.trustfund.model.dto.request.UpdateModuleGroupRequest;
import com.trustfund.model.dto.response.ModuleGroupDetailResponse;
import com.trustfund.model.dto.response.ModuleGroupResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ModuleGroupService {

    ModuleGroupDetailResponse update(Long id, UpdateModuleGroupRequest request);

    ModuleGroupDetailResponse getDetailById(Long id);

    ModuleGroupResponse create(CreateModuleGroupRequest request);

    void delete(Long id);

    List<ModuleGroupDetailResponse> getAll();

    List<ModuleGroupDetailResponse> getAllDetails();

    List<ModuleGroupDetailResponse> getActiveGroupsWithActiveModules();

    Page<ModuleGroupDetailResponse> searchModuleGroups(String keyword, Boolean isActive, Pageable pageable);
}
