package com.trustfund.model.request;

import com.trustfund.model.enums.MediaType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterMediaRequest {
    private String url;
    private MediaType mediaType;
    private Long postId;
    private Long campaignId;
    private Long expenditureId;
    private Long conversationId;
    private String description;
    private String fileName;
    private String contentType;
    private Long sizeBytes;
}
