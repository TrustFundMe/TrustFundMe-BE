package com.trustfund.service;

import com.trustfund.model.ModuleGroup;
import com.trustfund.repository.ModuleGroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ModuleService {
    private final ModuleGroupRepository moduleGroupRepository;

    public List<ModuleGroup> getActiveModuleGroups() {
        return moduleGroupRepository.findByIsActiveOrderByDisplayOrderAsc(true);
    }
}
