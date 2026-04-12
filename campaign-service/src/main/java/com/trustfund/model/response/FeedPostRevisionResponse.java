package com.trustfund.model.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeedPostRevisionResponse {

    private Long id;
    private Long postId;
    private Integer revisionNo;

    /** Snapshot: post title before edit */
    private String title;

    /** Snapshot: post content before edit */
    private String content;

    /** Snapshot: post status before edit */
    private String status;

    /**
     * Snapshot: media list before edit.
     * Deserialized from media_snapshot_json for convenience; each entry has keys:
     * mediaId, url, mediaType, sortOrder
     */
    private List<Map<String, Object>> mediaSnapshot;

    private Long editedBy;
    private String editedByName;
    private String editNote;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
