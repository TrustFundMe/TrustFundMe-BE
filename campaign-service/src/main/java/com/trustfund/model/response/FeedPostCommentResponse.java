package com.trustfund.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedPostCommentResponse {
    private Long id;
    private Long postId;
    private Long userId;
    private Long parentCommentId;
    private String content;
    private Integer likeCount;
    private Boolean isLiked;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String authorName;
    private String authorAvatar;
    private String postTitle;
    private String postType;
    private String postTargetName;
    private LocalDateTime postCreatedAt;
    private String postAuthorName;
    private String postAuthorAvatar;
    private String parentContent;
    private String parentAuthorName;
    private String parentAuthorAvatar;
    @Builder.Default
    private java.util.List<FeedPostCommentResponse> replies = new java.util.ArrayList<>();
}
