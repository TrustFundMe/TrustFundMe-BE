package com.trustfund.model.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateFeedPostContentRequest {

    @Size(max = 255)
    private String title;

    @Size(max = 2000)
    private String content;
}
