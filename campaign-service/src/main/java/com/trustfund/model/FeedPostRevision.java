package com.trustfund.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "feed_post_revisions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeedPostRevision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "revision_no", nullable = false)
    private Integer revisionNo;

    @Column(name = "title", length = 255)
    private String title;

    @Column(name = "content", nullable = false, length = 3000)
    private String content;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    /**
     * Snapshot of media attached to the post before this edit.
     * Stored as JSON array string:
     * [{"mediaId":1,"url":"...","mediaType":"IMAGE","sortOrder":1}]
     */
    @Column(name = "media_snapshot_json", columnDefinition = "JSON")
    private String mediaSnapshotJson;

    @Column(name = "edited_by", nullable = false)
    private Long editedBy;

    @Column(name = "edited_by_name", length = 255)
    private String editedByName;

    @Column(name = "edit_note", length = 500)
    private String editNote;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
