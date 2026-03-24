package com.trustfund.controller;

import com.trustfund.model.ModuleGroup;
import com.trustfund.service.ModuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/module-groups")
@RequiredArgsConstructor
public class ModuleGroupController {
    private final ModuleService moduleService;

    @GetMapping("/active")
    public List<ModuleGroup> getActiveModuleGroups() {
        return moduleService.getActiveModuleGroups();
    }
}
