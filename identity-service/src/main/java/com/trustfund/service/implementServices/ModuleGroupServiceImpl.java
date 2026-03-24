package com.trustfund.service.implementServices;

import com.trustfund.exception.exceptions.BadRequestException;
import com.trustfund.exception.exceptions.NotFoundException;
import com.trustfund.mapper.ModuleGroupMapper;
import com.trustfund.model.ModuleGroup;
import com.trustfund.model.dto.request.CreateModuleGroupRequest;
import com.trustfund.model.dto.request.UpdateModuleGroupRequest;
import com.trustfund.model.dto.response.ModuleGroupDetailResponse;
import com.trustfund.model.dto.response.ModuleGroupResponse;
import com.trustfund.repository.ModuleGroupRepository;
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
public class ModuleGroupServiceImpl implements com.trustfund.service.interfaceServices.ModuleGroupService {

    private final ModuleGroupRepository moduleGroupRepository;
    private final ModuleGroupMapper moduleGroupMapper;

    @Override
    public Page<ModuleGroupDetailResponse> searchModuleGroups(String keyword, Boolean isActive, Pageable pageable) {
        String kw = keyword == null ? null : "%" + keyword.toLowerCase() + "%";
        Page<ModuleGroup> page = moduleGroupRepository.search(kw, isActive, pageable);
        return page.map(moduleGroupMapper::toDetailResponse);
    }

    @Override
    public List<ModuleGroupDetailResponse> getAll() {
        return moduleGroupRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(g -> moduleGroupMapper.toResponse(g, false))
                .toList();
    }

    @Override
    public List<ModuleGroupDetailResponse> getAllDetails() {
        return moduleGroupRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(g -> moduleGroupMapper.toResponse(g, true))
                .toList();
    }

    @Override
    public ModuleGroupDetailResponse getDetailById(Long id) {
        ModuleGroup group = moduleGroupRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("ModuleGroup not found with id: " + id));
        return moduleGroupMapper.toResponse(group, true);
    }

    @Override
    public ModuleGroupResponse create(CreateModuleGroupRequest req) {
        String name = StringNormalizer.normalize(req.getName());

        if (moduleGroupRepository.existsByName(name)) {
            throw new BadRequestException("Module group name already exists: " + name);
        }

        long currentCount = moduleGroupRepository.count();
        int maxAllowed = (int) currentCount + 1;
        int newOrder = (req.getDisplayOrder() != null) ? req.getDisplayOrder() : maxAllowed;

        if (newOrder < 1) newOrder = 1;

        moduleGroupRepository.shiftOrdersForInsert(newOrder);

        ModuleGroup entity = new ModuleGroup();
        entity.setName(name);
        entity.setDescription(req.getDescription());
        entity.setDisplayOrder(newOrder);
        entity.setIsActive(req.getIsActive() != null ? req.getIsActive() : true);

        ModuleGroup saved = moduleGroupRepository.save(entity);
        return moduleGroupMapper.toResponse(saved);
    }

    @Override
    public ModuleGroupDetailResponse update(Long id, UpdateModuleGroupRequest req) {
        ModuleGroup group = moduleGroupRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("ModuleGroup not found with id: " + id));

        String name = StringNormalizer.normalize(req.getName());

        if (!group.getName().equals(name) && moduleGroupRepository.existsByName(name)) {
            throw new BadRequestException("Module group name already exists: " + name);
        }

        Integer oldOrder = group.getDisplayOrder();
        Integer newOrder = (req.getDisplayOrder() != null) ? req.getDisplayOrder() : oldOrder;

        long currentCount = moduleGroupRepository.count();

        if (newOrder > currentCount) {
            throw new BadRequestException("Display Order cannot exceed " + currentCount);
        }
        if (newOrder < 1) newOrder = 1;

        if (!newOrder.equals(oldOrder)) {
            if (newOrder < oldOrder) {
                moduleGroupRepository.shiftOrdersForMoveUp(newOrder, oldOrder);
            } else {
                moduleGroupRepository.shiftOrdersForMoveDown(oldOrder, newOrder);
            }
        }

        group.setName(name);
        group.setDescription(req.getDescription());
        group.setDisplayOrder(newOrder);
        group.setIsActive(req.getIsActive() != null ? req.getIsActive() : group.getIsActive());

        ModuleGroup saved = moduleGroupRepository.save(group);
        return moduleGroupMapper.toDetailResponse(saved);
    }

    @Override
    public void delete(Long id) {
        ModuleGroup group = moduleGroupRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("ModuleGroup not found with id: " + id));

        if (Boolean.FALSE.equals(group.getIsActive())) {
            throw new BadRequestException("Module group is already inactive");
        }

        if (group.getModules() != null) {
            group.getModules().forEach(module -> module.setIsActive(false));
        }
        group.setIsActive(false);

        moduleGroupRepository.save(group);
    }

    @Override
    public List<ModuleGroupDetailResponse> getActiveGroupsWithActiveModules() {
        return moduleGroupRepository.findByIsActiveTrueOrderByDisplayOrderAsc().stream()
                .map(moduleGroupMapper::toActiveResponse)
                .filter(res -> res.getTotalModules() != null && res.getTotalModules() > 0)
                .toList();
    }
}
