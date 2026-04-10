USE trustfundme_campaign_db;

CREATE TABLE IF NOT EXISTS feed_post_revisions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    post_id BIGINT NOT NULL COMMENT 'FK -> feed_post.id',
    revision_no INT NOT NULL COMMENT 'Version number, increments per post',

    -- snapshot of post state BEFORE the edit
    -- NOTE: VARCHAR inherits utf8mb4 from table charset (supports emoji / 4-byte chars).
    -- NVARCHAR in MySQL = utf8 (3-byte only) and would fail on emoji content.
    title VARCHAR(255) NULL,
    content VARCHAR(3000) NOT NULL,
    status VARCHAR(50) NOT NULL,

    -- snapshot of media attached at the time of edit
    -- JSON array: [{"mediaId":1,"url":"...","mediaType":"IMAGE","sortOrder":1}]
    media_snapshot_json JSON NULL,

    -- who edited
    edited_by BIGINT NOT NULL COMMENT 'user_id who triggered the edit',
    edited_by_name VARCHAR(255) NULL COMMENT 'cached display name',
    edit_note VARCHAR(500) NULL COMMENT 'optional reason note',

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_fpr_post
        FOREIGN KEY (post_id) REFERENCES feed_post(id) ON DELETE CASCADE,
    CONSTRAINT uq_fpr_post_revision_no
        UNIQUE (post_id, revision_no),

    INDEX idx_fpr_post_id (post_id),
    INDEX idx_fpr_edited_by (edited_by),
    INDEX idx_fpr_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
