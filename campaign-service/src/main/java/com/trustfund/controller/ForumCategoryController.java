package com.trustfund.controller;

import com.trustfund.model.response.ForumCategoryResponse;
import com.trustfund.service.interfaceServices.ForumCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/forum/categories")
@RequiredArgsConstructor
@Tag(name = "Forum Categories", description = "Forum category APIs")
public class ForumCategoryController {

    private final ForumCategoryService forumCategoryService;

    @GetMapping
    @Operation(summary = "Get all active categories", description = "Get list of all active forum categories with post counts")
    public ResponseEntity<List<ForumCategoryResponse>> getAllCategories() {
        List<ForumCategoryResponse> categories = forumCategoryService.getAllActiveCategories();
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Get category by slug", description = "Get a specific category by its slug")
    public ResponseEntity<ForumCategoryResponse> getCategoryBySlug(@PathVariable String slug) {
        ForumCategoryResponse category = forumCategoryService.getBySlug(slug);
        return ResponseEntity.ok(category);
    }
}
