package com.trustfund.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ForumCategoryResponse {
    private Long id;
    private String name;
    private String slug;
    private String description;
    private String iconUrl;
    private String color;
    private Integer displayOrder;
    private Long postCount;
}
