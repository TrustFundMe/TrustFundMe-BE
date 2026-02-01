-- =======================================
-- Seed fake feed posts for testing
-- Run after init-all-databases.sql (DB + schema + forum_category + sample users must exist).
-- Safe to run multiple times (ON DUPLICATE KEY UPDATE).
-- Post ids 8–19 to avoid conflict with init sample posts 1–7.
-- author_id 3,4,5,6 = users from identity (Fund Owner, Normal User, Alice, Bob).
-- category_id 1-4 = General, Campaigns, QA, News from init.
-- =======================================

USE trustfundme_feed_db;

-- Additional feed posts (ids 8–19)
INSERT INTO feed_post (id, author_id, category_id, type, visibility, title, content, status, reply_count, view_count, is_pinned, created_at, budget_id, parent_post_id)
VALUES
    -- General (category 1)
    (8, 4, 1, 'DISCUSSION', 'PUBLIC', 'Ý tưởng gây quỹ cộng đồng', 'Mọi người có ý tưởng nào hay cho hoạt động gây quỹ tại địa phương không? Mình muốn tham khảo.', 'PUBLISHED', 3, 28, FALSE, DATE_SUB(NOW(), INTERVAL 2 DAY), NULL, NULL),
    (9, 6, 1, 'DISCUSSION', 'PUBLIC', 'Cảm ơn TrustFundME', 'Sau khi nhận được hỗ trợ từ chiến dịch, gia đình mình đã vượt qua khó khăn. Cảm ơn cộng đồng!', 'PUBLISHED', 12, 89, FALSE, DATE_SUB(NOW(), INTERVAL 1 DAY), NULL, NULL),
    (10, 5, 1, 'DISCUSSION', 'PUBLIC', 'Chia sẻ kinh nghiệm vận hành chiến dịch', 'Mình đã chạy 2 chiến dịch nhỏ, muốn chia sẻ cách lên kế hoạch và báo cáo minh bạch.', 'PUBLISHED', 0, 15, FALSE, DATE_SUB(NOW(), INTERVAL 12 HOUR), NULL, NULL),

    -- Campaigns (category 2) – budget_id 10, 11 = campaigns from seed-campaigns.sql
    (11, 3, 2, 'CAMPAIGN_UPDATE', 'PUBLIC', 'Cập nhật Water For All Children', 'Đã hoàn thành 50% công trình nước sạch. Tuần sau sẽ có báo cáo hình ảnh chi tiết.', 'PUBLISHED', 5, 67, FALSE, DATE_SUB(NOW(), INTERVAL 6 HOUR), 10, NULL),
    (12, 4, 2, 'DISCUSSION', 'PUBLIC', 'Nên ủng hộ chiến dịch nào trước?', 'Mình có ngân sách nhỏ, muốn đóng góp hiệu quả. Mọi người gợi ý giúp chiến dịch nào đáng ủng hộ nhất ạ?', 'PUBLISHED', 8, 44, FALSE, DATE_SUB(NOW(), INTERVAL 1 DAY), NULL, NULL),
    (13, 3, 2, 'CAMPAIGN_UPDATE', 'PUBLIC', 'Medical Kits – đã giao đợt 1', 'Đã giao 50 bộ dụng cụ y tế cho 3 trạm y tế xã. Cảm ơn các nhà hảo tâm!', 'PUBLISHED', 2, 31, FALSE, NOW(), 11, NULL),

    -- QA (category 3)
    (14, 6, 3, 'QUESTION', 'PUBLIC', 'Rút tiền từ chiến dịch mất bao lâu?', 'Chiến dịch của mình đã kết thúc và đủ điều kiện rút. Cho mình hỏi thời gian xử lý rút tiền khoảng bao lâu?', 'PUBLISHED', 1, 22, FALSE, DATE_SUB(NOW(), INTERVAL 3 HOUR), NULL, NULL),
    (15, 5, 3, 'QUESTION', 'PUBLIC', 'Có được tạo nhiều chiến dịch cùng lúc không?', 'Mình là fund owner, đang chạy 1 chiến dịch. Có thể mở thêm chiến dịch thứ 2 song song không?', 'PUBLISHED', 4, 18, FALSE, DATE_SUB(NOW(), INTERVAL 1 DAY), NULL, NULL),
    (16, 4, 3, 'QUESTION', 'PUBLIC', 'Quyên góp ẩn danh có được không?', 'Mình muốn ủng hộ nhưng không hiển thị tên. Hệ thống có hỗ trợ quyên góp ẩn danh không?', 'PUBLISHED', 0, 9, FALSE, DATE_SUB(NOW(), INTERVAL 5 HOUR), NULL, NULL),

    -- News (category 4)
    (17, 1, 4, 'ANNOUNCEMENT', 'PUBLIC', 'Ra mắt tính năng Forum mới', 'Chúng tôi vừa nâng cấp khu vực thảo luận: thêm danh mục, ghim bài và báo cáo. Mời mọi người trải nghiệm.', 'PUBLISHED', 0, 210, TRUE, DATE_SUB(NOW(), INTERVAL 4 DAY), NULL, NULL),
    (18, 2, 4, 'ANNOUNCEMENT', 'PUBLIC', 'Lịch tập huấn cho Fund Owner tháng 2', 'Lớp tập huấn "Vận hành chiến dịch minh bạch" sẽ diễn ra vào 15/02. Đăng ký qua email support.', 'PUBLISHED', 0, 56, FALSE, DATE_SUB(NOW(), INTERVAL 2 DAY), NULL, NULL),

    -- One draft (for testing)
    (19, 3, 1, 'DISCUSSION', 'PUBLIC', 'Bài nháp – chưa đăng', 'Nội dung đang soạn, sẽ đăng sau.', 'DRAFT', 0, 0, FALSE, NOW(), NULL, NULL)
ON DUPLICATE KEY UPDATE
    title = VALUES(title),
    content = VALUES(content),
    status = VALUES(status),
    reply_count = VALUES(reply_count),
    view_count = VALUES(view_count),
    updated_at = NOW();

-- Replies (optional): link to existing posts. Use ids 1–7 from init (first 7 posts). Add replies to post 1 and 3.
-- Only run if your init posts have ids 1–7 and you want reply threads. Uncomment and adjust parent_post_id if needed.
/*
INSERT INTO feed_post (author_id, category_id, type, visibility, title, content, status, reply_count, view_count, is_pinned, created_at, budget_id, parent_post_id)
VALUES
    (6, 1, 'DISCUSSION', 'PUBLIC', NULL, 'Chào bạn, mình cũng mới tham gia. Chúc bạn có nhiều trải nghiệm tốt!', 'PUBLISHED', 0, 0, FALSE, NOW(), NULL, 1),
    (5, 1, 'DISCUSSION', 'PUBLIC', NULL, 'Welcome! Cộng đồng rất thân thiện.', 'PUBLISHED', 0, 0, FALSE, NOW(), NULL, 1),
    (4, 2, 'DISCUSSION', 'PUBLIC', NULL, 'Cảm ơn bạn đã cập nhật. Chúc chiến dịch thuận lợi!', 'PUBLISHED', 0, 0, FALSE, NOW(), NULL, 3)
ON DUPLICATE KEY UPDATE content = VALUES(content);
*/

-- Summary
SELECT COUNT(*) AS feed_posts_total FROM feed_post;
SELECT id, author_id, category_id, type, title, status, created_at FROM feed_post ORDER BY created_at DESC LIMIT 20;
