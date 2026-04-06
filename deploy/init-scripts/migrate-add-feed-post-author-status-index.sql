USE trustfund_campaign;

ALTER TABLE feed_post
    ADD INDEX idx_feed_post_author_status (author_id, status);
