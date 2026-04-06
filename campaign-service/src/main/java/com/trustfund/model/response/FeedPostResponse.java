package com.trustfund.model.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
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
    @JsonProperty("isLiked")
    private Boolean isLiked;
    @JsonProperty("isPinned")
    private Boolean isPinned;
    @JsonProperty("isLocked")
    private Boolean isLocked;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
