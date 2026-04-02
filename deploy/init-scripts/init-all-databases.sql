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

-- internal_transactions
CREATE TABLE `internal_transactions` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `from_campaign_id` BIGINT NULL,
    `to_campaign_id` BIGINT NULL,
    `amount` DECIMAL(19, 4) NOT NULL,
    `type` VARCHAR(50) NOT NULL COMMENT 'RECOVERY, SUPPORT, INITIAL',
    `reason` TEXT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`from_campaign_id`) REFERENCES `campaigns`(`id`) ON DELETE SET NULL,
    FOREIGN KEY (`to_campaign_id`) REFERENCES `campaigns`(`id`) ON DELETE SET NULL,
    INDEX `idx_int_trans_from` (`from_campaign_id`),
    INDEX `idx_int_trans_to` (`to_campaign_id`)
);

-- approval_tasks
CREATE TABLE `approval_tasks` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
-- ... rest of the file ...
    -- Group 4: Quản lý quỹ
    (12, 4, 'Yêu cầu giải ngân', '/payouts', 'clipboard-check', 1, TRUE, NOW(), NOW()),
    (13, 4, 'Lịch sử giải ngân', '/payout-history', 'history', 2, TRUE, NOW(), NOW()),
    (22, 4, 'Quỹ chung', '/general-fund', 'database', 0, TRUE, NOW(), NOW()),

    -- Group 5: Giao dịch
-- ... rest of the file ...
-- Campaigns (fund_owner_id points to user id = 3)
-- Campaign types: ITEMIZED (quỹ theo khoản mục, tự APPROVED khi tạo chi tiêu)
--                 AUTHORIZED (quỹ ủy quyền, cần staff duyệt chi tiêu)
--                 GENERAL_FUND (quỹ chung hệ thống)
-- Campaign statuses: PENDING_APPROVAL (mới gửi duyệt), APPROVED (đang gây quỹ), DRAFT (nháp)
INSERT INTO campaigns (id, fund_owner_id, approved_by_staff, approved_at, thank_message, balance, title, cover_image, description, category_id, start_date, end_date, status, rejection_reason, type, created_at, updated_at)
VALUES
    (1, 1, 1, NOW(), NULL, 1000000000.00, 'Quỹ chung hệ thống', NULL, 'Quỹ tập trung để điều tiết nguồn vốn cho các chiến dịch khẩn cấp.', NULL, NOW(), NULL, 'APPROVED', NULL, 'GENERAL_FUND', NOW(), NOW()),
    (101, 3, 2, NOW(), 'Cảm ơn tấm lòng vàng của các bạn dành cho miền Trung!', 50000000.00, 'Cứu trợ lũ lụt khẩn cấp miền Trung 2024', NULL, 'Chiến dịch tập trung cung cấp nhu yếu phẩm khẩn cấp cho bà con vùng lũ Quảng Bình, Quảng Trị.', 1, NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), 'APPROVED', NULL, 'ITEMIZED', NOW(), NOW()),
-- ... update other campaign IDs ...
    (102, 3, NULL, NULL, NULL, 0.00, 'Hỗ trợ cây giống tái thiết sau bão', NULL, 'Cung cấp cây giống và vật tư nông nghiệp để bà con ổn định cuộc sống sau mùa lũ.', 2, NOW(), DATE_ADD(NOW(), INTERVAL 60 DAY), 'DRAFT', NULL, 'ITEMIZED', NOW(), NOW()),
    (103, 3, NULL, NULL, NULL, 0.00, 'Xây trường cho em vùng cao Hà Giang', NULL, 'Góp gạch xây dựng điểm trường mầm non kiên cố cho trẻ em tại vùng sâu vùng xa Hà Giang.', 3, NOW(), DATE_ADD(NOW(), INTERVAL 90 DAY), 'PENDING_APPROVAL', NULL, 'ITEMIZED', NOW(), NOW()),
    (104, 3, 2, NOW(), NULL, 25000000.00, 'Quỹ hỗ trợ bệnh nhi ung thư nghèo', NULL, 'Hỗ trợ chi phí điều trị và thuốc men cho các bệnh nhi mắc bệnh hiểm nghèo có hoàn cảnh đặc biệt.', 4, NOW(), DATE_ADD(NOW(), INTERVAL 365 DAY), 'APPROVED', NULL, 'AUTHORIZED', NOW(), NOW()),
    (105, 3, NULL, NULL, NULL, 0.00, 'Trồng 1000 cây xanh phủ xanh đồi trọc', NULL, 'Chung tay đóng góp cây giống để phục hồi rừng đầu nguồn, bảo vệ môi trường bền vững.', 5, NOW(), DATE_ADD(NOW(), INTERVAL 120 DAY), 'PENDING_APPROVAL', NULL, 'ITEMIZED', NOW(), NOW()),
    (106, 3, 2, NOW(), NULL, 12000000.00, 'Cứu hộ và chăm sóc chó mèo bị bỏ rơi', NULL, 'Xây dựng mái ấm và cung cấp thức ăn, y tế cho các bạn động vật bị bỏ rơi hoặc ngược đãi.', 6, NOW(), DATE_ADD(NOW(), INTERVAL 180 DAY), 'APPROVED', NULL, 'AUTHORIZED', NOW(), NOW()),
    (107, 3, NULL, NULL, NULL, 0.00, 'Học bổng Chắp cánh ước mơ 2024', NULL, 'Trao học bổng cho học sinh nghèo vượt khó tại các tỉnh miền núi phía Bắc.', 3, NOW(), DATE_ADD(NOW(), INTERVAL 45 DAY), 'PENDING_APPROVAL', NULL, 'ITEMIZED', NOW(), NOW())
ON DUPLICATE KEY UPDATE title = VALUES(title), category_id = VALUES(category_id), status = VALUES(status), type = VALUES(type), description = VALUES(description);

-- Fundraising goals (mỗi chiến dịch có 1 mục tiêu gây quỹ)
INSERT INTO fundraising_goals (id, campaign_id, target_amount, description, is_active, created_at, updated_at)
VALUES
    (101, 101, 100000000.00, 'Ngân sách cho 2000 phần quà cứu trợ (mì tôm, nước, thuốc)', TRUE, NOW(), NOW()),
    (102, 102, 50000000.00, 'Hỗ trợ 500 hộ dân cây giống lúa và ngô', TRUE, NOW(), NOW()),
    (103, 103, 200000000.00, 'Xây dựng 1 điểm trường mầm non kiên cố 2 phòng học', TRUE, NOW(), NOW()),
    (104, 104, 500000000.00, 'Hỗ trợ chi phí điều trị cho 50 bệnh nhi trong 1 năm', TRUE, NOW(), NOW()),
    (105, 105, 30000000.00, 'Mua 1000 cây giống và thuê nhân công trồng rừng', TRUE, NOW(), NOW()),
    (106, 106, 80000000.00, 'Chi phí vận hành mái ấm cho 100 thú cưng trong 6 tháng', TRUE, NOW(), NOW()),
    (107, 107, 150000000.00, 'Trao 30 suất học bổng 5 triệu/suất cho học sinh vượt khó', TRUE, NOW(), NOW())
ON DUPLICATE KEY UPDATE target_amount = VALUES(target_amount), description = VALUES(description);

-- Expenditures
-- ... và các bảng khác cần cập nhật campaign_id ...



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
VALUES
    -- expenditure_id=1 (WITHDRAWAL_REQUESTED): pending payout
    (1, 15000000, 1, 3, 'VCB', '0011001234567', 'TRUSTFUND ADMIN', 'MB', '999988887777', 'NGUYEN VAN A', 'PAYOUT', 'PENDING'),
    -- expenditure_id=2 (DISBURSED): completed payout
    (2, 5000000, 1, 4, 'VCB', '0011001234567', 'TRUSTFUND ADMIN', 'TPB', '123456789012', 'TRAN THI B', 'PAYOUT', 'COMPLETED'),
    -- expenditure_id=3 (PENDING_REVIEW): rejected payout (example)
    (3, 8000000, 1, 5, 'VCB', '0011001234567', 'TRUSTFUND ADMIN', 'ACB', '987654321098', 'LE VAN C', 'PAYOUT', 'REJECTED');
