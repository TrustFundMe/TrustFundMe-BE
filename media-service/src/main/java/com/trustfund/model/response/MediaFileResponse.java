package com.trustfund.model.response;

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
    private Long ownerUserId;
    private String url;
    private String storedName;
    private String fileName;
    private String contentType;
    private Long sizeBytes;
    private LocalDateTime createdAt;
}

