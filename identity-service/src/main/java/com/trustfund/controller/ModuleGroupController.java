package com.trustfund.controller;

import com.trustfund.model.dto.request.CreateModuleGroupRequest;
import com.trustfund.model.dto.request.UpdateModuleGroupRequest;
import com.trustfund.model.dto.response.ApiResponse;
import com.trustfund.model.dto.response.ModuleGroupDetailResponse;
import com.trustfund.model.dto.response.ModuleGroupResponse;
import com.trustfund.model.dto.response.PageResponse;
import com.trustfund.service.interfaceServices.ModuleGroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/module-groups")
@RequiredArgsConstructor
public class ModuleGroupController {

    private final ModuleGroupService moduleGroupService;

    @GetMapping("/list")
    public ResponseEntity<List<ModuleGroupDetailResponse>> getAllModuleGroups() {
        return ResponseEntity.ok(moduleGroupService.getAll());
    }

    @GetMapping("/details")
    public ResponseEntity<List<ModuleGroupDetailResponse>> getAllModuleGroupDetails() {
        return ResponseEntity.ok(moduleGroupService.getAllDetails());
    }

    @GetMapping("/active")
    public ResponseEntity<List<ModuleGroupDetailResponse>> getActiveModuleGroups() {
        return ResponseEntity.ok(moduleGroupService.getActiveGroupsWithActiveModules());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ModuleGroupDetailResponse> viewModuleGroup(@PathVariable Long id) {
        return ResponseEntity.ok(moduleGroupService.getDetailById(id));
    }

    @PostMapping
    public ResponseEntity<ModuleGroupResponse> createModuleGroup(@Valid @RequestBody CreateModuleGroupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(moduleGroupService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ModuleGroupDetailResponse> updateModuleGroup(
            @PathVariable Long id,
            @Valid @RequestBody UpdateModuleGroupRequest request) {
        return ResponseEntity.ok(moduleGroupService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteModuleGroup(@PathVariable Long id) {
        moduleGroupService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(params = "page")
    public ResponseEntity<ApiResponse<PageResponse<ModuleGroupDetailResponse>>> searchModuleGroups(
            @RequestParam int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "displayOrder,asc") String[] sort,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean isActive
    ) {
        String sortField = sort[0];
        Sort.Direction direction = sort.length > 1 ? Sort.Direction.fromString(sort[1]) : Sort.Direction.ASC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortField));

        Page<ModuleGroupDetailResponse> pageResult =
                moduleGroupService.searchModuleGroups(keyword, isActive, pageable);

        return ResponseEntity.ok(
                ApiResponse.success(PageResponse.from(pageResult), "Module groups retrieved successfully")
        );
    }
}
