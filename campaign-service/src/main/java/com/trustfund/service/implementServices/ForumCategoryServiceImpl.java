package com.trustfund.service.implementServices;

import com.trustfund.model.ForumCategory;
import com.trustfund.model.response.ForumCategoryResponse;
import com.trustfund.repository.FeedPostRepository;
import com.trustfund.repository.ForumCategoryRepository;
import com.trustfund.service.interfaceServices.ForumCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ForumCategoryServiceImpl implements ForumCategoryService {

    private final ForumCategoryRepository forumCategoryRepository;
    private final FeedPostRepository feedPostRepository;

    @Override
    public List<ForumCategoryResponse> getAllActiveCategories() {
        List<ForumCategory> categories = forumCategoryRepository.findByIsActiveTrueOrderByDisplayOrderAsc();

        return categories.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ForumCategoryResponse getBySlug(String slug) {
        ForumCategory category = forumCategoryRepository.findBySlug(slug)
                .orElseThrow(() -> new RuntimeException("Category not found: " + slug));

        return toResponse(category);
    }

    private ForumCategoryResponse toResponse(ForumCategory category) {
        Long postCount = feedPostRepository.countByCategoryIdAndStatus(category.getId(), "PUBLISHED");

        return ForumCategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .description(category.getDescription())
                .iconUrl(category.getIconUrl())
                .color(category.getColor())
                .displayOrder(category.getDisplayOrder())
                .postCount(postCount != null ? postCount : 0L)
                .build();
    }
}
