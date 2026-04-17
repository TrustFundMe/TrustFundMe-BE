-- Full reset + create for all TrustFundME databases
-- Safe to run on empty DB host or to recreate schema from scratch.
-- WARNING: This will DROP existing databases.

-- =======================================
-- 0. Drop & recreate databases
-- =======================================
DROP DATABASE IF EXISTS trustfundme_campaign_db;
DROP DATABASE IF EXISTS trustfundme_identity_db;
DROP DATABASE IF EXISTS trustfundme_media_db;
DROP DATABASE IF EXISTS trustfundme_chat_db;
DROP DATABASE IF EXISTS trustfundme_payment_db;
DROP DATABASE IF EXISTS trustfundme_notification_db;

CREATE DATABASE trustfundme_campaign_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE trustfundme_identity_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE trustfundme_media_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE trustfundme_chat_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE trustfundme_payment_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE trustfundme_notification_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- =======================================
-- 1. Create user and grant privileges
-- =======================================
CREATE USER IF NOT EXISTS 'trustfundme_user'@'%' IDENTIFIED BY 'trustfundme_password';
GRANT ALL PRIVILEGES ON trustfundme_campaign_db.* TO 'trustfundme_user'@'%';
GRANT ALL PRIVILEGES ON trustfundme_identity_db.* TO 'trustfundme_user'@'%';
GRANT ALL PRIVILEGES ON trustfundme_media_db.* TO 'trustfundme_user'@'%';
GRANT ALL PRIVILEGES ON trustfundme_chat_db.* TO 'trustfundme_user'@'%';
GRANT ALL PRIVILEGES ON trustfundme_payment_db.* TO 'trustfundme_user'@'%';
GRANT ALL PRIVILEGES ON trustfundme_notification_db.* TO 'trustfundme_user'@'%';
FLUSH PRIVILEGES;

-- =======================================
-- 2. Schema: campaign-service (DB: trustfundme_campaign_db)
-- =======================================
USE trustfundme_campaign_db;

DROP TABLE IF EXISTS internal_transactions;
DROP TABLE IF EXISTS approval_tasks;
DROP TABLE IF EXISTS flags;
DROP TABLE IF EXISTS expenditure_items;
DROP TABLE IF EXISTS expenditure_transactions;
DROP TABLE IF EXISTS expenditures;
DROP TABLE IF EXISTS fundraising_goals;
DROP TABLE IF EXISTS campaign_follows;
DROP TABLE IF EXISTS campaigns;
DROP TABLE IF EXISTS campaign_categories;
DROP TABLE IF EXISTS trust_score_config;
DROP TABLE IF EXISTS trust_score_log;
DROP TABLE IF EXISTS feed_post_revisions;
DROP TABLE IF EXISTS user_post_seen;
DROP TABLE IF EXISTS feed_post;

CREATE TABLE campaign_categories (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(500) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) AUTO_ID_CACHE = 1;

CREATE TABLE campaigns (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    fund_owner_id BIGINT NOT NULL,
    approved_by_staff BIGINT NULL,
    approved_at DATETIME NULL,
    thank_message VARCHAR(2000) NULL,
    balance DECIMAL(19, 4) NOT NULL DEFAULT 0,
    title VARCHAR(255) NOT NULL,
    cover_image BIGINT NULL,
    description VARCHAR(5000) NULL,
    category_id BIGINT NULL,
    start_date DATETIME NULL,
    end_date DATETIME NULL,
    status VARCHAR(50) NULL,
    rejection_reason VARCHAR(1000) NULL,
    type VARCHAR(50) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (category_id) REFERENCES campaign_categories(id) ON DELETE SET NULL,
    INDEX idx_campaigns_fund_owner_id (fund_owner_id),
    INDEX idx_campaigns_status (status),
    INDEX idx_campaigns_created_at (created_at)
) AUTO_ID_CACHE = 1;

CREATE TABLE fundraising_goals (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    campaign_id BIGINT NOT NULL,
    target_amount DECIMAL(19, 4) NOT NULL,
    description VARCHAR(5000) NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (campaign_id) REFERENCES campaigns(id) ON DELETE CASCADE,
    INDEX idx_fundraising_goals_campaign_id (campaign_id),
    INDEX idx_fundraising_goals_is_active (is_active)
) AUTO_ID_CACHE = 1;

CREATE TABLE campaign_follows (
    campaign_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    followed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (campaign_id, user_id),
    CONSTRAINT fk_campaign_follows_campaign
        FOREIGN KEY (campaign_id) REFERENCES campaigns(id) ON DELETE CASCADE,
    INDEX idx_campaign_follows_user_id (user_id),
    INDEX idx_campaign_follows_followed_at (followed_at)
);

CREATE TABLE `expenditures` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `campaign_id` BIGINT NOT NULL,
    `evidence_due_at` DATETIME NULL,
    `evidence_status` VARCHAR(50) NULL,
    `total_amount` DECIMAL(19, 4) NULL,
    `total_expected_amount` DECIMAL(19, 4) NULL,
    `variance` DECIMAL(19, 4) NULL,
    `is_withdrawal_requested` BOOLEAN NOT NULL DEFAULT FALSE,
    `plan` VARCHAR(2000) NULL,
    `status` VARCHAR(50) NULL,
    `staff_review_id` BIGINT NULL,
    `reject_reason` VARCHAR(1000) NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`campaign_id`) REFERENCES `campaigns`(`id`) ON DELETE CASCADE,
    INDEX `idx_expenditures_campaign_id` (`campaign_id`),
    INDEX `idx_expenditures_status` (`status`)
) AUTO_ID_CACHE = 1;

CREATE TABLE `expenditure_transactions` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `expenditure_id` BIGINT NOT NULL,
    `from_user_id` BIGINT NULL,
    `to_user_id` BIGINT NULL,
    `amount` DECIMAL(19, 4) NOT NULL,
    `from_bank_code` VARCHAR(50) NULL,
    `from_account_number` VARCHAR(50) NULL,
    `from_account_holder_name` VARCHAR(255) NULL,
    `to_bank_code` VARCHAR(50) NULL,
    `to_account_number` VARCHAR(50) NULL,
    `to_account_holder_name` VARCHAR(255) NULL,
    `type` VARCHAR(50) NOT NULL COMMENT 'PAYOUT, REFUND',
    `proof_url` VARCHAR(1000) NULL,
    `status` VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`expenditure_id`) REFERENCES `expenditures`(`id`) ON DELETE CASCADE,
    INDEX `idx_exp_trans_exp_id` (`expenditure_id`)
) AUTO_ID_CACHE = 1;

CREATE TABLE `expenditure_items` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `expenditure_id` BIGINT NOT NULL,
    `category` VARCHAR(255) NULL,
    `quantity` INT NULL,
    `actual_quantity` INT NULL,
    `quantity_left` INT NULL,
    `price` DECIMAL(19, 4) NULL,
    `expected_price` DECIMAL(19, 4) NULL,
    `note` VARCHAR(1000) NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`expenditure_id`) REFERENCES `expenditures`(`id`) ON DELETE CASCADE,
    INDEX `idx_expenditure_items_expenditure_id` (`expenditure_id`)
) AUTO_ID_CACHE = 1;

CREATE TABLE `internal_transactions` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `from_campaign_id` BIGINT NULL,
    `to_campaign_id` BIGINT NULL,
    `amount` DECIMAL(19, 4) NOT NULL,
    `type` VARCHAR(50) NOT NULL,
    `reason` TEXT NULL,
    `created_by_staff_id` BIGINT NULL,
    `status` VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    `evidence_image_id` BIGINT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`from_campaign_id`) REFERENCES `campaigns`(`id`) ON DELETE SET NULL,
    FOREIGN KEY (`to_campaign_id`) REFERENCES `campaigns`(`id`) ON DELETE SET NULL,
    INDEX `idx_int_trans_from` (`from_campaign_id`),
    INDEX `idx_int_trans_to` (`to_campaign_id`),
    INDEX `idx_int_trans_status` (`status`)
);

CREATE TABLE `approval_tasks` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `type` VARCHAR(50) NOT NULL,
    `target_id` BIGINT NOT NULL,
    `staff_id` BIGINT NULL,
    `status` VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_approval_tasks_type` (`type`),
    INDEX `idx_approval_tasks_target_id` (`target_id`),
    INDEX `idx_approval_tasks_staff_id` (`staff_id`),
    INDEX `idx_approval_tasks_status` (`status`)
) AUTO_ID_CACHE = 1;

CREATE TABLE trust_score_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    rule_key VARCHAR(100) NOT NULL UNIQUE,
    rule_name VARCHAR(255) NOT NULL,
    points INT NOT NULL DEFAULT 0,
    description TEXT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE trust_score_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    rule_key VARCHAR(100) NOT NULL,
    points_change INT NOT NULL,
    reference_id BIGINT NULL,
    reference_type VARCHAR(50) NULL,
    description TEXT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_tsl_user_id (`user_id`),
    INDEX idx_tsl_rule_key (`rule_key`),
    INDEX idx_tsl_created_at (`created_at`)
);

CREATE TABLE IF NOT EXISTS feed_post (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    target_id BIGINT NULL,
    target_type VARCHAR(50) NULL,
    target_name VARCHAR(255) NULL,
    author_id BIGINT NOT NULL,
    author_name VARCHAR(255) NULL,
    parent_post_id BIGINT NULL,
    visibility NVARCHAR(50) NOT NULL,
    title NVARCHAR(255) NULL,
    content NVARCHAR(2000) NOT NULL,
    status NVARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    reply_count INT DEFAULT 0,
    view_count INT DEFAULT 0,
    like_count INT DEFAULT 0,
    comment_count INT DEFAULT 0,
    is_pinned BOOLEAN DEFAULT FALSE,
    is_locked BOOLEAN DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_feed_post_author_id (author_id),
    INDEX idx_feed_post_target (target_id, target_type),
    INDEX idx_feed_post_parent_post_id (parent_post_id),
    INDEX idx_feed_post_created_at (created_at)
) AUTO_ID_CACHE = 1;

CREATE TABLE IF NOT EXISTS feed_post_revisions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    post_id BIGINT NOT NULL,
    revision_no INT NOT NULL,
    title NVARCHAR(255) NULL,
    content NVARCHAR(2000) NOT NULL,
    status NVARCHAR(50) NOT NULL,
    media_snapshot_json JSON NULL,
    edited_by BIGINT NOT NULL,
    edited_by_name VARCHAR(255) NULL,
    edit_note VARCHAR(500) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_fpr_post FOREIGN KEY (post_id) REFERENCES feed_post(id) ON DELETE CASCADE,
    CONSTRAINT uq_fpr_post_revision_no UNIQUE (post_id, revision_no),
    INDEX idx_fpr_post_id (post_id),
    INDEX idx_fpr_edited_by (edited_by),
    INDEX idx_fpr_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS flags (
    flag_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    post_id BIGINT NULL,
    campaign_id BIGINT NULL,
    user_id BIGINT NOT NULL,
    reviewed_by BIGINT NULL,
    reason NVARCHAR(2000) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_flags_post_id (post_id),
    INDEX idx_flags_campaign_id (campaign_id),
    INDEX idx_flags_user_id (user_id),
    INDEX idx_flags_status (status)
) AUTO_ID_CACHE = 1;

CREATE TABLE IF NOT EXISTS user_post_seen (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    post_id BIGINT NOT NULL,
    seen_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_post (user_id, post_id),
    INDEX idx_user_post_seen_user_id (user_id),
    INDEX idx_user_post_seen_post_id (post_id),
    CONSTRAINT fk_user_post_seen_post FOREIGN KEY (post_id) REFERENCES feed_post(id) ON DELETE CASCADE
) AUTO_ID_CACHE = 1;

-- =======================================
-- 3. Schema: identity-service (DB: trustfundme_identity_db)
-- =======================================
USE trustfundme_identity_db;

DROP TABLE IF EXISTS modules;
DROP TABLE IF EXISTS module_groups;
DROP TABLE IF EXISTS user_kyc;
DROP TABLE IF EXISTS otp_tokens;
DROP TABLE IF EXISTS bank_account;
DROP TABLE IF EXISTS users;

CREATE TABLE module_groups (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(1000) NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    display_order INT DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP
) AUTO_ID_CACHE = 1;

CREATE TABLE modules (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    module_group_id BIGINT NOT NULL,
    title VARCHAR(100) NOT NULL,
    url VARCHAR(500) NULL,
    icon VARCHAR(50) NULL,
    description VARCHAR(500) NULL,
    display_order INT DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (module_group_id) REFERENCES module_groups(id) ON DELETE CASCADE
) AUTO_ID_CACHE = 1;

CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    phone_number VARCHAR(255) UNIQUE,
    avatar_url VARCHAR(1000),
    role VARCHAR(50) NOT NULL DEFAULT 'USER',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    trust_score INT NOT NULL DEFAULT 0,
    ban_reason VARCHAR(1000) NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    INDEX idx_email (email)
) AUTO_ID_CACHE = 1;

CREATE TABLE bank_account (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    bank_code VARCHAR(50) NOT NULL,
    account_number VARCHAR(50) NOT NULL,
    account_holder_name VARCHAR(255) NOT NULL,
    is_verified BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(50) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_bank_account_user_id (user_id),
    CONSTRAINT fk_bank_account_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) AUTO_ID_CACHE = 1;

CREATE TABLE otp_tokens (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    email VARCHAR(255) NOT NULL,
    otp VARCHAR(6) NOT NULL,
    expires_at DATETIME NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_email (email),
    INDEX idx_otp (otp),
    INDEX idx_expires_at (expires_at)
) AUTO_ID_CACHE = 1;

CREATE TABLE user_kyc (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    id_type VARCHAR(255) NOT NULL,
    id_number VARCHAR(50) NOT NULL,
    issue_date DATE NOT NULL,
    expiry_date DATE NOT NULL,
    issue_place VARCHAR(255) NOT NULL,
    id_image_front VARCHAR(1000) NOT NULL,
    id_image_back VARCHAR(1000) NOT NULL,
    selfie_image VARCHAR(1000) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    rejection_reason VARCHAR(255) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_user_kyc_user FOREIGN KEY (user_id) REFERENCES users (id)
) AUTO_ID_CACHE = 1;

-- =======================================
-- 3.1 Schema: media-service (DB: trustfundme_media_db)
-- =======================================
USE trustfundme_media_db;

DROP TABLE IF EXISTS media;

CREATE TABLE media (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    post_id BIGINT NULL,
    campaign_id BIGINT NULL,
    conversation_id BIGINT NULL,
    expenditure_id BIGINT NULL,
    expenditure_item_id BIGINT NULL,
    media_type VARCHAR(50) NOT NULL,
    url VARCHAR(1000) NOT NULL,
    description VARCHAR(2000) NULL,
    status VARCHAR(20) DEFAULT 'PENDING',
    file_name VARCHAR(255) NULL,
    content_type VARCHAR(100) NULL,
    size_bytes BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_media_post_id (post_id),
    INDEX idx_media_campaign_id (campaign_id),
    INDEX idx_media_expenditure_id (expenditure_id),
    INDEX idx_media_expenditure_item_id (expenditure_item_id),
    INDEX idx_media_status (status)
) AUTO_ID_CACHE = 1;

-- =======================================
-- 3.4 Schema: chat-service (DB: trustfundme_chat_db)
-- =======================================
USE trustfundme_chat_db;

DROP TABLE IF EXISTS messages;
DROP TABLE IF EXISTS conversations;
DROP TABLE IF EXISTS appointment_schedules;

CREATE TABLE conversations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    staff_id BIGINT NULL,
    fund_owner_id BIGINT NOT NULL,
    campaign_id BIGINT NULL,
    last_message_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_conversations_staff_id (staff_id),
    INDEX idx_conversations_fund_owner_id (fund_owner_id),
    INDEX idx_conversations_campaign_id (campaign_id),
    INDEX idx_conversations_last_message_at (last_message_at)
) AUTO_ID_CACHE = 1;

CREATE TABLE messages (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    conversation_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    content VARCHAR(5000) NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    read_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_messages_conversation_id (conversation_id),
    INDEX idx_messages_sender_id (sender_id),
    INDEX idx_messages_created_at (created_at),
    INDEX idx_messages_is_read (is_read),
    CONSTRAINT fk_messages_conversation
        FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE
) AUTO_ID_CACHE = 1;

CREATE TABLE IF NOT EXISTS appointment_schedules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    donor_id BIGINT NOT NULL,
    staff_id BIGINT NOT NULL,
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    status VARCHAR(50) NOT NULL,
    location VARCHAR(500),
    purpose TEXT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    INDEX idx_donor_id (donor_id),
    INDEX idx_staff_id (staff_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_ID_CACHE = 1;

-- =======================================
-- 3.5 Schema: notification-service (DB: trustfundme_notification_db)
-- =======================================
USE trustfundme_notification_db;

DROP TABLE IF EXISTS notification;

CREATE TABLE notification (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    type VARCHAR(100),
    target_id BIGINT,
    target_type VARCHAR(50),
    title VARCHAR(255),
    content TEXT,
    data JSON,
    is_read TINYINT(1) DEFAULT 0,
    read_at DATETIME,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME,
    INDEX idx_notification_user_id (user_id),
    INDEX idx_notification_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AUTO_ID_CACHE = 1;

-- =======================================
-- 3.6 Schema: payment-service (DB: trustfundme_payment_db)
-- =======================================
USE trustfundme_payment_db;

DROP TABLE IF EXISTS donation_items;
DROP TABLE IF EXISTS donations;
DROP TABLE IF EXISTS payments;

CREATE TABLE IF NOT EXISTS `payments` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `description` VARCHAR(255) NULL,
    `amount` DECIMAL(19, 4) NOT NULL,
    `qr_code` VARCHAR(1000) NULL,
    `order_code` BIGINT UNIQUE NULL,
    `payment_link_id` VARCHAR(255) UNIQUE NULL,
    `status` VARCHAR(50) NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) AUTO_ID_CACHE = 1;

CREATE TABLE IF NOT EXISTS `donations` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `donor_id` BIGINT NULL,
    `campaign_id` BIGINT NULL,
    `payment_id` BIGINT NULL,
    `donation_amount` DECIMAL(19, 4) NULL,
    `tip_amount` DECIMAL(19, 4) NULL,
    `total_amount` DECIMAL(19, 4) NULL,
    `is_balance_synchronized` BOOLEAN NOT NULL DEFAULT FALSE,
    `status` VARCHAR(50) NULL,
    `is_anonymous` BOOLEAN NOT NULL DEFAULT FALSE,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`payment_id`) REFERENCES `payments`(`id`) ON DELETE CASCADE
) AUTO_ID_CACHE = 1;

CREATE TABLE IF NOT EXISTS `donation_items` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `donation_id` BIGINT NOT NULL,
    `expenditure_item_id` BIGINT NULL,
    `quantity` INT NULL,
    `amount` DECIMAL(19, 4) NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`donation_id`) REFERENCES `donations`(`id`) ON DELETE CASCADE
) AUTO_ID_CACHE = 1;


-- =======================================
-- 4. Sample data for users, bank_account, and campaign categories
-- =======================================
USE trustfundme_identity_db;

INSERT INTO module_groups (id, name, description, is_active, display_order, created_at, updated_at) VALUES
    (1, 'Tổng quan', 'Tổng quan hệ thống', TRUE, 1, NOW(), NOW()),
    (2, 'Quản lý người dùng', 'Quản lý người dùng, vai trò và quyền truy cập', TRUE, 2, NOW(), NOW()),
    (3, 'Quản lý chiến dịch', 'Quản lý chiến dịch gây quỹ', TRUE, 3, NOW(), NOW()),
    (4, 'Quản lý quỹ', 'Quản lý chi tiêu và giải ngân', TRUE, 4, NOW(), NOW()),
    (5, 'Giao dịch', 'Quản lý thanh toán và lịch sử giao dịch', TRUE, 5, NOW(), NOW()),
    (6, 'Giao tiếp', 'Chat, diễn đàn và thông báo', TRUE, 6, NOW(), NOW()),
    (7, 'Hệ thống', 'Cấu hình và quản lý hệ thống', TRUE, 7, NOW(), NOW())
ON DUPLICATE KEY UPDATE name = VALUES(name), display_order = VALUES(display_order);

INSERT INTO modules (id, module_group_id, title, url, icon, display_order, is_active, created_at, updated_at) VALUES
    -- Group 1: Tổng quan
    (1, 1, 'Dashboard', '/dashboard', 'home', 0, TRUE, NOW(), NOW()),

    -- Group 2: Quản lý người dùng
    (2, 2, 'Người dùng', '/users', 'users', 0, TRUE, NOW(), NOW()),
    (3, 2, 'Vai trò', '/roles', 'shield', 1, TRUE, NOW(), NOW()),
    (4, 2, 'Xác minh KYC', '/kyc', 'user-check', 2, TRUE, NOW(), NOW()),
    (5, 2, 'Tài khoản ngân hàng', '/bank-accounts', 'building', 3, TRUE, NOW(), NOW()),

    -- Group 3: Quản lý chiến dịch
    (6, 3, 'Chiến dịch', '/campaigns', 'folder', 0, TRUE, NOW(), NOW()),
    (7, 3, 'Danh mục', '/categories', 'tag', 1, TRUE, NOW(), NOW()),
    (8, 3, 'Mục tiêu gây quỹ', '/fundraising-goals', 'target', 2, TRUE, NOW(), NOW()),
    (9, 3, 'Chi tiêu', '/expenditures', 'credit-card', 3, TRUE, NOW(), NOW()),
    (10, 3, 'Flag / Báo cáo', '/flags', 'flag', 4, TRUE, NOW(), NOW()),
    (11, 3, 'Nhiệm vụ duyệt', '/tasks', 'clipboard-check', 5, TRUE, NOW(), NOW()),

    -- Group 4: Quản lý quỹ
    (12, 4, 'Yêu cầu giải ngân', '/payouts', 'clipboard-check', 1, TRUE, NOW(), NOW()),
    (13, 4, 'Lịch sử giải ngân', '/payout-history', 'history', 2, TRUE, NOW(), NOW()),
    (22, 4, 'Quỹ chung', '/general-fund', 'database', 0, TRUE, NOW(), NOW()),
    (23, 4, 'Tổng quan quỹ', '/funds-overview', 'chart-bar', 3, TRUE, NOW(), NOW()),

    -- Group 5: Giao dịch
    (14, 5, 'Quyên góp', '/donations', 'heart', 0, TRUE, NOW(), NOW()),
    (15, 5, 'Lịch sử thanh toán', '/payments', 'dollar-sign', 1, TRUE, NOW(), NOW()),

    -- Group 6: Giao tiếp
    (16, 6, 'Chat', '/chat', 'message-circle', 0, TRUE, NOW(), NOW()),
    (17, 6, 'Diễn đàn', '/forum', 'message-square', 1, TRUE, NOW(), NOW()),
    (18, 6, 'Bài đăng', '/feed', 'rss', 2, TRUE, NOW(), NOW()),
    (19, 6, 'Thông báo', '/notifications', 'bell', 3, TRUE, NOW(), NOW()),
    (24, 6, 'Điểm Uy Tín', '/trust-score', 'star', 4, TRUE, NOW(), NOW()),

    -- Group 7: Hệ thống
    (20, 7, 'Nhóm module', '/module-groups', 'layers', 0, TRUE, NOW(), NOW()),
    (21, 7, 'Module', '/modules', 'menu', 1, TRUE, NOW(), NOW())
ON DUPLICATE KEY UPDATE title = VALUES(title), url = VALUES(url), display_order = VALUES(display_order);

INSERT INTO users (id, email, password, full_name, phone_number, avatar_url, role, is_active, verified, trust_score, created_at, updated_at)
VALUES
    (1, 'admin@example.com',    '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'Admin User',   '0900000001', NULL, 'ADMIN', TRUE, TRUE, 0, NOW(), NOW()),
    (2, 'staff1@example.com',    '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'Staff 1',      '0900000002', 'https://ui-avatars.com/api/?name=Staff+1&background=random', 'STAFF', TRUE, TRUE, 0, NOW(), NOW()),
    (21, 'staff2@example.com',  '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'Staff 2',      '0900000021', 'https://ui-avatars.com/api/?name=Staff+2&background=random', 'STAFF', TRUE, TRUE, 0, NOW(), NOW()),
    (22, 'staff3@example.com',  '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'Staff 3',      '0900000022', 'https://ui-avatars.com/api/?name=Staff+3&background=random', 'STAFF', TRUE, TRUE, 0, NOW(), NOW()),
    (23, 'staff4@example.com',  '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'Staff 4',      '0900000023', 'https://ui-avatars.com/api/?name=Staff+4&background=random', 'STAFF', TRUE, TRUE, 0, NOW(), NOW()),
    (3, 'owner@example.com',    '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'Fund Owner',   '0900000003', 'https://ui-avatars.com/api/?name=Fund+Owner&background=random', 'FUND_OWNER', TRUE, TRUE, 0, NOW(), NOW()),
    (4, 'user@example.com',     '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'Normal User',  '0900000004', 'https://ui-avatars.com/api/?name=Normal+User&background=random', 'USER', TRUE, FALSE, 0, NOW(), NOW()),
    (5, 'alice@example.com',    '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'Alice Nguyen', '0900000005', 'https://ui-avatars.com/api/?name=Alice+Nguyen&background=random', 'USER', TRUE, TRUE, 0, NOW(), NOW()),
    (6, 'bob@example.com',      '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'Bob Tran',     '0900000006', 'https://ui-avatars.com/api/?name=Bob+Tran&background=random', 'USER', TRUE, TRUE, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE email = VALUES(email), role = VALUES(role), full_name = VALUES(full_name);

INSERT INTO bank_account (id, user_id, bank_code, account_number, account_holder_name, is_verified, status, created_at, updated_at)
VALUES
    (1, 1, 'MB', '9677888899', 'TRAN BINH DOAN TRINH', TRUE, 'ACTIVE', NOW(), NOW())
ON DUPLICATE KEY UPDATE account_number = VALUES(account_number);


USE trustfundme_campaign_db;

INSERT INTO campaign_categories (id, name, description, created_at, updated_at)
VALUES
    (1, 'Nhân đạo', 'Cứu trợ nhân đạo và hỗ trợ khẩn cấp', NOW(), NOW()),
    (2, 'Nông nghiệp', 'Phát triển nông nghiệp và hỗ trợ nông dân', NOW(), NOW()),
    (3, 'Giáo dục', 'Giáo dục, trường học và học bổng', NOW(), NOW()),
    (4, 'Y tế', 'Y tế, chữa bệnh và hỗ trợ bệnh nhi', NOW(), NOW()),
    (5, 'Môi trường', 'Bảo vệ môi trường và tái tạo rừng', NOW(), NOW()),
    (6, 'Động vật', 'Cứu hộ và chăm sóc động vật', NOW(), NOW())
ON DUPLICATE KEY UPDATE name = VALUES(name), description = VALUES(description);
