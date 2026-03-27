-- =======================================
-- Extra sample feed posts
-- Run after init-all-databases.sql (và tùy chọn seed-campaigns.sql cho bài gắn campaign 11–12).
-- DB: trustfundme_campaign_db — khớp feed_post trong init-all-databases.sql.
-- IDs 8–20: init đã tạo 6 bài (id 1–6), không dùng trustfundme_feed_db hay cột category/type/budget cũ.
-- author_id: 1 admin, 2 staff1, 3 fund owner, 4 user, 5 alice, 6 bob (identity seed).
--
-- Nếu feed_post là DDL cũ (thiếu cột): khối dưới thêm cột khi chưa có (MySQL 5.7+ / 8.0 / MariaDB 10.x).
-- Hoặc DROP TABLE feed_post rồi chạy CREATE trong init-all-databases.sql.
-- =======================================

USE trustfundme_campaign_db;

-- Portable: ADD COLUMN chỉ khi INFORMATION_SCHEMA chưa có cột (tránh IF NOT EXISTS — không có trên mọi bản MySQL).
SET @seed_db := DATABASE();

SET @seed_chk := (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = @seed_db AND TABLE_NAME = 'feed_post' AND COLUMN_NAME = 'author_name');
SET @seed_sql := IF(@seed_chk = 0,
  'ALTER TABLE feed_post ADD COLUMN author_name VARCHAR(255) NULL COMMENT ''Cached author full name for display''',
  'SELECT 1');
PREPARE seed_stmt FROM @seed_sql;
EXECUTE seed_stmt;
DEALLOCATE PREPARE seed_stmt;

SET @seed_chk := (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = @seed_db AND TABLE_NAME = 'feed_post' AND COLUMN_NAME = 'parent_post_id');
SET @seed_sql := IF(@seed_chk = 0,
  'ALTER TABLE feed_post ADD COLUMN parent_post_id BIGINT NULL',
  'SELECT 1');
PREPARE seed_stmt FROM @seed_sql;
EXECUTE seed_stmt;
DEALLOCATE PREPARE seed_stmt;

SET @seed_chk := (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = @seed_db AND TABLE_NAME = 'feed_post' AND COLUMN_NAME = 'is_locked');
SET @seed_sql := IF(@seed_chk = 0,
  'ALTER TABLE feed_post ADD COLUMN is_locked BOOLEAN DEFAULT FALSE',
  'SELECT 1');
PREPARE seed_stmt FROM @seed_sql;
EXECUTE seed_stmt;
DEALLOCATE PREPARE seed_stmt;

SET @seed_chk := (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = @seed_db AND TABLE_NAME = 'feed_post' AND COLUMN_NAME = 'target_id');
SET @seed_sql := IF(@seed_chk = 0,
  'ALTER TABLE feed_post ADD COLUMN target_id BIGINT NULL COMMENT ''ID of linked entity (campaign or expenditure)''',
  'SELECT 1');
PREPARE seed_stmt FROM @seed_sql;
EXECUTE seed_stmt;
DEALLOCATE PREPARE seed_stmt;

SET @seed_chk := (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = @seed_db AND TABLE_NAME = 'feed_post' AND COLUMN_NAME = 'target_type');
SET @seed_sql := IF(@seed_chk = 0,
  'ALTER TABLE feed_post ADD COLUMN target_type VARCHAR(50) NULL COMMENT ''EXPENDITURE or CAMPAIGN''',
  'SELECT 1');
PREPARE seed_stmt FROM @seed_sql;
EXECUTE seed_stmt;
DEALLOCATE PREPARE seed_stmt;

SET @seed_chk := (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = @seed_db AND TABLE_NAME = 'feed_post' AND COLUMN_NAME = 'target_name');
SET @seed_sql := IF(@seed_chk = 0,
  'ALTER TABLE feed_post ADD COLUMN target_name VARCHAR(255) NULL COMMENT ''Cached name of linked entity''',
  'SELECT 1');
PREPARE seed_stmt FROM @seed_sql;
EXECUTE seed_stmt;
DEALLOCATE PREPARE seed_stmt;

INSERT INTO feed_post (
    id,
    author_id,
    author_name,
    visibility,
    title,
    content,
    status,
    reply_count,
    view_count,
    like_count,
    comment_count,
    is_pinned,
    is_locked,
    created_at,
    updated_at,
    target_id,
    target_type,
    target_name,
    parent_post_id
) VALUES
    (8, 4, 'Normal User', 'PUBLIC', 'Ý tưởng gây quỹ cộng đồng',
     'Mọi người có ý tưởng nào hay cho hoạt động gây quỹ tại địa phương không? Mình muốn tham khảo.',
     'PUBLISHED', 3, 28, 0, 3, FALSE, FALSE, DATE_SUB(NOW(), INTERVAL 2 DAY), NULL, NULL, NULL, NULL, NULL),

    (9, 6, 'Bob Tran', 'PUBLIC', 'Cảm ơn TrustFundME',
     'Sau khi nhận được hỗ trợ từ chiến dịch, gia đình mình đã vượt qua khó khăn. Cảm ơn cộng đồng!',
     'PUBLISHED', 12, 89, 2, 12, FALSE, FALSE, DATE_SUB(NOW(), INTERVAL 1 DAY), NULL, NULL, NULL, NULL, NULL),

    (10, 5, 'Alice Nguyen', 'PUBLIC', 'Chia sẻ kinh nghiệm vận hành chiến dịch',
     'Mình đã chạy 2 chiến dịch nhỏ, muốn chia sẻ cách lên kế hoạch và báo cáo minh bạch.',
     'PUBLISHED', 0, 15, 1, 0, FALSE, FALSE, DATE_SUB(NOW(), INTERVAL 12 HOUR), NULL, NULL, NULL, NULL, NULL),

    (11, 3, 'Fund Owner', 'PUBLIC', 'Cập nhật: Nước sạch cho trẻ em vùng núi',
     'Đã hoàn thành 50% hạng mục khoan thử. Tuần sau sẽ có báo cáo hình ảnh chi tiết.',
     'PUBLISHED', 5, 67, 4, 5, FALSE, FALSE, DATE_SUB(NOW(), INTERVAL 6 HOUR), NULL,
     11, 'CAMPAIGN', 'Nước sạch cho trẻ em vùng núi', NULL),

    (12, 4, 'Normal User', 'PUBLIC', 'Nên ủng hộ chiến dịch nào trước?',
     'Mình có ngân sách nhỏ, muốn đóng góp hiệu quả. Mọi người gợi ý giúp chiến dịch nào đáng ủng hộ nhất ạ?',
     'PUBLISHED', 8, 44, 0, 8, FALSE, FALSE, DATE_SUB(NOW(), INTERVAL 1 DAY), NULL, NULL, NULL, NULL, NULL),

    (13, 3, 'Fund Owner', 'PUBLIC', 'Bộ kit y tế – đã giao đợt 1',
     'Đã giao 50 bộ dụng cụ y tế cho 3 trạm y tế xã. Cảm ơn các nhà hảo tâm!',
     'PUBLISHED', 2, 31, 6, 2, FALSE, FALSE, NOW(), NULL,
     12, 'CAMPAIGN', 'Bộ kit y tế cho trạm xã', NULL),

    (14, 6, 'Bob Tran', 'PUBLIC', 'Rút tiền từ chiến dịch mất bao lâu?',
     'Chiến dịch của mình đã kết thúc và đủ điều kiện rút. Cho mình hỏi thời gian xử lý rút tiền khoảng bao lâu?',
     'PUBLISHED', 1, 22, 0, 1, FALSE, FALSE, DATE_SUB(NOW(), INTERVAL 3 HOUR), NULL, NULL, NULL, NULL, NULL),

    (15, 5, 'Alice Nguyen', 'PUBLIC', 'Có được tạo nhiều chiến dịch cùng lúc không?',
     'Mình là fund owner, đang chạy 1 chiến dịch. Có thể mở thêm chiến dịch thứ 2 song song không?',
     'PUBLISHED', 4, 18, 0, 4, FALSE, FALSE, DATE_SUB(NOW(), INTERVAL 1 DAY), NULL, NULL, NULL, NULL, NULL),

    (16, 4, 'Normal User', 'PUBLIC', 'Quyên góp ẩn danh có được không?',
     'Mình muốn ủng hộ nhưng không hiển thị tên. Hệ thống có hỗ trợ quyên góp ẩn danh không?',
     'PUBLISHED', 0, 9, 0, 0, FALSE, FALSE, DATE_SUB(NOW(), INTERVAL 5 HOUR), NULL, NULL, NULL, NULL, NULL),

    (17, 1, 'Admin User', 'PUBLIC', 'Thông báo: cập nhật khu vực thảo luận',
     'Chúng tôi cải thiện feed cộng đồng: ghim bài, báo cáo và liên kết chiến dịch/đợt chi. Mời mọi người dùng thử.',
     'PUBLISHED', 0, 210, 3, 0, TRUE, FALSE, DATE_SUB(NOW(), INTERVAL 4 DAY), NULL, NULL, NULL, NULL, NULL),

    (18, 2, 'Staff 1', 'PUBLIC', 'Lịch tập huấn Fund Owner tháng tới',
     'Lớp "Vận hành chiến dịch minh bạch" sẽ mở đăng ký qua kênh hỗ trợ. Chi tiết sẽ gửi email.',
     'PUBLISHED', 0, 56, 1, 0, FALSE, FALSE, DATE_SUB(NOW(), INTERVAL 2 DAY), NULL, NULL, NULL, NULL, NULL),

    (19, 3, 'Fund Owner', 'PUBLIC', 'Bài nháp – chưa đăng',
      'Nội dung đang soạn, sẽ đăng sau.',
     'DRAFT', 0, 0, 0, 0, FALSE, FALSE, NOW(), NULL, NULL, NULL, NULL, NULL),

    -- Gắn expenditure id 1 từ init (campaign 1); id 20 tránh trùng id 1–6 và các bài seed phía trên.
    (20, 3, 'Fund Owner', 'PUBLIC', 'Ghi chú seed: bài gắn đợt chi (expenditure 1)',
     'Bài mẫu bổ sung cho test target EXPENDITURE. Ý nghĩa tương tự bài cập nhật cứu trợ trong init.',
     'PUBLISHED', 0, 5, 0, 0, FALSE, FALSE, DATE_SUB(NOW(), INTERVAL 1 HOUR), NULL,
     1, 'EXPENDITURE', 'Chi mua nhu yếu phẩm đợt 1 cho huyện Lệ Thủy', NULL)
ON DUPLICATE KEY UPDATE
    title = VALUES(title),
    content = VALUES(content),
    author_name = VALUES(author_name),
    status = VALUES(status),
    reply_count = VALUES(reply_count),
    view_count = VALUES(view_count),
    like_count = VALUES(like_count),
    comment_count = VALUES(comment_count),
    target_id = VALUES(target_id),
    target_type = VALUES(target_type),
    target_name = VALUES(target_name),
    updated_at = NOW();

SELECT COUNT(*) AS feed_posts_total FROM feed_post;
SELECT id, author_id, status, target_type, target_id, LEFT(title, 40) AS title_preview
FROM feed_post ORDER BY id DESC LIMIT 25;
