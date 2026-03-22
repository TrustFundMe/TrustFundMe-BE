package com.trustfund.model.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeedPostResponse {

    private Long id;
    private Long targetId;
    private String targetType;
    private String targetName;
    private Long authorId;
    private String authorName;
    private String authorAvatar;
    private String type;
    private String visibility;
    private String title;
    private String content;
    private String status;

    // Resolved category slug/name for client display (joined from feed_category)
    private String category;

    private Long categoryId;
    private Long parentPostId;
    private Integer replyCount;
    private Integer viewCount;
    private Integer likeCount;
    private Integer commentCount;
    /**
     * Count of pending flags for this post (used by admin/staff feed table).
     * If not requested by service layer, this may be null.
     */
    private Integer flagCount;
    private Boolean isLiked;
    private Boolean isPinned;
    private Boolean isLocked;

    private java.util.List<ForumAttachmentResponse> attachments;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
