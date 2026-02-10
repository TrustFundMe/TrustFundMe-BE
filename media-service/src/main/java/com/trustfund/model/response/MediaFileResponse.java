package com.trustfund.model.response;

import com.trustfund.model.enums.MediaType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MediaFileResponse {
    private Long id;
    private Long postId;
    private Long campaignId;
    private Long conversationId;
    private MediaType mediaType;
    private String url;
    private String description;
    private String fileName;
    private String contentType;
    private Long sizeBytes;
    private LocalDateTime createdAt;
}
