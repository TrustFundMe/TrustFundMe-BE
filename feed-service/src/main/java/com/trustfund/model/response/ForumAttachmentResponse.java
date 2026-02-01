package com.trustfund.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ForumAttachmentResponse {
    private Long id;
    private String type;
    private String url;
    private String fileName;
    private Long fileSize;
    private String mimeType;
    private Integer displayOrder;
}
