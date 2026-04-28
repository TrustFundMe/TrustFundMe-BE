package com.trustfund.model.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateExpenditureCatologyRequest {

    @NotBlank(message = "Tên danh mục không được để trống")
    private String name;

    private String description;

    private String withdrawalCondition;

    private List<CreateExpenditureItemRequest> items;
}
