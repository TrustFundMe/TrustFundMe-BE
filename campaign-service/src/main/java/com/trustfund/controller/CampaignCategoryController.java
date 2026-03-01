package com.trustfund.controller;

import com.trustfund.model.request.CampaignCategoryRequest;
import com.trustfund.model.response.CampaignCategoryResponse;
import com.trustfund.service.CampaignCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/campaign-categories")
@RequiredArgsConstructor
@Tag(name = "Campaign Categories", description = "API quản lý danh mục chiến dịch")
public class CampaignCategoryController {

    private final CampaignCategoryService categoryService;

    @GetMapping
    @Operation(summary = "Lấy tất cả danh mục", description = "Lấy danh sách tất cả các danh mục chiến dịch (Công khai)")
    public List<CampaignCategoryResponse> getAll() {
        return categoryService.getAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy danh mục theo ID", description = "Lấy thông tin chi tiết của một danh mục theo ID")
    public ResponseEntity<CampaignCategoryResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(categoryService.getById(id));
    }

    @PostMapping
    @Operation(summary = "Tạo mới danh mục", description = "Tạo một danh mục chiến dịch mới (Chỉ dành cho Staff và Admin)")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<CampaignCategoryResponse> create(@Valid @RequestBody CampaignCategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật danh mục", description = "Cập nhật thông tin danh mục hiện có (Chỉ dành cho Staff và Admin)")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<CampaignCategoryResponse> update(@PathVariable Long id,
            @Valid @RequestBody CampaignCategoryRequest request) {
        return ResponseEntity.ok(categoryService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa danh mục", description = "Xóa một danh mục chiến dịch (Chỉ dành cho Staff và Admin)")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
