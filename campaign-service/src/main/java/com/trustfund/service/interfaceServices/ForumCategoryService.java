package com.trustfund.service.interfaceServices;

import com.trustfund.model.response.ForumCategoryResponse;

import java.util.List;

public interface ForumCategoryService {

    List<ForumCategoryResponse> getAllActiveCategories();

    ForumCategoryResponse getBySlug(String slug);
}
