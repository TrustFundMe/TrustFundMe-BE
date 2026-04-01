package com.trustfund.service.implementServices;

import com.trustfund.exception.exceptions.BadRequestException;
import com.trustfund.exception.exceptions.NotFoundException;
import com.trustfund.mapper.ModuleMapper;
import com.trustfund.model.Module;
import com.trustfund.model.ModuleGroup;
import com.trustfund.model.dto.request.CreateModuleRequest;
import com.trustfund.model.dto.request.UpdateModuleRequest;
import com.trustfund.model.dto.response.CreateModuleResponse;
import com.trustfund.model.dto.response.ModuleDetail;
import com.trustfund.model.dto.response.UpdateModuleResponse;
import com.trustfund.repository.ModuleGroupRepository;
import com.trustfund.repository.ModuleRepository;
import com.trustfund.utils.StringNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ModuleItemServiceImpl implements com.trustfund.service.interfaceServices.ModuleItemService {

    private final ModuleRepository moduleRepository;
    private final ModuleGroupRepository moduleGroupRepository;
    private final ModuleMapper moduleMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<ModuleDetail> searchModules(String keyword, Long moduleGroupId, Boolean isActive, Pageable pageable) {
        Page<Module> page = moduleRepository.search(keyword, moduleGroupId, isActive, pageable);
        return page.map(moduleMapper::toDetailResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ModuleDetail getModuleDetail(Long id) {
        Module module = moduleRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Module not found with id: " + id));
        return moduleMapper.toDetailResponse(module);
    }

    @Override
    public CreateModuleResponse createModule(CreateModuleRequest req) {
        if (moduleRepository.existsByUrl(req.getUrl())) {
            throw new BadRequestException("The URL '" + req.getUrl() + "' already exists in the system.");
        }

        if (moduleGroupRepository.existsByNameIgnoreCase(req.getTitle().trim())) {
            throw new BadRequestException(
                    "Module name cannot be the same as an existing Module Group name: '" + req.getTitle() + "'");
        }

        ModuleGroup group = moduleGroupRepository.findById(req.getModuleGroupId())
                .orElseThrow(() -> new NotFoundException(
                        "ModuleGroup not found with id: " + req.getModuleGroupId()));

        validateModuleNameNotSameAsGroup(req.getTitle(), group.getName());

        String title = StringNormalizer.normalize(req.getTitle());
        String url;
        try {
            url = StringNormalizer.normalizeUrl(req.getUrl());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(e.getMessage());
        }

        if (moduleRepository.existsByModuleGroupIdAndTitle(req.getModuleGroupId(), title)) {
            throw new BadRequestException("Module name already exists in this module group");
        }

        if (moduleRepository.existsByModuleGroupIdAndUrl(req.getModuleGroupId(), url)) {
            throw new BadRequestException("Module URL already exists in this module group");
        }

        Module module = moduleMapper.toEntity(req, group);
        module.setTitle(title);
        module.setUrl(url);

        Module saved = moduleRepository.saveAndFlush(module);
        return moduleMapper.toCreateResponse(saved);
    }

    @Override
    public UpdateModuleResponse updateModule(Long moduleId, UpdateModuleRequest req) {
        Module module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new NotFoundException("Module not found with id: " + moduleId));

        if (!module.getUrl().equals(req.getUrl()) &&
                moduleRepository.existsByUrlAndIdNot(req.getUrl(), moduleId)) {
            throw new BadRequestException("The URL '" + req.getUrl() + "' is already used by another module.");
        }

        if (moduleGroupRepository.existsByNameIgnoreCase(req.getTitle().trim())) {
            throw new BadRequestException(
                    "Module name cannot be the same as an existing Module Group name: '" + req.getTitle() + "'");
        }

        ModuleGroup group = module.getModuleGroup();
        if (!req.getModuleGroupId().equals(group.getId())) {
            group = moduleGroupRepository.findById(req.getModuleGroupId())
                    .orElseThrow(() -> new NotFoundException("Module Group not found with id: " + req.getModuleGroupId()));
        }

        validateModuleNameNotSameAsGroup(req.getTitle(), group.getName());

        String title = StringNormalizer.normalize(req.getTitle());
        String url;
        try {
            url = StringNormalizer.normalizeUrl(req.getUrl());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(e.getMessage());
        }

        if (!module.getTitle().equals(title)
                && moduleRepository.existsByModuleGroupIdAndTitle(req.getModuleGroupId(), title)) {
            throw new BadRequestException("Module name already exists in this module group");
        }

        if (!module.getUrl().equals(url)
                && moduleRepository.existsByModuleGroupIdAndUrl(req.getModuleGroupId(), url)) {
            throw new BadRequestException("Module URL already exists in this module group");
        }

        module.setTitle(title);
        module.setUrl(url);
        module.setIcon(req.getIcon());
        module.setDescription(req.getDescription());
        module.setModuleGroup(group);
        module.setDisplayOrder(req.getDisplayOrder());
        module.setIsActive(req.getIsActive());


        moduleRepository.save(module);
        return moduleMapper.toUpdateResponse(module);
    }

    @Override
    public List<ModuleDetail> getModulesByGroupId(Long groupId) {
        if (!moduleGroupRepository.existsById(groupId)) {
            throw new NotFoundException("ModuleGroup not found with id: " + groupId);
        }
        return moduleRepository.findByModuleGroupIdOrderByDisplayOrderAsc(groupId).stream()
                .map(moduleMapper::toDetailResponse)
                .toList();
    }

    @Override
    public void deleteModule(Long id) {
        if (!moduleRepository.existsById(id)) {
            throw new NotFoundException("Module not found with id: " + id);
        }
        moduleRepository.deleteById(id);
    }

    private void validateModuleNameNotSameAsGroup(String moduleName, String groupName) {
        if (moduleName != null && moduleName.trim().equalsIgnoreCase(groupName.trim())) {
            throw new BadRequestException("Module name cannot be the same as Module Group name: " + groupName);
        }
    }
}
