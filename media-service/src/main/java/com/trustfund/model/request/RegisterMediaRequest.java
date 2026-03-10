package com.trustfund.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Register a media record whose file was already uploaded externally (e.g. via
 * Supabase JS client from the frontend). No file upload involved.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterMediaRequest {
    /** Public URL of the already-uploaded file (required) */
    private String url;
    /** PHOTO | VIDEO | FILE (required) */
    private String mediaType;
    private String fileName;
    private String contentType;
    private Long sizeBytes;
    private Long postId;
    private Long campaignId;
    private Long expenditureId;
    private Long conversationId;
    private String description;
}
