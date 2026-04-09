-- ============================================================
-- Dữ liệu mẫu: campaigns, feed posts, flags
-- Database: trustfundme_campaign_db
-- Tránh trùng ID: campaigns 21-30, feed_post 21-30, flags 1-10
-- Users: 1=admin, 2=staff1, 3=fund_owner, 4=user, 5=alice, 6=bob
-- ============================================================

USE trustfundme_campaign_db;

-- =======================================
-- 1. CAMPAIGNS (IDs 21-30)
-- =======================================
INSERT INTO campaigns (
    id, fund_owner_id, approved_by_staff, approved_at,
    thank_message, balance, title, cover_image, description,
    category_id, start_date, end_date, status,
    rejection_reason, type, created_at, updated_at
) VALUES
    (21, 3, 2, DATE_SUB(NOW(), INTERVAL 5 DAY),
     'Cảm ơn các bạn đã đồng hành!', 15750000.00,
     'Xe lăn cho người khuyết tật vùng cao',
     NULL,
     'Chương trình trao xe lăn miễn phí cho người khuyết tật tại các huyện vùng cao Tây Bắc.',
     4, DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_ADD(NOW(), INTERVAL 55 DAY),
     'APPROVED', NULL, 'ITEMIZED', NOW(), NOW()),

    (22, 3, 2, DATE_SUB(NOW(), INTERVAL 3 DAY),
     'Một lần nữa cảm ơn tấm lòng!', 8900000.00,
     'Sách giáo khoa cho học sinh nghèo',
     NULL,
     'Cung cấp bộ sách giáo khoa và đồ dùng học tập cho 200 em học sinh nghèo vùng núi.',
     3, DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_ADD(NOW(), INTERVAL 27 DAY),
     'APPROVED', NULL, 'ITEMIZED', NOW(), NOW()),

    (23, 3, 2, NOW(),
     'Cảm ơn mọi người!', 34500000.00,
     'Phẫu thuật tim miễn phí cho trẻ em',
     NULL,
     'Hỗ trợ chi phí phẫu thuật tim bẩm sinh cho 10 trẻ em có hoàn cảnh khó khăn.',
     4, DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_ADD(NOW(), INTERVAL 80 DAY),
     'APPROVED', NULL, 'AUTHORIZED', NOW(), NOW()),

    (24, 3, NULL, NULL, NULL, 0.00,
     'Nước uống sạch cho trẻ mầm non',
     NULL,
     'Lắp đặt hệ thống lọc nước RO cho 5 trường mầm non tại huyện miền núi.',
     1, DATE_ADD(NOW(), INTERVAL 2 DAY), DATE_ADD(NOW(), INTERVAL 60 DAY),
     'PENDING_APPROVAL', NULL, 'ITEMIZED', NOW(), NOW()),

    (25, 3, NULL, NULL, NULL, 0.00,
     'Thiết bị y tế cho trạm xá thôn bản',
     NULL,
     'Cung cấp máy đo huyết áp, nhiệt kế, bông băng cho 20 trạm xá thôn bản.',
     4, DATE_ADD(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 45 DAY),
     'PENDING_APPROVAL', NULL, 'AUTHORIZED', NOW(), NOW()),

    (26, 3, NULL, NULL, NULL, 0.00,
     'Xây nhà tình nghĩa cho cụ già cô đơn',
     NULL,
     'Xây dựng mái ấm và chăm sóc cuộc sống cho 15 cụ già cô đơn tại tỉnh Hà Tĩnh.',
     1, NOW(), DATE_ADD(NOW(), INTERVAL 90 DAY),
     'DRAFT', NULL, 'ITEMIZED', NOW(), NOW()),

    (27, 3, 2, DATE_SUB(NOW(), INTERVAL 60 DAY),
     NULL, 9800000.00,
     'Vá lưới thuyền cho ngư dân nghèo',
     NULL,
     'Hỗ trợ chi phí vá lưới và sửa chữa thuyền cho 50 ngư dân nghèo ven biển.',
     2, DATE_SUB(NOW(), INTERVAL 60 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY),
     'APPROVED', NULL, 'ITEMIZED', NOW(), NOW()),

    (28, 3, 2, DATE_SUB(NOW(), INTERVAL 2 DAY),
     'Cảm ơn tấm lòng vàng!', 22000000.00,
     'Cứu hộ động vật hoang dã bị thương',
     NULL,
     'Chăm sóc và phục hồi các loài động vật hoang dã bị thương trước khi thả về tự nhiên.',
     6, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_ADD(NOW(), INTERVAL 28 DAY),
     'APPROVED', NULL, 'AUTHORIZED', NOW(), NOW()),

    (29, 3, 2, NULL,
     NULL, 0.00,
     'Xây khu nghỉ dưỡng từ thiện cao cấp',
     NULL,
     'Dự án nghỉ dưỡng cao cấp kết hợp từ thiện (không hợp lệ: mục đích kinh doanh).',
     1, DATE_ADD(NOW(), INTERVAL 30 DAY), DATE_ADD(NOW(), INTERVAL 180 DAY),
     'REJECTED', 'Mục đích chiến dịch không phù hợp với quy định từ thiện của nền tảng.',
     'ITEMIZED', NOW(), NOW()),

    (30, 3, 2, DATE_SUB(NOW(), INTERVAL 1 DAY),
     'Cảm ơn mọi người!', 5500000.00,
     'Chăn ấm mùa đông cho học sinh vùng cao',
     NULL,
     'Phát 300 chiếc chăn ấm và áo ấm cho học sinh tại các trường vùng cao Lào Cai.',
     3, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 20 DAY),
     'APPROVED', NULL, 'ITEMIZED', NOW(), NOW())
AS new ON DUPLICATE KEY UPDATE
    title            = new.title,
    description      = new.description,
    category_id      = new.category_id,
    status           = new.status,
    balance          = new.balance,
    type             = new.type,
    rejection_reason = new.rejection_reason,
    updated_at       = NOW();

-- =======================================
-- 2. FUNDRAISING GOALS (IDs 21-30)
-- =======================================
INSERT INTO fundraising_goals (id, campaign_id, target_amount, description, is_active, created_at, updated_at)
VALUES
    (21, 21, 30000000.00,  '20 chiếc xe lăn tiêu chuẩn + chi phí vận chuyển',         TRUE, NOW(), NOW()),
    (22, 22, 20000000.00,  '200 bộ sách giáo khoa + đồ dùng học tập',                 TRUE, NOW(), NOW()),
    (23, 23, 150000000.00, 'Phẫu thuật tim cho 10 trẻ em (25 triệu/ca)',               TRUE, NOW(), NOW()),
    (24, 24, 40000000.00,  '5 hệ thống lọc nước RO cho trường mầm non',              TRUE, NOW(), NOW()),
    (25, 25, 25000000.00,  '20 bộ thiết bị y tế cơ bản cho trạm xá thôn bản',         TRUE, NOW(), NOW()),
    (26, 26, 120000000.00, 'Xây 3 mái ấm nhỏ cho cụ già cô đơn',                     TRUE, NOW(), NOW()),
    (27, 27, 15000000.00,  'Vá lưới và sửa chữa thuyền cho 50 ngư dân',               TRUE, NOW(), NOW()),
    (28, 28, 50000000.00,  'Chi phí thức ăn, thuốc và chuồng trại cho động vật',      TRUE, NOW(), NOW()),
    (29, 29, 50000000.00,  '(Da bi tu choi) Khong ap dung',                            FALSE, NOW(), NOW()),
    (30, 30, 10000000.00,  '300 chiếc chăn ấm + 300 áo phao cho học sinh vùng cao',   TRUE, NOW(), NOW())
AS new ON DUPLICATE KEY UPDATE
    target_amount = new.target_amount,
    description   = new.description,
    is_active     = new.is_active,
    updated_at    = NOW();

-- =======================================
-- 3. CAMPAIGN FOLLOWS
-- =======================================
INSERT INTO campaign_follows (campaign_id, user_id, followed_at)
VALUES
    (21, 4, NOW()),
    (21, 5, NOW()),
    (22, 4, NOW()),
    (23, 4, NOW()),
    (23, 5, NOW()),
    (23, 6, NOW()),
    (28, 4, NOW()),
    (28, 5, NOW()),
    (30, 4, NOW())
ON DUPLICATE KEY UPDATE followed_at = NOW();

-- =======================================
-- 4. FEED POSTS (IDs 21-30)
-- =======================================
INSERT INTO feed_post (
    id, author_id, author_name, visibility,
    title, content, status,
    reply_count, view_count, like_count, comment_count,
    is_pinned, is_locked, created_at, updated_at,
    target_id, target_type, target_name, parent_post_id
) VALUES
    (21, 3, 'Fund Owner', 'PUBLIC',
     'Xe lăn – cập nhật tiến độ giao hàng',
     'Đã liên hệ nhà cung cấp, dự kiến tuần tới sẽ giao 10 chiếc xe lăn đầu tiên đến huyện Mường Khong, Điện Biên.',
     'PUBLISHED', 3, 45, 7, 3,
     FALSE, FALSE, DATE_SUB(NOW(), INTERVAL 3 DAY), NULL,
     21, 'CAMPAIGN', 'Xe lăn cho người khuyết tật vùng cao', NULL),

    (22, 3, 'Fund Owner', 'PUBLIC',
     'Phẫu thuật tim – cập nhật bệnh nhi thứ 3',
     'Bệnh nhi Nguyễn Văn A đã được phẫu thuật thành công ngày 05/04. Đang trong giai đoạn hồi phục tại Bệnh viện Tim Hà Nội. Cảm ơn các nhà hảo tâm!',
     'PUBLISHED', 8, 120, 15, 8,
     FALSE, FALSE, DATE_SUB(NOW(), INTERVAL 1 DAY), NULL,
     23, 'CAMPAIGN', 'Phẫu thuật tim miễn phí cho trẻ em', NULL),

    (23, 3, 'Fund Owner', 'PUBLIC',
     'Kế hoạch lắp đặt nước uống sạch – xin ý kiến',
     'Chúng tôi dự định lắp 5 hệ thống lọc nước tại 5 trường mầm non vùng cao. Mọi người có góp ý gì thêm không?',
     'PUBLISHED', 5, 30, 4, 5,
     FALSE, FALSE, DATE_SUB(NOW(), INTERVAL 2 DAY), NULL,
     24, 'CAMPAIGN', 'Nước uống sạch cho trẻ mầm non', NULL),

    (24, 3, 'Fund Owner', 'PUBLIC',
     'Kết thúc chiến dịch vá lưới thuyền – báo cáo tài chính',
     'Chiến dịch đã kết thúc! Tổng thu: 9,800,000đ. Đã chi: 9,500,000đ vá lưới + sửa thuyền cho 48/50 ngư dân. Số dư 300,000đ sẽ chuyển sang chiến dịch tiếp theo.',
     'PUBLISHED', 6, 85, 12, 6,
     FALSE, FALSE, DATE_SUB(NOW(), INTERVAL 5 DAY), NULL,
     27, 'CAMPAIGN', 'Vá lưới thuyền cho ngư dân nghèo', NULL),

    (25, 3, 'Fund Owner', 'PUBLIC',
     'Cứu hộ động vật – ca báo động đỏ tuần này',
     'Trung tâm vừa tiếp nhận 1 cá thể chim Cắt bị gãy cánh. Cần kinh phí phẫu thuật ước tính 2 triệu. Mọi người ủng hộ nhé!',
     'PUBLISHED', 2, 60, 9, 2,
     FALSE, FALSE, DATE_SUB(NOW(), INTERVAL 6 HOUR), NULL,
     28, 'CAMPAIGN', 'Cứu hộ động vật hoang dã bị thương', NULL),

    (26, 3, 'Fund Owner', 'PUBLIC',
     'Chăn ấm – đã phát 150/300 suất',
     'Đợt phát chăn ấm đầu tiên tại Lào Cai đã hoàn thành, 150 em nhận được chăn và áo phao. Đợt 2 sẽ tiếp tục vào tuần sau.',
     'PUBLISHED', 4, 55, 8, 4,
     FALSE, FALSE, DATE_SUB(NOW(), INTERVAL 8 HOUR), NULL,
     30, 'CAMPAIGN', 'Chăn ấm mùa đông cho học sinh vùng cao', NULL),

    (27, 3, 'Fund Owner', 'PUBLIC',
     'Chia sẻ: tại sao chiến dịch bị từ chối và bài học',
     'Chiến dịch xây khu nghỉ dưỡng của mình bị từ chối vì mục đích không phù hợp. Mình hiểu và sẽ điều chỉnh nội dung. Cảm ơn đội ngũ đã phản hồi chi tiết.',
     'PUBLISHED', 10, 95, 6, 10,
     FALSE, FALSE, DATE_SUB(NOW(), INTERVAL 10 DAY), NULL,
     29, 'CAMPAIGN', 'Xây khu nghỉ dưỡng từ thiện cao cấp', NULL),

    (28, 5, 'Alice Nguyen', 'PUBLIC',
     'Hướng dẫn cách viết mô tả chiến dịch thu hút',
     'Nhiều bạn hỏi mình cách viết mô tả chiến dịch cho tốt. Theo kinh nghiệm: 1) Viết ngắn gọn 2) Dùng hình ảnh thực tế 3) Nêu rõ mục tiêu cụ thể 4) Cập nhật thường xuyên.',
     'PUBLISHED', 7, 110, 20, 7,
     FALSE, FALSE, DATE_SUB(NOW(), INTERVAL 4 DAY), NULL,
     NULL, NULL, NULL, NULL),

    (29, 6, 'Bob Tran', 'PUBLIC',
     'Câu hỏi: tiền quyên góp có bảo mật không?',
     'Mình muốn quyên góp nhưng lo ngại thông tin tài khoản bị lộ. Hệ thống TrustFundME có bảo mật thông tin người quyên góp không?',
     'PUBLISHED', 3, 40, 2, 3,
     FALSE, FALSE, DATE_SUB(NOW(), INTERVAL 1 DAY), NULL,
     NULL, NULL, NULL, NULL),

    (30, 3, 'Fund Owner', 'PUBLIC',
     'Nháp – dự thảo cập nhật sách giáo khoa',
     'Nội dung đang soạn, hình ảnh chụp chưa đủ. Sẽ đăng sau khi hoàn thiện.',
     'DRAFT', 0, 0, 0, 0,
     FALSE, FALSE, NOW(), NULL,
     NULL, NULL, NULL, NULL)
AS new ON DUPLICATE KEY UPDATE
    title         = new.title,
    content       = new.content,
    author_name   = new.author_name,
    status        = new.status,
    reply_count   = new.reply_count,
    view_count    = new.view_count,
    like_count    = new.like_count,
    comment_count = new.comment_count,
    target_id     = new.target_id,
    target_type   = new.target_type,
    target_name   = new.target_name,
    updated_at    = NOW();

-- =======================================
-- 5. FLAGS (Báo cáo vi phạm) (IDs 1-10)
-- =======================================
INSERT INTO flags (
    flag_id, post_id, campaign_id, user_id,
    reviewed_by, reason, status, created_at
) VALUES
    (1,  28, NULL, 4, NULL,
     'Bài viết có thể chứa thông tin không chính xác về cách gây quỹ hiệu quả. Mong được kiểm chứng.',
     'PENDING', DATE_SUB(NOW(), INTERVAL 2 DAY)),

    (2,  29, NULL, 5, NULL,
     'Lo ngại về vấn đề bảo mật thông tin. Cần được giải đáp rõ ràng từ đội ngũ kỹ thuật.',
     'PENDING', DATE_SUB(NOW(), INTERVAL 1 DAY)),

    (3,  NULL, 27, 4, NULL,
     'Chiến dịch có dấu hiệu không minh bạch: số tiền chi không khớp với báo cáo.',
     'PENDING', DATE_SUB(NOW(), INTERVAL 6 HOUR)),

    (4,  23, NULL, 6, NULL,
     'Nội dung kế hoạch chưa rõ ràng, có thể gây hiểu lầm cho người ủng hộ.',
     'PENDING', NOW()),

    (5,  21, NULL, 4, 2,
     'Báo cáo sai tiến độ giao hàng.',
     'RESOLVED', DATE_SUB(NOW(), INTERVAL 5 DAY)),

    (6,  22, NULL, 6, 2,
     'Tên bệnh nhi không nên công khai theo quy định bảo mật.',
     'RESOLVED', DATE_SUB(NOW(), INTERVAL 3 DAY)),

    (7,  NULL, 29, 5, 2,
     'Chiến dịch vi phạm quy định sử dụng nền tảng.',
     'RESOLVED', DATE_SUB(NOW(), INTERVAL 8 DAY)),

    (8,  24, NULL, 6, 2,
     'Cho rằng báo cáo tài chính không đầy đủ.',
     'DISMISSED', DATE_SUB(NOW(), INTERVAL 7 DAY)),

    (9,  25, NULL, 4, 2,
     'Bài viết có hành vi lợi dụng lòng thương xót để quyên góp.',
     'DISMISSED', DATE_SUB(NOW(), INTERVAL 4 DAY)),

    (10, NULL, 27, 5, 2,
     'Nghi ngờ có gian lận trong chiến dịch.',
     'DISMISSED', DATE_SUB(NOW(), INTERVAL 6 DAY))
AS new ON DUPLICATE KEY UPDATE
    reason      = new.reason,
    status      = new.status,
    reviewed_by = new.reviewed_by;

-- =======================================
-- Xác nhận kết quả
-- =======================================
SELECT '=== CAMPAIGNS ===' AS info;
SELECT id, title, category_id, status, balance, type FROM campaigns WHERE id BETWEEN 21 AND 30 ORDER BY id;

SELECT '=== FEED POSTS ===' AS info;
SELECT id, author_id, status, target_type, target_id, LEFT(title, 40) AS title_preview FROM feed_post WHERE id BETWEEN 21 AND 30 ORDER BY id;

SELECT '=== FLAGS ===' AS info;
SELECT flag_id, post_id, campaign_id, user_id, status, LEFT(reason, 50) AS reason_preview FROM flags ORDER BY flag_id;

SELECT '=== FUNDRAISING GOALS ===' AS info;
SELECT fg.id, fg.campaign_id, c.title, fg.target_amount, fg.is_active
FROM fundraising_goals fg
JOIN campaigns c ON fg.campaign_id = c.id
WHERE fg.id BETWEEN 21 AND 30
ORDER BY fg.id;
