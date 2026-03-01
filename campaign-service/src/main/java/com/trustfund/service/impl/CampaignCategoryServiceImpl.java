package com.trustfund.service.impl;

import com.trustfund.exception.ResourceNotFoundException;
import com.trustfund.model.CampaignCategory;
import com.trustfund.model.request.CampaignCategoryRequest;
import com.trustfund.model.response.CampaignCategoryResponse;
import com.trustfund.repository.CampaignCategoryRepository;
import com.trustfund.service.CampaignCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CampaignCategoryServiceImpl implements CampaignCategoryService {

    private final CampaignCategoryRepository categoryRepository;

    @Override
    public List<CampaignCategoryResponse> getAll() {
        return categoryRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public CampaignCategoryResponse getById(Long id) {
        CampaignCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục với ID: " + id));
        return mapToResponse(category);
    }

    @Override
    @Transactional
    public CampaignCategoryResponse create(CampaignCategoryRequest request) {
        if (categoryRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("Tên danh mục đã tồn tại");
        }

        CampaignCategory category = CampaignCategory.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();

        return mapToResponse(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public CampaignCategoryResponse update(Long id, CampaignCategoryRequest request) {
        CampaignCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục với ID: " + id));

        if (!category.getName().equals(request.getName()) && categoryRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("Tên danh mục đã tồn tại");
        }

        category.setName(request.getName());
        category.setDescription(request.getDescription());

        return mapToResponse(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new ResourceNotFoundException("Không tìm thấy danh mục với ID: " + id);
        }
        categoryRepository.deleteById(id);
    }

    private CampaignCategoryResponse mapToResponse(CampaignCategory category) {
        return CampaignCategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }
}
