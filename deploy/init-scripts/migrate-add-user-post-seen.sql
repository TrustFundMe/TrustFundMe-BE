-- Migration: Add user_post_seen table to existing trustfundme_campaign_db
-- Safe to run even if table already exists (uses CREATE TABLE IF NOT EXISTS)

USE trustfundme_campaign_db;

CREATE TABLE IF NOT EXISTS user_post_seen (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    post_id BIGINT NOT NULL,
    seen_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_post (user_id, post_id),
    INDEX idx_user_post_seen_user_id (user_id),
    INDEX idx_user_post_seen_post_id (post_id),
    CONSTRAINT fk_user_post_seen_post FOREIGN KEY (post_id) REFERENCES feed_post(id) ON DELETE CASCADE
);
