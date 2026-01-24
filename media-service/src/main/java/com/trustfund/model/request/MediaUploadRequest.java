package com.trustfund.model.request;

import com.trustfund.model.enums.MediaType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MediaUploadRequest {
    private MultipartFile file;
    private Long postId;
    private Long campaignId;
    private MediaType mediaType;
    private String description;
}
