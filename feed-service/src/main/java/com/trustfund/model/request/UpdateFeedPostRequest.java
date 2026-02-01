package com.trustfund.model.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateFeedPostRequest {

    @Size(max = 255)
    private String title;

    @Size(max = 2000)
    private String content;

    @Pattern(regexp = "DRAFT|ACTIVE")
    private String status;

    private Long budgetId;

    private List<AttachmentInput> attachments;
}
