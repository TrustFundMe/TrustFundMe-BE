package com.trustfund.service.interfaceServices;

import com.trustfund.model.dto.request.CreateModuleRequest;
import com.trustfund.model.dto.request.UpdateModuleRequest;
import com.trustfund.model.dto.response.CreateModuleResponse;
import com.trustfund.model.dto.response.ModuleDetail;
import com.trustfund.model.dto.response.UpdateModuleResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ModuleItemService {

    ModuleDetail getModuleDetail(Long id);

    List<ModuleDetail> getModulesByGroupId(Long groupId);

    CreateModuleResponse createModule(CreateModuleRequest request);

    UpdateModuleResponse updateModule(Long id, UpdateModuleRequest req);

    void deleteModule(Long id);

    Page<ModuleDetail> searchModules(String keyword, Long moduleGroupId, Boolean isActive, Pageable pageable);
}
