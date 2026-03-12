package com.trustfund.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttachmentInput {

    @NotBlank
    @Size(max = 1000)
    private String url;

    @Size(max = 20)
    private String type; // IMAGE, FILE - default IMAGE
}
