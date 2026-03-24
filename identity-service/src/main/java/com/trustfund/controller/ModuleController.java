package com.trustfund.controller;

import com.trustfund.model.dto.request.CreateModuleRequest;
import com.trustfund.model.dto.request.UpdateModuleRequest;
import com.trustfund.model.dto.response.*;
import com.trustfund.service.interfaceServices.ModuleItemService;
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
@RequestMapping("/api/modules")
@RequiredArgsConstructor
public class ModuleController {

    private final ModuleItemService moduleItemService;

    @GetMapping("/{id}")
    public ResponseEntity<ModuleDetail> viewModule(@PathVariable Long id) {
        return ResponseEntity.ok(moduleItemService.getModuleDetail(id));
    }

    @PostMapping
    public ResponseEntity<CreateModuleResponse> createModule(@Valid @RequestBody CreateModuleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(moduleItemService.createModule(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UpdateModuleResponse> updateModule(
            @PathVariable Long id,
            @Valid @RequestBody UpdateModuleRequest request) {
        return ResponseEntity.ok(moduleItemService.updateModule(id, request));
    }

    @GetMapping("/module-group/{id}")
    public ResponseEntity<List<ModuleDetail>> getModulesByGroup(@PathVariable Long id) {
        return ResponseEntity.ok(moduleItemService.getModulesByGroupId(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteModule(@PathVariable Long id) {
        moduleItemService.deleteModule(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ModuleDetail>>> searchModules(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long moduleGroupId,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "displayOrder,asc") String[] sort
    ) {
        int pageIndex = Math.max(page - 1, 0);
        String sortField = sort[0];
        Sort.Direction direction = sort.length > 1 ? Sort.Direction.fromString(sort[1]) : Sort.Direction.ASC;

        Pageable pageable = PageRequest.of(pageIndex, size, Sort.by(direction, sortField));

        Page<ModuleDetail> pageResult =
                moduleItemService.searchModules(keyword, moduleGroupId, isActive, pageable);

        return ResponseEntity.ok(
                ApiResponse.success(PageResponse.from(pageResult), "Modules retrieved successfully")
        );
    }
}
