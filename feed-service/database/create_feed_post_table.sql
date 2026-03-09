CREATE DATABASE IF NOT EXISTS trustfundme_feed_db;

USE trustfundme_feed_db;

CREATE TABLE IF NOT EXISTS forum_category (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    icon_url VARCHAR(500),
    color VARCHAR(20),
    display_order INT DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_forum_category_slug (slug),
    INDEX idx_forum_category_is_active (is_active)
);

CREATE TABLE IF NOT EXISTS feed_post (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    campaign_id BIGINT NULL,
    budget_id BIGINT NULL,
    author_id BIGINT NOT NULL,
    category_id BIGINT NULL,
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
    INDEX idx_feed_post_budget_id (budget_id),
    INDEX idx_feed_post_category_id (category_id),
    INDEX idx_feed_post_parent_post_id (parent_post_id),
    INDEX idx_feed_post_created_at (created_at)
);

CREATE TABLE IF NOT EXISTS forum_attachment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    post_id BIGINT NOT NULL,
    type VARCHAR(20) NOT NULL DEFAULT 'IMAGE',
    url VARCHAR(1000) NOT NULL,
    file_name VARCHAR(255) NULL,
    file_size BIGINT NULL,
    mime_type VARCHAR(100) NULL,
    display_order INT DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_forum_attachment_post_id (post_id),
    CONSTRAINT fk_forum_attachment_post
        FOREIGN KEY (post_id) REFERENCES feed_post(id) ON DELETE CASCADE
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
