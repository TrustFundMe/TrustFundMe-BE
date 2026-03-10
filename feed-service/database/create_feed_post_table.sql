CREATE DATABASE IF NOT EXISTS trustfundme_feed_db;

USE trustfundme_feed_db;

CREATE TABLE IF NOT EXISTS feed_post (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    campaign_id BIGINT NULL,
    expenditure_id BIGINT NULL,
    author_id BIGINT NOT NULL,
    category VARCHAR(100) NULL,
    parent_post_id BIGINT NULL,
    type NVARCHAR(50) NOT NULL,
    visibility NVARCHAR(50) NOT NULL,
    title NVARCHAR(255) NULL,
    content NVARCHAR(2000) NOT NULL,
    status NVARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    reply_count INT NOT NULL DEFAULT 0,
    view_count INT NOT NULL DEFAULT 0,
    like_count INT NOT NULL DEFAULT 0,
    comment_count INT NOT NULL DEFAULT 0,
    is_pinned BOOLEAN DEFAULT FALSE,
    is_locked BOOLEAN DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_feed_post_author_id (author_id),
    INDEX idx_feed_post_campaign_id (campaign_id),
    INDEX idx_feed_post_expenditure_id (expenditure_id),
    INDEX idx_feed_post_category (category),
    INDEX idx_feed_post_parent_post_id (parent_post_id),
    INDEX idx_feed_post_created_at (created_at)
);

CREATE TABLE IF NOT EXISTS feed_post_comment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    post_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    parent_comment_id BIGINT NULL,
    content VARCHAR(1000) NOT NULL,
    like_count INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_feed_post_comment_post_id (post_id),
    INDEX idx_feed_post_comment_user_id (user_id),
    INDEX idx_feed_post_comment_parent_id (parent_comment_id),
    CONSTRAINT fk_feed_post_comment_post
        FOREIGN KEY (post_id) REFERENCES feed_post(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS feed_post_like (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    post_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_feed_post_like (post_id, user_id),
    INDEX idx_feed_post_like_post_id (post_id),
    INDEX idx_feed_post_like_user_id (user_id),
    CONSTRAINT fk_feed_post_like_post
        FOREIGN KEY (post_id) REFERENCES feed_post(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS feed_post_comment_like (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    comment_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_feed_post_comment_like (comment_id, user_id),
    INDEX idx_feed_post_comment_like_comment_id (comment_id),
    INDEX idx_feed_post_comment_like_user_id (user_id),
    CONSTRAINT fk_feed_post_comment_like_comment
        FOREIGN KEY (comment_id) REFERENCES feed_post_comment(id) ON DELETE CASCADE
);
