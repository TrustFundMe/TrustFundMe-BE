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
public class FlagResponse {
    private Long id;
    private Long postId;
    private Long campaignId;
    private Long userId;
    private Long reviewedBy;
    private String reason;
    private String status;
    private LocalDateTime createdAt;

    // Reporter info
    private String reporterName;
    private String reporterEmail;
    private String reporterAvatarUrl;

    // Campaign detail (when campaignId != null)
    private CampaignDetail campaign;

    // Post detail (when postId != null)
    private PostDetail post;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CampaignDetail {
        private Long id;
        private String title;
        private String description;
        private String imageUrl;
        private String status;
        private Long raisedAmount;
        private Long authorId;
        private String authorName;
        private LocalDateTime createdAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PostDetail {
        private Long id;
        private String title;
        private String content;
        private String status;
        private Long authorId;
        private String authorName;
        private String authorAvatarUrl;
        private Integer likeCount;
        private Integer commentCount;
        private Integer viewCount;
        private Boolean isLocked;
        private LocalDateTime createdAt;
    }
}
