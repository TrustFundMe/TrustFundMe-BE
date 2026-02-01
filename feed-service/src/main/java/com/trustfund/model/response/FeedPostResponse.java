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
    private Long budgetId;
    private Long authorId;
    private String type;
    private String visibility;
    private String title;
    private String content;
    private String status;

    private Long categoryId;
    private Long parentPostId;
    private Integer replyCount;
    private Integer viewCount;
    private Boolean isPinned;
    private Boolean isLocked;

    private java.util.List<ForumAttachmentResponse> attachments;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
