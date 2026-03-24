-- Full reset + create for all TrustFundME databases
-- Safe to run on empty DB host or to recreate schema from scratch.
-- WARNING: This will DROP existing databases (campaign + identity).

-- =======================================
-- 0. Drop & recreate databases
-- =======================================
DROP DATABASE IF EXISTS trustfundme_campaign_db;
DROP DATABASE IF EXISTS trustfundme_identity_db;
DROP DATABASE IF EXISTS trustfundme_media_db;
DROP DATABASE IF EXISTS trustfundme_moderation_db;
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

DROP TABLE IF EXISTS approval_tasks;
DROP TABLE IF EXISTS flag_reports;
DROP TABLE IF EXISTS flags;
DROP TABLE IF EXISTS expenditure_items;
DROP TABLE IF EXISTS expenditure_transactions;
DROP TABLE IF EXISTS expenditures;
DROP TABLE IF EXISTS fundraising_goals;
DROP TABLE IF EXISTS campaign_follows;
DROP TABLE IF EXISTS campaigns;
DROP TABLE IF EXISTS campaign_categories;

-- campaign_categories
CREATE TABLE campaign_categories (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(500) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- campaigns
CREATE TABLE campaigns (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    fund_owner_id BIGINT NOT NULL,
    approved_by_staff BIGINT NULL, -- id staff duyệt
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
);

-- fundraising_goals
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
);

-- campaign_follows
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

-- expenditures (1 expenditure mẫu cho mỗi campaign)
CREATE TABLE `expenditures` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `campaign_id` BIGINT NOT NULL UNIQUE,
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
);

-- expenditure_transactions
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
    `status` VARCHAR(50) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING, COMPLETED, REJECTED',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`expenditure_id`) REFERENCES `expenditures`(`id`) ON DELETE CASCADE,
    INDEX `idx_exp_trans_exp_id` (`expenditure_id`)
);

-- expenditure_items
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
);

-- approval_tasks
CREATE TABLE `approval_tasks` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `type` VARCHAR(50) NOT NULL COMMENT 'CAMPAIGN, EXPENDITURE, FLAG',
    `target_id` BIGINT NOT NULL,
    `staff_id` BIGINT NULL,
    `status` VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_approval_tasks_type` (`type`),
    INDEX `idx_approval_tasks_target_id` (`target_id`),
    INDEX `idx_approval_tasks_staff_id` (`staff_id`),
    INDEX `idx_approval_tasks_status` (`status`)
);

-- =======================================
-- 3. Schema: identity-service (DB: trustfundme_identity_db)
-- =======================================
USE trustfundme_identity_db;

-- =======================================
-- 3a. Module Groups & Modules (sidebar navigation)
-- =======================================
DROP TABLE IF EXISTS modules;
DROP TABLE IF EXISTS module_groups;

-- module_groups
CREATE TABLE module_groups (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(1000) NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    display_order INT DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP
);

-- modules
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
);

-- =======================================
-- 3b. Module & Module Group sample data
-- =======================================
-- Insert module groups first
INSERT INTO module_groups (id, name, description, is_active, display_order, created_at, updated_at) VALUES
    (1, 'Tổng quan', 'Tổng quan hệ thống', TRUE, 1, NOW(), NOW()),
    (2, 'Quản lý người dùng', 'Quản lý người dùng, vai trò và quyền truy cập', TRUE, 2, NOW(), NOW()),
    (3, 'Quản lý chiến dịch', 'Quản lý chiến dịch gây quỹ', TRUE, 3, NOW(), NOW()),
    (4, 'Quản lý quỹ', 'Quản lý chi tiêu và giải ngân', TRUE, 4, NOW(), NOW()),
    (5, 'Giao dịch', 'Quản lý thanh toán và lịch sử giao dịch', TRUE, 5, NOW(), NOW()),
    (6, 'Giao tiếp', 'Chat, diễn đàn và thông báo', TRUE, 6, NOW(), NOW()),
    (7, 'Hệ thống', 'Cấu hình và quản lý hệ thống', TRUE, 7, NOW(), NOW())
ON DUPLICATE KEY UPDATE name = VALUES(name), display_order = VALUES(display_order);

-- Insert modules
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
    (12, 4, 'Yêu cầu giải ngân', '/payouts', 'clipboard-check', 0, TRUE, NOW(), NOW()),
    (13, 4, 'Lịch sử giải ngân', '/payout-history', 'history', 1, TRUE, NOW(), NOW()),

    -- Group 5: Giao dịch
    (14, 5, 'Quyên góp', '/donations', 'heart', 0, TRUE, NOW(), NOW()),
    (15, 5, 'Lịch sử thanh toán', '/payments', 'dollar-sign', 1, TRUE, NOW(), NOW()),

    -- Group 6: Giao tiếp
    (16, 6, 'Chat', '/chat', 'message-circle', 0, TRUE, NOW(), NOW()),
    (17, 6, 'Diễn đàn', '/forum', 'message-square', 1, TRUE, NOW(), NOW()),
    (18, 6, 'Bài đăng', '/feed', 'rss', 2, TRUE, NOW(), NOW()),
    (19, 6, 'Thông báo', '/notifications', 'bell', 3, TRUE, NOW(), NOW()),

    -- Group 7: Hệ thống
    (20, 7, 'Nhóm module', '/module-groups', 'layers', 0, TRUE, NOW(), NOW()),
    (21, 7, 'Module', '/modules', 'menu', 1, TRUE, NOW(), NOW())
ON DUPLICATE KEY UPDATE title = VALUES(title), url = VALUES(url), display_order = VALUES(display_order);

DROP TABLE IF EXISTS user_kyc;
DROP TABLE IF EXISTS otp_tokens;
DROP TABLE IF EXISTS bank_account;
DROP TABLE IF EXISTS users;

-- users
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
    ban_reason VARCHAR(1000) NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    INDEX idx_email (email)
);

-- bank_account
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
);

-- otp_tokens
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
);

-- user_kyc
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
);

-- =======================================
-- 3.1 Schema: media-service (DB: trustfundme_media_db)
-- =======================================
-- Media service không lưu metadata vào DB, chỉ upload lên Supabase và trả về URL
-- DB này giữ lại để sau này có thể dùng cho các tính năng khác
USE trustfundme_media_db;

DROP TABLE IF EXISTS media;

CREATE TABLE media (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    post_id BIGINT NULL,
    campaign_id BIGINT NULL,
    conversation_id BIGINT NULL,
    expenditure_id BIGINT NULL,
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
    INDEX idx_media_status (status)
);

-- =======================================
-- 3.2 Schema: feed-service (Now merged into DB: trustfundme_campaign_db)
-- =======================================
USE trustfundme_campaign_db;

DROP TABLE IF EXISTS feed_post_comment_like;
DROP TABLE IF EXISTS feed_post_like;
DROP TABLE IF EXISTS feed_post_comment;
DROP TABLE IF EXISTS forum_attachment;
DROP TABLE IF EXISTS feed_post;
DROP TABLE IF EXISTS feed_category;

CREATE TABLE forum_category (
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
    target_id BIGINT NULL COMMENT 'ID of linked entity (campaign or expenditure)',
    target_type VARCHAR(50) NULL COMMENT 'EXPENDITURE or CAMPAIGN',
    target_name VARCHAR(255) NULL COMMENT 'Cached name of linked entity',
    author_id BIGINT NOT NULL,
    author_name VARCHAR(255) NULL COMMENT 'Cached author full name for display',
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

-- =======================================
-- 3.3 Schema: flag-service (Now merged into DB: trustfundme_campaign_db)
-- =======================================
USE trustfundme_campaign_db;

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
);

-- user_post_seen: tracks which posts a user has seen
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

-- =======================================
-- 3.4 Schema: chat-service (DB: trustfundme_chat_db)
-- =======================================
USE trustfundme_chat_db;

DROP TABLE IF EXISTS messages;
DROP TABLE IF EXISTS conversations;

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
);

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
);
-- Create table for Appointment Schedules
CREATE TABLE IF NOT EXISTS appointment_schedules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    donor_id BIGINT NOT NULL,
    staff_id BIGINT NOT NULL,
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    status VARCHAR(50) NOT NULL COMMENT 'PENDING, CONFIRMED, CANCELLED, COMPLETED',
    location VARCHAR(500),
    purpose TEXT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    INDEX idx_donor_id (donor_id),
    INDEX idx_staff_id (staff_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Sample Chat Data
USE trustfundme_chat_db;
INSERT INTO conversations (id, staff_id, fund_owner_id, campaign_id, last_message_at)
VALUES
    (1, 2, 3, 1, NOW()),
    (2, 2, 5, NULL, NOW())
ON DUPLICATE KEY UPDATE last_message_at = VALUES(last_message_at);

INSERT INTO messages (id, conversation_id, sender_id, content, is_read, created_at)
VALUES
    (1, 1, 3, 'Chào staff, tôi muốn hỏi về việc rút tiền đợt 1 cho chiến dịch cứu trợ miền Trung.', TRUE, DATE_SUB(NOW(), INTERVAL 1 HOUR)),
    (2, 1, 2, 'Chào bạn, chúng tôi đã nhận được yêu cầu. Đang tiến hành kiểm tra chứng từ.', TRUE, DATE_SUB(NOW(), INTERVAL 50 MINUTE)),
    (3, 1, 3, 'Cảm ơn bạn, tôi đã đính kèm thêm hóa đơn VAT rồi nhé.', FALSE, DATE_SUB(NOW(), INTERVAL 30 MINUTE)),
    (4, 2, 5, 'Chào admin, làm sao để tôi có thể trở thành tình nguyện viên?', TRUE, DATE_SUB(NOW(), INTERVAL 2 DAY)),
    (5, 2, 2, 'Bạn có thể nhấn vào nút "Become Volunteer" ở trang chủ nhé!', TRUE, DATE_SUB(NOW(), INTERVAL 1 DAY))
ON DUPLICATE KEY UPDATE content = VALUES(content);

INSERT INTO appointment_schedules (id, donor_id, staff_id, start_time, end_time, status, location, purpose, created_at, updated_at)
VALUES
    (1, 5, 2, '2024-03-10 09:00:00', '2024-03-10 10:00:00', 'PENDING', 'Trung tâm cứu trợ ABC', 'Trao đổi về việc ủng hộ nhu yếu phẩm', NOW(), NOW()),
    (2, 6, 2, '2024-03-11 14:00:00', '2024-03-11 15:00:00', 'CONFIRMED', 'Văn phòng TrustFundMe', 'Ký hợp đồng ủng hộ dài hạn', NOW(), NOW())
ON DUPLICATE KEY UPDATE status = VALUES(status);

-- =======================================
-- 4. Sample data
-- =======================================
-- Users (passwords are plain text placeholders; replace in real env)
USE trustfundme_identity_db;
INSERT INTO users (id, email, password, full_name, phone_number, avatar_url, role, is_active, verified, created_at, updated_at)
VALUES
    (1, 'admin@example.com',    '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'Admin User',   '0900000001', NULL, 'ADMIN', TRUE, TRUE, NOW(), NOW()),
    (2, 'staff1@example.com',    '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'Staff 1',      '0900000002', 'https://ui-avatars.com/api/?name=Staff+1&background=random', 'STAFF', TRUE, TRUE, NOW(), NOW()),
    (21, 'staff2@example.com',  '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'Staff 2',      '0900000021', 'https://ui-avatars.com/api/?name=Staff+2&background=random', 'STAFF', TRUE, TRUE, NOW(), NOW()),
    (22, 'staff3@example.com',  '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'Staff 3',      '0900000022', 'https://ui-avatars.com/api/?name=Staff+3&background=random', 'STAFF', TRUE, TRUE, NOW(), NOW()),
    (23, 'staff4@example.com',  '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'Staff 4',      '0900000023', 'https://ui-avatars.com/api/?name=Staff+4&background=random', 'STAFF', TRUE, TRUE, NOW(), NOW()),
    (3, 'owner@example.com',    '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'Fund Owner',   '0900000003', 'https://ui-avatars.com/api/?name=Fund+Owner&background=random', 'FUND_OWNER', TRUE, TRUE, NOW(), NOW()),
    (4, 'user@example.com',     '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'Normal User',  '0900000004', 'https://ui-avatars.com/api/?name=Normal+User&background=random', 'USER', TRUE, FALSE, NOW(), NOW()),
    (5, 'alice@example.com',    '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'Alice Nguyen', '0900000005', 'https://ui-avatars.com/api/?name=Alice+Nguyen&background=random', 'USER', TRUE, TRUE, NOW(), NOW()),
    (6, 'bob@example.com',      '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'Bob Tran',     '0900000006', 'https://ui-avatars.com/api/?name=Bob+Tran&background=random', 'USER', TRUE, TRUE, NOW(), NOW()),
    (300, 'unverified@test.com', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'Unverified Tester', '0900000300', 'https://ui-avatars.com/api/?name=Unverified+Tester&background=random', 'USER', TRUE, FALSE, NOW(), NOW())
ON DUPLICATE KEY UPDATE email = VALUES(email), role = VALUES(role), full_name = VALUES(full_name);

-- Bank accounts (link to sample users)
INSERT INTO bank_account (id, user_id, bank_code, account_number, account_holder_name, is_verified, status, created_at, updated_at)
VALUES
    (1, 3, 'VCB', '123456789', 'Fund Owner', TRUE, 'ACTIVE', NOW(), NOW()),
    (2, 4, 'ACB', '987654321', 'Normal User', FALSE, 'PENDING', NOW(), NOW())
ON DUPLICATE KEY UPDATE account_number = VALUES(account_number);

-- Switch back to campaign DB for sample campaign data
USE trustfundme_campaign_db;

-- Campaign categories
INSERT INTO campaign_categories (id, name, description, created_at, updated_at)
VALUES
    (1, 'Nhân đạo', 'Cứu trợ nhân đạo và hỗ trợ khẩn cấp', NOW(), NOW()),
    (2, 'Nông nghiệp', 'Phát triển nông nghiệp và hỗ trợ nông dân', NOW(), NOW()),
    (3, 'Giáo dục', 'Giáo dục, trường học và học bổng', NOW(), NOW()),
    (4, 'Y tế', 'Y tế, chữa bệnh và hỗ trợ bệnh nhi', NOW(), NOW()),
    (5, 'Môi trường', 'Bảo vệ môi trường và tái tạo rừng', NOW(), NOW()),
    (6, 'Động vật', 'Cứu hộ và chăm sóc động vật', NOW(), NOW())
ON DUPLICATE KEY UPDATE name = VALUES(name), description = VALUES(description);

-- Campaigns (fund_owner_id points to user id = 3)
-- Campaign types: ITEMIZED (quỹ theo khoản mục, tự APPROVED khi tạo chi tiêu)
--                 AUTHORIZED (quỹ ủy quyền, cần staff duyệt chi tiêu)
-- Campaign statuses: PENDING_APPROVAL (mới gửi duyệt), APPROVED (đang gây quỹ), DRAFT (nháp)
INSERT INTO campaigns (id, fund_owner_id, approved_by_staff, approved_at, thank_message, balance, title, cover_image, description, category_id, start_date, end_date, status, rejection_reason, type, created_at, updated_at)
VALUES
    (1, 3, 2, NOW(), 'Cảm ơn tấm lòng vàng của các bạn dành cho miền Trung!', 50000000.00, 'Cứu trợ lũ lụt khẩn cấp miền Trung 2024', NULL, 'Chiến dịch tập trung cung cấp nhu yếu phẩm khẩn cấp cho bà con vùng lũ Quảng Bình, Quảng Trị.', 1, NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), 'APPROVED', NULL, 'ITEMIZED', NOW(), NOW()),
    (2, 3, NULL, NULL, NULL, 0.00, 'Hỗ trợ cây giống tái thiết sau bão', NULL, 'Cung cấp cây giống và vật tư nông nghiệp để bà con ổn định cuộc sống sau mùa lũ.', 2, NOW(), DATE_ADD(NOW(), INTERVAL 60 DAY), 'DRAFT', NULL, 'ITEMIZED', NOW(), NOW()),
    (3, 3, NULL, NULL, NULL, 0.00, 'Xây trường cho em vùng cao Hà Giang', NULL, 'Góp gạch xây dựng điểm trường mầm non kiên cố cho trẻ em tại vùng sâu vùng xa Hà Giang.', 3, NOW(), DATE_ADD(NOW(), INTERVAL 90 DAY), 'PENDING_APPROVAL', NULL, 'ITEMIZED', NOW(), NOW()),
    (4, 3, 2, NOW(), NULL, 25000000.00, 'Quỹ hỗ trợ bệnh nhi ung thư nghèo', NULL, 'Hỗ trợ chi phí điều trị và thuốc men cho các bệnh nhi mắc bệnh hiểm nghèo có hoàn cảnh đặc biệt.', 4, NOW(), DATE_ADD(NOW(), INTERVAL 365 DAY), 'APPROVED', NULL, 'AUTHORIZED', NOW(), NOW()),
    (5, 3, NULL, NULL, NULL, 0.00, 'Trồng 1000 cây xanh phủ xanh đồi trọc', NULL, 'Chung tay đóng góp cây giống để phục hồi rừng đầu nguồn, bảo vệ môi trường bền vững.', 5, NOW(), DATE_ADD(NOW(), INTERVAL 120 DAY), 'PENDING_APPROVAL', NULL, 'ITEMIZED', NOW(), NOW()),
    (6, 3, 2, NOW(), NULL, 12000000.00, 'Cứu hộ và chăm sóc chó mèo bị bỏ rơi', NULL, 'Xây dựng mái ấm và cung cấp thức ăn, y tế cho các bạn động vật bị bỏ rơi hoặc ngược đãi.', 6, NOW(), DATE_ADD(NOW(), INTERVAL 180 DAY), 'APPROVED', NULL, 'AUTHORIZED', NOW(), NOW()),
    (7, 3, NULL, NULL, NULL, 0.00, 'Học bổng Chắp cánh ước mơ 2024', NULL, 'Trao học bổng cho học sinh nghèo vượt khó tại các tỉnh miền núi phía Bắc.', 3, NOW(), DATE_ADD(NOW(), INTERVAL 45 DAY), 'PENDING_APPROVAL', NULL, 'ITEMIZED', NOW(), NOW())
ON DUPLICATE KEY UPDATE title = VALUES(title), category_id = VALUES(category_id), status = VALUES(status), type = VALUES(type), description = VALUES(description);

-- Fundraising goals (mỗi chiến dịch có 1 mục tiêu gây quỹ)
INSERT INTO fundraising_goals (id, campaign_id, target_amount, description, is_active, created_at, updated_at)
VALUES
    (1, 1, 100000000.00, 'Ngân sách cho 2000 phần quà cứu trợ (mì tôm, nước, thuốc)', TRUE, NOW(), NOW()),
    (2, 2, 50000000.00, 'Hỗ trợ 500 hộ dân cây giống lúa và ngô', TRUE, NOW(), NOW()),
    (3, 3, 200000000.00, 'Xây dựng 1 điểm trường mầm non kiên cố 2 phòng học', TRUE, NOW(), NOW()),
    (4, 4, 500000000.00, 'Hỗ trợ chi phí điều trị cho 50 bệnh nhi trong 1 năm', TRUE, NOW(), NOW()),
    (5, 5, 30000000.00, 'Mua 1000 cây giống và thuê nhân công trồng rừng', TRUE, NOW(), NOW()),
    (6, 6, 80000000.00, 'Chi phí vận hành mái ấm cho 100 thú cưng trong 6 tháng', TRUE, NOW(), NOW()),
    (7, 7, 150000000.00, 'Trao 30 suất học bổng 5 triệu/suất cho học sinh vượt khó', TRUE, NOW(), NOW())
ON DUPLICATE KEY UPDATE target_amount = VALUES(target_amount), description = VALUES(description);


-- Expenditures
-- Mỗi campaign chỉ có 1 expenditure mẫu (UNIQUE campaign_id)
-- Expenditure statuses:
--   APPROVED         → ITEMIZED campaign: tự approved khi fund owner tạo
--   PENDING_REVIEW   → AUTHORIZED campaign: cần staff duyệt
--   WITHDRAWAL_REQUESTED → Sau khi fund owner bấm "Yêu cầu rút tiền"
--   DISBURSED        → Sau khi admin xác nhận đã chuyển tiền
INSERT INTO expenditures (id, campaign_id, evidence_due_at, evidence_status, total_amount, total_expected_amount, variance, plan, status, is_withdrawal_requested, staff_review_id, reject_reason, created_at, updated_at)
VALUES
    -- Campaign 1 (ITEMIZED/ACTIVE): fund owner đã yêu cầu rút tiền
    (1, 1, DATE_ADD(NOW(), INTERVAL 3 DAY), 'PENDING', 0.00, 15000000.00, 15000000.00, 'Chi mua nhu yếu phẩm đợt 1 cho huyện Lệ Thủy', 'WITHDRAWAL_REQUESTED', TRUE, NULL, NULL, NOW(), NOW()),
    -- Campaign 4 (AUTHORIZED/ACTIVE): đã DISBURSED
    (2, 4, DATE_ADD(NOW(), INTERVAL 7 DAY), 'PENDING', 5000000.00, 5000000.00, 0.00, 'Mua thiết bị y tế đợt 1', 'DISBURSED', TRUE, 2, NULL, NOW(), NOW()),
    -- Campaign 6 (AUTHORIZED/ACTIVE): chờ staff duyệt
    (3, 6, DATE_ADD(NOW(), INTERVAL 5 DAY), 'PENDING', 0.00, 8000000.00, 8000000.00, 'Mua thức ăn và thuốc thú y', 'PENDING_REVIEW', FALSE, NULL, NULL, NOW(), NOW())
ON DUPLICATE KEY UPDATE plan = VALUES(plan), status = VALUES(status), total_expected_amount = VALUES(total_expected_amount), staff_review_id = VALUES(staff_review_id), reject_reason = VALUES(reject_reason);

-- Expenditure Items
-- exp 1 (Campaign 1 - ITEMIZED): đã WITHDRAWAL_REQUESTED
-- exp 2 (Campaign 4 - AUTHORIZED): đã DISBURSED
-- exp 3 (Campaign 6 - AUTHORIZED): PENDING_REVIEW, chờ staff duyệt
INSERT INTO expenditure_items (expenditure_id, category, quantity, actual_quantity, quantity_left, price, expected_price, note, created_at, updated_at)
VALUES
    (1, 'Mì tôm', 500, 0, 500, 0.00, 15000.00, 'Thùng mì tôm Hảo Hảo', NOW(), NOW()),
    (1, 'Nước sạch', 1000, 0, 1000, 0.00, 5000.00, 'Chai nước khoáng 500ml', NOW(), NOW()),
    (1, 'Lương khô', 200, 0, 200, 0.00, 12500.00, 'Gói lương khô quân đội', NOW(), NOW()),
    (2, 'Máy đo huyết áp', 10, 10, 0, 500000.00, 500000.00, 'Máy Omron tự động', NOW(), NOW()),
    (3, 'Thức ăn hạt', 100, 0, 100, 0.00, 50000.00, 'Thức ăn cho chó mèo', NOW(), NOW()),
    (3, 'Thuốc thú y', 20, 0, 20, 0.00, 150000.00, 'Vaccine và thuốc tẩy giun', NOW(), NOW());


-- Campaign follows (user 4 follows campaign 1)
INSERT INTO campaign_follows (campaign_id, user_id, followed_at)
VALUES
    (1, 4, NOW())
ON DUPLICATE KEY UPDATE followed_at = VALUES(followed_at);

-- =======================================
-- 5. Sample Data: feed-service (Mapped to trustfundme_campaign_db)
-- =======================================
USE trustfundme_campaign_db;

INSERT INTO feed_post (author_id, author_name, visibility, title, content, status, reply_count, view_count, like_count, comment_count, is_pinned, created_at, target_id, target_type, target_name)
VALUES
    -- General Discussion
    (5, 'Alice Nguyen', 'PUBLIC', 'Chào mọi người!', 'Xin chào các bạn, mình là thành viên mới. Rất vui được tham gia cộng đồng TrustFundME!', 'PUBLISHED', 2, 0, 3, 2, FALSE, NOW(), NULL, NULL, NULL),
    (6, 'Bob Tran', 'PUBLIC', 'Thảo luận về từ thiện minh bạch', 'Mình thấy việc minh bạch tài chính là quan trọng nhất. Các bạn nghĩ sao?', 'PUBLISHED', 5, 0, 8, 5, FALSE, DATE_SUB(NOW(), INTERVAL 1 DAY), NULL, NULL, NULL),

    -- Campaign Updates - linked to expenditure 1 (EXPENDITURE)
    (3, 'Fund Owner', 'PUBLIC', 'Cập nhật chuyến xe cứu trợ Quảng Bình', 'Đoàn xe chở 500 thùng mì tôm và 1000 chai nước đã xuất phát. Cảm ơn tình cảm của mọi người!', 'PUBLISHED', 10, 156, 25, 10, TRUE, NOW(), 1, 'EXPENDITURE', 'Chi mua nhu yếu phẩm đợt 1 cho huyện Lệ Thủy'),
    (4, 'Normal User', 'PUBLIC', 'Hỏi về các điểm tiếp nhận nhu yếu phẩm', 'Hiện tại mình có ít quần áo cũ và mì tôm, có thể gửi ở đâu Hà Nội ạ?', 'PUBLISHED', 1, 0, 2, 1, FALSE, DATE_SUB(NOW(), INTERVAL 2 HOUR), 1, 'EXPENDITURE', 'Chi mua nhu yếu phẩm đợt 1 cho huyện Lệ Thủy'),

    -- QA
    (5, 'Alice Nguyen', 'PUBLIC', 'Làm sao để tạo chiến dịch?', 'Mình có hoàn cảnh khó khăn cần giúp đỡ, thủ tục tạo chiến dịch như thế nào?', 'PUBLISHED', 0, 0, 1, 0, FALSE, DATE_SUB(NOW(), INTERVAL 5 HOUR), NULL, NULL, NULL),

    -- News/Announcement
    (1, 'Admin User', 'PUBLIC', 'Thông báo bảo trì hệ thống', 'Hệ thống sẽ bảo trì vào 0h ngày mai. Mong các bạn lưu ý.', 'PUBLISHED', 0, 0, 15, 0, TRUE, DATE_SUB(NOW(), INTERVAL 3 DAY), NULL, NULL, NULL)
ON DUPLICATE KEY UPDATE title = VALUES(title);

-- =======================================
-- 6. Schema: payment-service (DB: trustfundme_payment_db)
-- =======================================
USE trustfundme_payment_db;

-- payments
CREATE TABLE IF NOT EXISTS `payments` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `description` VARCHAR(255) NULL,
    `amount` DECIMAL(19, 4) NOT NULL,
    `qr_code` VARCHAR(1000) NULL,
    `payment_link_id` VARCHAR(255) UNIQUE NULL,
    `status` VARCHAR(50) NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- donations
CREATE TABLE IF NOT EXISTS `donations` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `donor_id` BIGINT NULL,
    `campaign_id` BIGINT NULL,
    `payment_id` BIGINT NULL,
    `donation_amount` DECIMAL(19, 4) NULL,
    `tip_amount` DECIMAL(19, 4) NULL,
    `total_amount` DECIMAL(19, 4) NULL,
    `status` VARCHAR(50) NULL,
    `is_anonymous` BOOLEAN NOT NULL DEFAULT FALSE,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`payment_id`) REFERENCES `payments`(`id`) ON DELETE CASCADE
);

-- donation_items
CREATE TABLE IF NOT EXISTS `donation_items` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `donation_id` BIGINT NOT NULL,
    `expenditure_item_id` BIGINT NULL,
    `quantity` INT NULL,
    `amount` DECIMAL(19, 4) NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`donation_id`) REFERENCES `donations`(`id`) ON DELETE CASCADE
);

-- Add sample data for the new Expenditure Transaction structure
USE trustfundme_campaign_db;
-- Creating a sample campaign for payout testing
INSERT INTO campaigns (id, fund_owner_id, title, description, balance, type, status, category_id)
VALUES (10, 3, 'Hỗ trợ đồng bào vùng lũ', 'Quyên góp khẩn cấp cho miền Trung', 450000000, 'AUTHORIZED', 'APPROVED', 1);

-- Fundraising goal for campaign 10
INSERT INTO fundraising_goals (campaign_id, target_amount, description, is_active)
VALUES (10, 500000000, 'Ngân sách hỗ trợ khẩn cấp', TRUE);

-- Expenditure for campaign 10 (AUTHORIZED)
INSERT INTO expenditures (id, campaign_id, total_amount, total_expected_amount, variance, plan, status, is_withdrawal_requested, created_at)
VALUES (10, 10, 0, 15000000, 15000000, 'Mua 300 suất quà nhu yếu phẩm', 'WITHDRAWAL_REQUESTED', true, NOW());

-- Transaction for expenditure 10
INSERT INTO expenditure_transactions (expenditure_id, amount, from_user_id, to_user_id, from_bank_code, from_account_number, from_account_holder_name, to_bank_code, to_account_number, to_account_holder_name, type, status)
VALUES (10, 15000000, 1, 3, 'VCB', '0011001234567', 'TRUSTFUND ADMIN', 'MB', '999988887777', 'NGUYEN VAN A', 'PAYOUT', 'PENDING');
