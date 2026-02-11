-- Full reset + create for all TrustFundME databases
-- Safe to run on empty DB host or to recreate schema from scratch.
-- WARNING: This will DROP existing databases (campaign + identity).

-- =======================================
-- 0. Drop & recreate databases
-- =======================================
DROP DATABASE IF EXISTS trustfundme_campaign_db;
DROP DATABASE IF EXISTS trustfundme_identity_db;
DROP DATABASE IF EXISTS trustfundme_media_db;
DROP DATABASE IF EXISTS trustfundme_feed_db;
DROP DATABASE IF EXISTS trustfundme_moderation_db;
DROP DATABASE IF EXISTS trustfundme_flag_db;
DROP DATABASE IF EXISTS trustfundme_chat_db;

CREATE DATABASE trustfundme_campaign_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE trustfundme_identity_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE trustfundme_media_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE trustfundme_feed_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE trustfundme_flag_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE trustfundme_chat_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- =======================================
-- 1. Create user and grant privileges
-- =======================================
CREATE USER IF NOT EXISTS 'trustfundme_user'@'%' IDENTIFIED BY 'trustfundme_password';
GRANT ALL PRIVILEGES ON trustfundme_campaign_db.* TO 'trustfundme_user'@'%';
GRANT ALL PRIVILEGES ON trustfundme_identity_db.* TO 'trustfundme_user'@'%';
GRANT ALL PRIVILEGES ON trustfundme_media_db.* TO 'trustfundme_user'@'%';
GRANT ALL PRIVILEGES ON trustfundme_feed_db.* TO 'trustfundme_user'@'%';
GRANT ALL PRIVILEGES ON trustfundme_flag_db.* TO 'trustfundme_user'@'%';
GRANT ALL PRIVILEGES ON trustfundme_chat_db.* TO 'trustfundme_user'@'%';
FLUSH PRIVILEGES;

-- =======================================
-- 2. Schema: campaign-service (DB: trustfundme_campaign_db)
-- =======================================
USE trustfundme_campaign_db;

-- campaigns
CREATE TABLE campaigns (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    fund_owner_id BIGINT NOT NULL,
    approved_by_staff BIGINT NULL, -- id staff duyệt
    approved_at DATETIME NULL,
    thank_message VARCHAR(2000) NULL,
    balance DECIMAL(19, 4) NOT NULL DEFAULT 0,
    title VARCHAR(255) NOT NULL,
    description VARCHAR(5000) NULL,
    category VARCHAR(100) NULL,
    start_date DATETIME NULL,
    end_date DATETIME NULL,
    status VARCHAR(50) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
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

-- expenditures
CREATE TABLE `expenditures` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `campaign_id` BIGINT NOT NULL,
    `evidence_due_at` DATETIME NULL,
    `evidence_status` VARCHAR(50) NULL,
    `evidence_status` VARCHAR(50) NULL,
    `total_amount` DECIMAL(19, 4) NULL,
    `total_expected_amount` DECIMAL(19, 4) NULL,
    `variance` DECIMAL(19, 4) NULL,
    `plan` VARCHAR(2000) NULL,
    `status` VARCHAR(50) NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`campaign_id`) REFERENCES `campaigns`(`id`) ON DELETE CASCADE,
    INDEX `idx_expenditures_campaign_id` (`campaign_id`),
    INDEX `idx_expenditures_status` (`status`)
);

-- expenditure_items
CREATE TABLE `expenditure_items` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `expenditure_id` BIGINT NOT NULL,
    `category` VARCHAR(255) NULL,
    `quantity` INT NULL,
    `actual_quantity` INT NULL,
    `price` DECIMAL(19, 4) NULL,
    `expected_price` DECIMAL(19, 4) NULL,
    `note` VARCHAR(1000) NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`expenditure_id`) REFERENCES `expenditures`(`id`) ON DELETE CASCADE,
    INDEX `idx_expenditure_items_expenditure_id` (`expenditure_id`)
);

-- =======================================
-- 3. Schema: identity-service (DB: trustfundme_identity_db)
-- =======================================
USE trustfundme_identity_db;

-- users
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    phone_number VARCHAR(255),
    avatar_url VARCHAR(500),
    role VARCHAR(50) NOT NULL DEFAULT 'USER',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    verified BOOLEAN NOT NULL DEFAULT FALSE,
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

CREATE TABLE IF NOT EXISTS media (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    post_id BIGINT NULL,
    campaign_id BIGINT NULL,
    media_type VARCHAR(50) NOT NULL,
    url VARCHAR(1000) NOT NULL,
    description VARCHAR(2000) NULL,
    file_name VARCHAR(255) NULL,
    content_type VARCHAR(100) NULL,
    size_bytes BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_media_post_id (post_id),
    INDEX idx_media_campaign_id (campaign_id)
);

-- =======================================
-- 3.2 Schema: feed-service (DB: trustfundme_feed_db)
-- =======================================
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
    budget_id BIGINT NULL,
    author_id BIGINT NOT NULL,
    category_id BIGINT NULL,
    parent_post_id BIGINT NULL,
    type NVARCHAR(50) NOT NULL,
    visibility NVARCHAR(50) NOT NULL,
    title NVARCHAR(255) NULL,
    content NVARCHAR(2000) NOT NULL,
    status NVARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    reply_count INT DEFAULT 0,
    view_count INT DEFAULT 0,
    is_pinned BOOLEAN DEFAULT FALSE,
    is_locked BOOLEAN DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_feed_post_author_id (author_id),
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

-- =======================================
-- 3.3 Schema: flag-service (DB: trustfundme_flag_db)
-- =======================================
USE trustfundme_flag_db;

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

-- =======================================
-- 3.4 Schema: chat-service (DB: trustfundme_chat_db)
-- =======================================
USE trustfundme_chat_db;

CREATE TABLE IF NOT EXISTS conversations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    staff_id BIGINT NOT NULL,
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

CREATE TABLE IF NOT EXISTS messages (
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

-- =======================================
-- 4. Sample data
-- =======================================
-- Users (passwords are plain text placeholders; replace in real env)
USE trustfundme_identity_db;
INSERT INTO users (id, email, password, full_name, phone_number, avatar_url, role, is_active, verified, created_at, updated_at)
VALUES
    (1, 'admin@example.com',    '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'Admin User',   '0900000001', NULL, 'ADMIN', TRUE, TRUE, NOW(), NOW()),
    (2, 'staff@example.com',    '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'Staff User',   '0900000002', NULL, 'STAFF', TRUE, TRUE, NOW(), NOW()),
    (3, 'owner@example.com',    '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'Fund Owner',   '0900000003', 'https://ui-avatars.com/api/?name=Fund+Owner&background=random', 'FUND_OWNER', TRUE, TRUE, NOW(), NOW()),
    (4, 'user@example.com',     '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'Normal User',  '0900000004', 'https://ui-avatars.com/api/?name=Normal+User&background=random', 'USER', TRUE, FALSE, NOW(), NOW()),
    (5, 'alice@example.com',    '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'Alice Nguyen', '0900000005', 'https://ui-avatars.com/api/?name=Alice+Nguyen&background=random', 'USER', TRUE, TRUE, NOW(), NOW()),
    (6, 'bob@example.com',      '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'Bob Tran',     '0900000006', 'https://ui-avatars.com/api/?name=Bob+Tran&background=random', 'USER', TRUE, TRUE, NOW(), NOW())
ON DUPLICATE KEY UPDATE email = VALUES(email);

-- Bank accounts (link to sample users)
INSERT INTO bank_account (id, user_id, bank_code, account_number, account_holder_name, is_verified, status, created_at, updated_at)
VALUES
    (1, 3, 'VCB', '123456789', 'Fund Owner', TRUE, 'ACTIVE', NOW(), NOW()),
    (2, 4, 'ACB', '987654321', 'Normal User', FALSE, 'PENDING', NOW(), NOW())
ON DUPLICATE KEY UPDATE account_number = VALUES(account_number);

-- Switch back to campaign DB for sample campaign data
USE trustfundme_campaign_db;

-- Campaigns (fund_owner_id points to user id = 3)
INSERT INTO campaigns (id, fund_owner_id, approved_by_staff, approved_at, thank_message, balance, title, description, category, start_date, end_date, status, created_at, updated_at)
VALUES
    (1, 3, 2, NOW(), 'Cảm ơn tấm lòng vàng của các bạn dành cho miền Trung!', 50000000.00, 'Cứu trợ lũ lụt khẩn cấp miền Trung 2024', 'Chiến dịch tập trung cung cấp nhu yếu phẩm khẩn cấp cho bà con vùng lũ Quảng Bình, Quảng Trị.', 'Humanitarian', NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), 'ACTIVE', NOW(), NOW()),
    (2, 3, NULL, NULL, NULL, 0.00, 'Hỗ trợ cây giống tái thiết sau bão', 'Cung cấp cây giống và vật tư nông nghiệp để bà con ổn định cuộc sống sau mùa lũ.', 'Agriculture', NOW(), DATE_ADD(NOW(), INTERVAL 60 DAY), 'DRAFT', NOW(), NOW())
ON DUPLICATE KEY UPDATE title = VALUES(title);

-- Fundraising goals
INSERT INTO fundraising_goals (id, campaign_id, target_amount, description, is_active, created_at, updated_at)
VALUES
    (1, 1, 100000000.00, 'Ngân sách cho 2000 phần quà cứu trợ (mì tôm, nước, thuốc)', TRUE, NOW(), NOW()),
    (2, 2, 50000000.00, 'Hỗ trợ 500 hộ dân cây giống lúa và ngô', TRUE, NOW(), NOW())
ON DUPLICATE KEY UPDATE target_amount = VALUES(target_amount);

-- Expenditures
INSERT INTO expenditures (id, campaign_id, evidence_due_at, evidence_status, total_amount, plan, status, created_at, updated_at)
VALUES
    (1, 1, DATE_ADD(NOW(), INTERVAL 3 DAY), 'PENDING', 15000000.00, 'Chi mua nhu yếu phẩm đợt 1 cho huyện Lệ Thủy', 'APPROVED', NOW(), NOW()),
    (2, 2, DATE_ADD(NOW(), INTERVAL 7 DAY), 'PENDING', 5000000.00, 'Mua cây giống lúa ngắn ngày đợt 1', 'PENDING_REVIEW', NOW(), NOW()),
    (3, 1, DATE_ADD(NOW(), INTERVAL 10 DAY), 'PENDING', 3000000.00, 'Thuê xe tải vận chuyển hàng cứu trợ đợt 2', 'APPROVED', NOW(), NOW()),
    (4, 1, DATE_ADD(NOW(), INTERVAL 15 DAY), 'PENDING', 2000000.00, 'Mua thuốc men và vật tư y tế', 'PENDING_REVIEW', NOW(), NOW())
ON DUPLICATE KEY UPDATE plan = VALUES(plan);

-- Expenditure Items
INSERT INTO expenditure_items (expenditure_id, category, quantity, price, note, created_at, updated_at)
VALUES
    (1, 'Mì tôm', 500, 15000.00, 'Thùng mì tôm Hảo Hảo', NOW(), NOW()),
    (1, 'Nước sạch', 1000, 5000.00, 'Chai nước khoáng 500ml', NOW(), NOW()),
    (1, 'Lương khô', 200, 12500.00, 'Gói lương khô quân đội', NOW(), NOW()),
    (2, 'Giống lúa KH5', 500, 10000.00, 'Kg giống lúa năng suất cao', NOW(), NOW()),
    (3, 'Thuê vận tải', 1, 3000000.00, 'Xe tải 5 tấn đợt 2', NOW(), NOW()),
    (4, 'Thuốc men', 50, 40000.00, 'Gói cứu thương cá nhân', NOW(), NOW());

-- Campaign follows (user 4 follows campaign 1)
INSERT INTO campaign_follows (campaign_id, user_id, followed_at)
VALUES
    (1, 4, NOW())
ON DUPLICATE KEY UPDATE followed_at = VALUES(followed_at);

-- =======================================
-- 5. Sample Data: feed-service
-- =======================================
USE trustfundme_feed_db;
INSERT INTO forum_category (id, name, slug, description, color, display_order, is_active, created_at)
VALUES
    (1, 'Chung', 'general', 'Thảo luận chung về mọi chủ đề', '#6366f1', 1, TRUE, NOW()),
    (2, 'Chiến dịch', 'campaigns', 'Thảo luận về các chiến dịch gây quỹ', '#ff5e14', 2, TRUE, NOW()),
    (3, 'Hỏi đáp', 'qa', 'Hỏi đáp và hỗ trợ cộng đồng', '#10b981', 3, TRUE, NOW()),
    (4, 'Tin tức', 'news', 'Tin tức mới nhất từ hệ thống', '#8b5cf6', 4, TRUE, NOW())
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO feed_post (author_id, category_id, type, visibility, title, content, status, reply_count, view_count, is_pinned, created_at, budget_id)
VALUES
    -- General Category
    (5, 1, 'DISCUSSION', 'PUBLIC', 'Chào mọi người!', 'Xin chào các bạn, mình là thành viên mới. Rất vui được tham gia cộng đồng TrustFundME!', 'PUBLISHED', 2, 15, FALSE, NOW(), NULL),
    (6, 1, 'DISCUSSION', 'PUBLIC', 'Thảo luận về từ thiện minh bạch', 'Mình thấy việc minh bạch tài chính là quan trọng nhất. Các bạn nghĩ sao?', 'PUBLISHED', 5, 42, FALSE, DATE_SUB(NOW(), INTERVAL 1 DAY), NULL),
    
    -- Campaigns Category
    (3, 2, 'CAMPAIGN_UPDATE', 'PUBLIC', 'Cập nhật chuyến xe cứu trợ Quảng Bình', 'Đoàn xe chở 500 thùng mì tôm và 1000 chai nước đã xuất phát. Cảm ơn tình cảm của mọi người!', 'PUBLISHED', 10, 156, TRUE, NOW(), 1),
    (4, 2, 'DISCUSSION', 'PUBLIC', 'Hỏi về các điểm tiếp nhận nhu yếu phẩm', 'Hiện tại mình có ít quần áo cũ và mì tôm, có thể gửi ở đâu Hà Nội ạ?', 'PUBLISHED', 1, 8, FALSE, DATE_SUB(NOW(), INTERVAL 2 HOUR), 1),
    
    -- QA Category
    (5, 3, 'QUESTION', 'PUBLIC', 'Làm sao để tạo chiến dịch?', 'Mình có hoàn cảnh khó khăn cần giúp đỡ, thủ tục tạo chiến dịch như thế nào?', 'PUBLISHED', 0, 5, FALSE, DATE_SUB(NOW(), INTERVAL 5 HOUR), NULL),
    
    -- News Category
    (1, 4, 'ANNOUNCEMENT', 'PUBLIC', 'Thông báo bảo trì hệ thống', 'Hệ thống sẽ bảo trì vào 0h ngày mai. Mong các bạn lưu ý.', 'PUBLISHED', 0, 1024, TRUE, DATE_SUB(NOW(), INTERVAL 3 DAY), NULL)
ON DUPLICATE KEY UPDATE title = VALUES(title);

