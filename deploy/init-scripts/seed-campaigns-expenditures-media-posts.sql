-- ================================================================
-- Tạo dữ liệu mẫu: campaigns + expenditures + media + feed posts
-- Database: trustfundme_campaign_db / trustfundme_media_db
-- Tránh trùng ID: campaigns 31-35, expenditures 31-35,
--                  media 31-40, feed_post 31-40
-- Users: 1=admin, 2=staff1, 3=fund_owner, 4=user, 5=alice, 6=bob
-- ================================================================

USE trustfundme_campaign_db;

-- ─── 0. THÊM CỘT feed_post (chạy trước ALTER TABLE, rồi mới INSERT) ──
ALTER TABLE feed_post
    ADD COLUMN IF NOT EXISTS author_name VARCHAR(255) NULL COMMENT 'Cached author full name for display',
    ADD COLUMN IF NOT EXISTS parent_post_id BIGINT NULL,
    ADD COLUMN IF NOT EXISTS is_locked BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS target_id BIGINT NULL COMMENT 'ID of linked entity (campaign or expenditure)',
    ADD COLUMN IF NOT EXISTS target_type VARCHAR(50) NULL COMMENT 'EXPENDITURE or CAMPAIGN',
    ADD COLUMN IF NOT EXISTS target_name VARCHAR(255) NULL COMMENT 'Cached name of linked entity';

-- ─── 1. CAMPAIGNS (IDs 31-35) ───────────────────────────────────
INSERT INTO campaigns (
    id, fund_owner_id, approved_by_staff, approved_at,
    thank_message, balance, title, cover_image, description,
    category_id, start_date, end_date, status, type,
    created_at, updated_at
) VALUES
    (31, 3, 2, DATE_SUB(NOW(), INTERVAL 4 DAY),
     'Cảm ơn các bạn đã đồng hành!', 12500000.00,
     'Chuyến xe cứu trợ Quảng Bình', NULL,
     'Mua thực phẩm, nhu yếu phẩm và vật liệu xây nhà tạm cho 50 hộ dân vùng lũ Quảng Bình.',
     4, DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_ADD(NOW(), INTERVAL 30 DAY),
     'APPROVED', 'ITEMIZED', NOW(), NOW()),

    (32, 3, 2, DATE_SUB(NOW(), INTERVAL 2 DAY),
     'Một lần nữa cảm ơn tấm lòng!', 7800000.00,
     'Bộ kit y tế cho trạm xã vùng cao', NULL,
     'Cung cấp 20 bộ kit y tế cơ bản (bông băng, thuốc RED, nhiệt kế, máy đo huyết áp) cho 20 trạm xá thôn bản.',
     4, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_ADD(NOW(), INTERVAL 25 DAY),
     'APPROVED', 'ITEMIZED', NOW(), NOW()),

    (33, 3, NULL, NULL, NULL, 0.00,
     'Gạo từ thiện cho hộ nghèo ven biển', NULL,
     'Phát gạo và thực phẩm cơ bản cho 100 hộ nghèo tại các xã ven biển tỉnh Thanh Hóa.',
     1, DATE_ADD(NOW(), INTERVAL 5 DAY), DATE_ADD(NOW(), INTERVAL 40 DAY),
     'PENDING_APPROVAL', 'ITEMIZED', NOW(), NOW()),

    (34, 3, 2, DATE_SUB(NOW(), INTERVAL 7 DAY),
     'Cảm ơn mọi người!', 9400000.00,
     'Dụng cụ học tập cho học sinh vùng cao', NULL,
     'Mua 300 bộ sách giáo khoa, vở, bút và ba lô cho học sinh tiểu học tại huyện Mường Khong, Điện Biên.',
     3, DATE_SUB(NOW(), INTERVAL 7 DAY), DATE_ADD(NOW(), INTERVAL 15 DAY),
     'APPROVED', 'ITEMIZED', NOW(), NOW()),

    (35, 3, 2, DATE_SUB(NOW(), INTERVAL 1 DAY),
     'Cảm ơn tấm lòng vàng!', 3500000.00,
     'Nước sạch cho trẻ em miền Tây', NULL,
     'Lắp đặt 3 máy lọc nước RO tại trường học và trạm y tế xã vùng núi Cần Thơ.',
     4, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 20 DAY),
     'APPROVED', 'AUTHORIZED', NOW(), NOW())
ON DUPLICATE KEY UPDATE
    title       = VALUES(title),
    description = VALUES(description),
    category_id = VALUES(category_id),
    status      = VALUES(status),
    balance     = VALUES(balance),
    type        = VALUES(type),
    updated_at  = NOW();

-- ─── 2. FUNDRAISING GOALS (IDs 31-35) ───────────────────────────
INSERT INTO fundraising_goals (id, campaign_id, target_amount, description, is_active, created_at, updated_at)
VALUES
    (31, 31, 20000000.00, 'Thực phẩm + vật liệu cho 50 hộ dân vùng lũ Quảng Bình',      TRUE, NOW(), NOW()),
    (32, 32, 15000000.00, '20 bộ kit y tế đầy đủ cho trạm xá thôn bản',                  TRUE, NOW(), NOW()),
    (33, 33, 12000000.00, 'Gạo + thực phẩm cho 100 hộ nghèo ven biển Thanh Hóa',        TRUE, NOW(), NOW()),
    (34, 34, 10000000.00, '300 bộ dụng cụ học tập cho học sinh vùng cao Điện Biên',     TRUE, NOW(), NOW()),
    (35, 35,  8000000.00, '3 máy lọc nước RO cho trường học và trạm y tế xã Cần Thơ',   TRUE, NOW(), NOW())
ON DUPLICATE KEY UPDATE
    target_amount = VALUES(target_amount),
    description   = VALUES(description),
    is_active     = VALUES(is_active),
    updated_at    = NOW();

-- ─── 3. EXPENDITURES (IDs 31-35) ────────────────────────────────
INSERT INTO expenditures (
    id, campaign_id, evidence_due_at, evidence_status,
    total_amount, total_expected_amount, variance,
    is_withdrawal_requested, plan, status,
    staff_review_id, created_at, updated_at
) VALUES
    (31, 31, DATE_ADD(NOW(), INTERVAL 7 DAY), 'PENDING',
     8500000.00, 8500000.00, 0.00, FALSE,
     'Đợt 1: Mua nhu yếu phẩm (gạo 500kg, mì gói 200 thùng, nước đóng chai 100 thùng) giao cho huyện Lệ Thủy, Quảng Bình.',
     'APPROVED', 2, NOW(), NOW()),

    (32, 31, DATE_ADD(NOW(), INTERVAL 5 DAY), 'PENDING',
     12000000.00, 12000000.00, 0.00, TRUE,
     'Đợt 2: Mua xi măng, gạch, tôn để xây 10 căn nhà tạm cho hộ dân vùng lũ.',
     'PENDING', NULL, NOW(), NOW()),

    (33, 32, DATE_ADD(NOW(), INTERVAL 3 DAY), 'PENDING',
     6000000.00, 6000000.00, 0.00, FALSE,
     'Mua 20 bộ kit y tế cơ bản (bông băng, thuốc RED, nhiệt kế, máy đo huyết áp) cho 20 trạm xã.',
     'APPROVED', 2, NOW(), NOW()),

    (34, 34, DATE_ADD(NOW(), INTERVAL 10 DAY), 'PENDING',
     5000000.00, 5000000.00, 0.00, FALSE,
     'Mua 200 bộ sách giáo khoa lớp 1-5, 200 cuốn vở, 200 cây bút, 100 ba lô.',
     'APPROVED', 2, NOW(), NOW()),

    (35, 35, DATE_ADD(NOW(), INTERVAL 2 DAY), 'PENDING',
     3500000.00, 3500000.00, 0.00, FALSE,
     'Mua và lắp đặt 3 máy lọc nước RO tại trường mầm non và trạm y tế xã.',
     'APPROVED', 2, NOW(), NOW())
ON DUPLICATE KEY UPDATE
    total_amount = VALUES(total_amount),
    plan         = VALUES(plan),
    status       = VALUES(status),
    updated_at   = NOW();

-- ─── 4. FEED POSTS (IDs 31-40) ──────────────────────────────────
-- 31-34: DISCUSSION (target NULL) — ảnh: cc/ee/96...
-- 35-37: CAMPAIGN_UPDATE target CAMPAIGN — ảnh: 0b/cb/07...
-- 38-40: CAMPAIGN_UPDATE target EXPENDITURE — ảnh: a1/7c/fb...

INSERT INTO feed_post (
    id, author_id, author_name, visibility,
    title, content, status,
    reply_count, view_count, like_count, comment_count,
    is_pinned, is_locked, created_at, updated_at,
    target_id, target_type, target_name, parent_post_id
) VALUES
    (31, 4, 'Normal User', 'PUBLIC',
     'Kinh nghiệm gây quỹ hiệu quả cho người mới bắt đầu',
     'Mình mới làm Fund Owner được 2 tháng, chia sẻ vài kinh nghiệm: viết tiêu đề ngắn gọn, dùng ảnh thật, cập nhật tiến độ thường xuyên để giữ lòng tin người ủng hộ. Mọi người có tips gì thêm không?',
     'PUBLISHED', 5, 75, 12, 5, FALSE, FALSE,
     DATE_SUB(NOW(), INTERVAL 3 DAY), NULL,
     NULL, NULL, NULL, NULL),

    (32, 5, 'Alice Nguyen', 'PUBLIC',
     'Hỏi về quy trình duyệt chiến dịch mất bao lâu?',
     'Chiến dịch mới nộp đã 3 ngày nhưng chưa thấy duyệt. Cho mình hỏi thời gian xử lý trung bình là bao lâu? Có cần bổ sung giấy tờ gì không?',
     'PUBLISHED', 8, 110, 3, 8, FALSE, FALSE,
     DATE_SUB(NOW(), INTERVAL 2 DAY), NULL,
     NULL, NULL, NULL, NULL),

    (33, 6, 'Bob Tran', 'PUBLIC',
     'Chia sẻ cảm xúc sau khi nhận được hỗ trợ từ chiến dịch',
     'Gia đình mình vừa nhận được gạo và nhu yếu phẩm từ chiến dịch cứu trợ. Lần đầu tiên nhận được sự hỗ trợ từ cộng đồng, lòng mình rất xúc động. Cảm ơn tất cả các nhà hảo tâm!',
     'PUBLISHED', 15, 200, 25, 15, FALSE, FALSE,
     DATE_SUB(NOW(), INTERVAL 1 DAY), NULL,
     NULL, NULL, NULL, NULL),

    (34, 4, 'Normal User', 'PUBLIC',
     'Nên chọn chiến dịch nào để ủng hộ cuối năm?',
     'Mình đang có ngân sách 2 triệu muốn đóng góp vào dịp cuối năm. Các bạn gợi ý chiến dịch nào đáng tin cậy, có tiến độ rõ ràng và minh bạch?',
     'PUBLISHED', 3, 45, 1, 3, FALSE, FALSE,
     DATE_SUB(NOW(), INTERVAL 6 HOUR), NULL,
     NULL, NULL, NULL, NULL),

    (35, 3, 'Fund Owner', 'PUBLIC',
     'Cập nhật chuyến xe cứu trợ Quảng Bình – đợt 1 đã giao',
     'Đợt 1 đã hoàn thành! Đã giao 500kg gạo, 200 thùng mì gói, 100 thùng nước đóng chai đến huyện Lệ Thủy. 30/50 hộ dân đã nhận đủ nhu yếu phẩm. Tuần sau sẽ giao đợt 2.',
     'PUBLISHED', 6, 95, 18, 6, FALSE, FALSE,
     DATE_SUB(NOW(), INTERVAL 1 DAY), NULL,
     31, 'CAMPAIGN', 'Chuyến xe cứu trợ Quảng Bình', NULL),

    (36, 3, 'Fund Owner', 'PUBLIC',
     'Bộ kit y tế – đã giao đợt 1 cho 10 trạm xã',
     'Đã giao 10/20 bộ kit y tế cho 10 trạm xá thôn bản. Mỗi bộ gồm bông băng, thuốc RED, nhiệt kế, máy đo huyết áp cơ bản. Đợt 2 (10 bộ còn lại) sẽ giao tuần sau.',
     'PUBLISHED', 3, 60, 10, 3, FALSE, FALSE,
     DATE_SUB(NOW(), INTERVAL 2 DAY), NULL,
     32, 'CAMPAIGN', 'Bộ kit y tế cho trạm xã vùng cao', NULL),

    (37, 3, 'Fund Owner', 'PUBLIC',
     'Dụng cụ học tập – đã phát 200/300 bộ cho học sinh',
     'Đợt phát đầu tiên đã hoàn thành tại xã Mường Khong. 200 em nhận được sách giáo khoa, vở, bút và ba lô mới. 100 bộ còn lại sẽ phát vào tuần sau tại các xã lân cận.',
     'PUBLISHED', 2, 80, 14, 2, FALSE, FALSE,
     DATE_SUB(NOW(), INTERVAL 12 HOUR), NULL,
     34, 'CAMPAIGN', 'Dụng cụ học tập cho học sinh vùng cao', NULL),

    (38, 3, 'Fund Owner', 'PUBLIC',
     'Chi mua nhu yếu phẩm đợt 1 – đã hoàn tất, giao cho huyện Lệ Thủy',
     'Đã chi 8,500,000đ mua nhu yếu phẩm đợt 1: 500kg gạo, 200 thùng mì gói, 100 thùng nước đóng chai. Đã giao đủ cho 30 hộ dân đầu tiên tại huyện Lệ Thủy, Quảng Bình. Biên nhận và hình ảnh đính kèm.',
     'PUBLISHED', 4, 70, 9, 4, FALSE, FALSE,
     DATE_SUB(NOW(), INTERVAL 1 HOUR), NULL,
     31, 'EXPENDITURE', 'Chi mua nhu yếu phẩm đợt 1 cho huyện Lệ Thủy', NULL),

    (39, 3, 'Fund Owner', 'PUBLIC',
     'Chi mua kit y tế đợt 1 – đã giao 10 bộ cho 10 trạm xã',
     'Đã chi 6,000,000đ mua 10/20 bộ kit y tế. Mỗi bộ gồm: bông băng y tế 5 cuộn, thuốc RED 10 vỉ, nhiệt kế thủy ngân 10 cái, máy đo huyết áp cơ bản 10 cái. Đã phát cho 10 trạm xá thôn bản vùng cao.',
     'PUBLISHED', 2, 50, 7, 2, FALSE, FALSE,
     DATE_SUB(NOW(), INTERVAL 2 HOUR), NULL,
     33, 'EXPENDITURE', 'Chi mua kit y tế đợt 1 cho 20 trạm xã', NULL),

    (40, 3, 'Fund Owner', 'PUBLIC',
     'Chi lắp đặt máy lọc nước RO – đã hoàn tất 3 vị trí',
     'Đã chi 3,500,000đ mua và lắp đặt 3 máy lọc nước RO tại 3 điểm: Trường Mầm non X, Trạm Y tế Y, Trường Tiểu học Z. Tất cả đã chạy thử thành công, chất lượng nước đạt chuẩn.',
     'PUBLISHED', 3, 55, 11, 3, FALSE, FALSE,
     NOW(), NULL,
     35, 'EXPENDITURE', 'Chi lắp đặt máy lọc nước RO đợt 1 cho 3 điểm', NULL)
ON DUPLICATE KEY UPDATE
    title         = VALUES(title),
    content       = VALUES(content),
    author_name   = VALUES(author_name),
    status        = VALUES(status),
    reply_count   = VALUES(reply_count),
    view_count    = VALUES(view_count),
    like_count    = VALUES(like_count),
    comment_count = VALUES(comment_count),
    target_id     = VALUES(target_id),
    target_type   = VALUES(target_type),
    target_name   = VALUES(target_name),
    updated_at    = NOW();

-- ─── 5. MEDIA (IDs 31-40) ───────────────────────────────────────
USE trustfundme_media_db;

INSERT INTO media (id, post_id, media_type, url, status, created_at)
VALUES
    (31, 31, 'IMAGE', 'https://i.pinimg.com/474x/cc/ee/96/ccee969f87fa8e506fd5a03d0a9c2c26.jpg', 'APPROVED', NOW()),
    (32, 32, 'IMAGE', 'https://i.pinimg.com/474x/cc/ee/96/ccee969f87fa8e506fd5a03d0a9c2c26.jpg', 'APPROVED', NOW()),
    (33, 33, 'IMAGE', 'https://i.pinimg.com/474x/cc/ee/96/ccee969f87fa8e506fd5a03d0a9c2c26.jpg', 'APPROVED', NOW()),
    (34, 34, 'IMAGE', 'https://i.pinimg.com/474x/cc/ee/96/ccee969f87fa8e506fd5a03d0a9c2c26.jpg', 'APPROVED', NOW()),
    (35, 35, 'IMAGE', 'https://i.pinimg.com/736x/0b/cb/07/0bcb077e40a779547f9274c0a747b682.jpg', 'APPROVED', NOW()),
    (36, 36, 'IMAGE', 'https://i.pinimg.com/736x/0b/cb/07/0bcb077e40a779547f9274c0a747b682.jpg', 'APPROVED', NOW()),
    (37, 37, 'IMAGE', 'https://i.pinimg.com/736x/0b/cb/07/0bcb077e40a779547f9274c0a747b682.jpg', 'APPROVED', NOW()),
    (38, 38, 'IMAGE', 'https://i.pinimg.com/1200x/a1/7c/fb/a17cfb5ab31a48fff98bcdaf4dd0e70c.jpg', 'APPROVED', NOW()),
    (39, 39, 'IMAGE', 'https://i.pinimg.com/1200x/a1/7c/fb/a17cfb5ab31a48fff98bcdaf4dd0e70c.jpg', 'APPROVED', NOW()),
    (40, 40, 'IMAGE', 'https://i.pinimg.com/1200x/a1/7c/fb/a17cfb5ab31a48fff98bcdaf4dd0e70c.jpg', 'APPROVED', NOW())
ON DUPLICATE KEY UPDATE url = VALUES(url);

-- ─── 6. XÁC NHẬN ────────────────────────────────────────────────
USE trustfundme_campaign_db;
SELECT '=== CAMPAIGNS ===' AS info;
SELECT id, title, status, balance FROM campaigns WHERE id BETWEEN 31 AND 35 ORDER BY id;

SELECT '=== EXPENDITURES ===' AS info;
SELECT id, campaign_id, status, total_amount, LEFT(plan, 50) AS plan_preview
FROM expenditures WHERE id BETWEEN 31 AND 35 ORDER BY id;

SELECT '=== FEED POSTS ===' AS info;
SELECT id, author_id, status, target_type, target_id, LEFT(title, 50) AS title_preview
FROM feed_post WHERE id BETWEEN 31 AND 40 ORDER BY id;

USE trustfundme_media_db;
SELECT '=== MEDIA ===' AS info;
SELECT id, post_id, media_type, LEFT(url, 60) AS url_preview
FROM media WHERE id BETWEEN 31 AND 40 ORDER BY id;

-- ─── 7. COMMENTS (IDs 41-60) ─────────────────────────────────────
USE trustfundme_campaign_db;

-- Bảng feed_post_comment: id, post_id, user_id, parent_comment_id, content, like_count, created_at, updated_at
-- Không có author_name (cache từ identity-service), không có status
INSERT INTO feed_post_comment (
    id, post_id, user_id, parent_comment_id,
    content, like_count, created_at, updated_at
) VALUES
    -- User 4 bình luận trên campaign post 35 (campaign 31 - Quảng Bình)
    (41, 35, 4, NULL,
     'Bài viết rất chi tiết! Mình ủng hộ chiến dịch này, mong sớm giao đợt 2.',
     0, DATE_SUB(NOW(), INTERVAL 20 HOUR), NULL),

    -- User 3 trả lời user 4 trên post 35
    (42, 35, 3, 41,
     'Cảm ơn Normal User! Đợt 2 đang chuẩn bị hàng, dự kiến tuần sau sẽ giao. Cập nhật sớm nhất cho các bạn!',
     0, DATE_SUB(NOW(), INTERVAL 18 HOUR), NULL),

    -- User 4 bình luận trên campaign post 36 (campaign 32 - kit y tế)
    (43, 36, 4, NULL,
     'Mỗi bộ kit giá bao nhiêu vậy? Có ghi chú giá cụ thể không?',
     0, DATE_SUB(NOW(), INTERVAL 15 HOUR), NULL),

    -- User 3 trả lời user 4 trên post 36
    (44, 36, 3, 43,
     'Mỗi bộ kit 600,000đ gồm đầy đủ bông băng, thuốc RED, nhiệt kế, máy đo huyết áp. Minh bạch từng đồng nhé!',
     0, DATE_SUB(NOW(), INTERVAL 14 HOUR), NULL),

    -- User 4 bình luận trên campaign post 37 (campaign 34 - dụng cụ học tập)
    (45, 37, 4, NULL,
     '200 em đã nhận rồi, 100 em còn lại tuần sau có đủ hàng không ạ?',
     0, DATE_SUB(NOW(), INTERVAL 10 HOUR), NULL),

    -- User 3 trả lời user 4 trên post 37
    (46, 37, 3, 45,
     'Đã đặt đủ 100 bộ còn lại, tuần sau sẽ phát đầy đủ. Hình ảnh và danh sách em nhận sẽ được cập nhật!',
     0, DATE_SUB(NOW(), INTERVAL 9 HOUR), NULL),

    -- User 4 bình luận trên expenditure post 38 (expenditure 31)
    (47, 38, 4, NULL,
     'Biên nhận rõ ràng, 8.5 triệu cho 30 hộ là hợp lý. Hy vọng 20 hộ còn lại sẽ nhận đủ trong đợt 2.',
     0, DATE_SUB(NOW(), INTERVAL 30 HOUR), NULL),

    -- User 3 trả lời user 4 trên post 38
    (48, 38, 3, 47,
     'Cảm ơn Normal User đã theo dõi! Đợt 2 đang chờ duyệt chi, sẽ giao đủ 20 hộ còn lại ngay khi được phê duyệt.',
     0, DATE_SUB(NOW(), INTERVAL 28 HOUR), NULL),

    -- User 4 bình luận trên expenditure post 40 (expenditure 35)
    (49, 40, 4, NULL,
     'Máy lọc nước RO lắp ở trường mầm non là ý tưởng hay! Trẻ em được dùng nước sạch.',
     0, DATE_SUB(NOW(), INTERVAL 5 HOUR), NULL),

    -- User 3 trả lời user 4 trên post 40
    (50, 40, 3, 49,
     'Đúng rồi, mình ưu tiên trường mầm non và trạm y tế. 3 máy này phục vụ khoảng 500 người mỗi ngày.',
     0, DATE_SUB(NOW(), INTERVAL 4 HOUR), NULL),

    -- User 3 bình luận trên discussion post 31 (user 4 viết)
    (51, 31, 3, NULL,
     'Tips rất hữu ích, nhất là phần cập nhật tiến độ thường xuyên. Mình cũng thường làm vậy để giữ lòng tin.',
     0, DATE_SUB(NOW(), INTERVAL 22 HOUR), NULL),

    -- User 4 trả lời user 3 trên post 31
    (52, 31, 4, 51,
     'Cảm ơn Fund Owner! Bạn có mẹo nào về cách viết mô tả chiến dịch thu hút không?',
     0, DATE_SUB(NOW(), INTERVAL 20 HOUR), NULL),

    -- User 3 trả lời user 4 trên post 31
    (53, 31, 3, 52,
     'Mình thường dùng cấu trúc: vấn đề → giải pháp → tác động. Kèm hình ảnh thực tế, tránh ngôn ngữ mơ hồ.',
     0, DATE_SUB(NOW(), INTERVAL 18 HOUR), NULL),

    -- User 3 bình luận trên discussion post 34 (user 4 viết - tự hỏi)
    (54, 34, 3, NULL,
     'Mình gợi ý campaign 31 (Quảng Bình) và 34 (dụng cụ học tập) — cả hai đều có tiến độ rõ ràng và minh bạch chi tiêu.',
     0, DATE_SUB(NOW(), INTERVAL 5 HOUR), NULL),

    -- User 3 bình luận trên discussion post 33 (user 6 viết)
    (55, 33, 3, NULL,
     'Thật sự cảm ơn Bob Tran đã chia sẻ. Là Fund Owner, mình thấy việc minh bạch chi tiêu là cách tốt nhất để giữ lòng tin.',
     0, DATE_SUB(NOW(), INTERVAL 8 HOUR), NULL),

    -- User 4 bình luận thêm trên campaign post 35 (campaign 31)
    (56, 35, 4, NULL,
     'Hình ảnh giao hàng ở đâu vậy bạn? Mình muốn xem thực tế hàng đã đến tay người dân chưa.',
     0, DATE_SUB(NOW(), INTERVAL 12 HOUR), NULL)
ON DUPLICATE KEY UPDATE
    content      = VALUES(content),
    parent_comment_id = VALUES(parent_comment_id),
    updated_at   = NOW();

-- Cập nhật reply_count / comment_count cho các post liên quan
UPDATE feed_post SET reply_count = reply_count + 1, comment_count = comment_count + 1 WHERE id = 35;
UPDATE feed_post SET reply_count = reply_count + 1, comment_count = comment_count + 1 WHERE id = 36;
UPDATE feed_post SET reply_count = reply_count + 1, comment_count = comment_count + 1 WHERE id = 37;
UPDATE feed_post SET reply_count = reply_count + 1, comment_count = comment_count + 1 WHERE id = 38;
UPDATE feed_post SET reply_count = reply_count + 1, comment_count = comment_count + 1 WHERE id = 40;
UPDATE feed_post SET reply_count = reply_count + 2, comment_count = comment_count + 2 WHERE id = 31;
UPDATE feed_post SET reply_count = reply_count + 1, comment_count = comment_count + 1 WHERE id = 34;
UPDATE feed_post SET reply_count = reply_count + 1, comment_count = comment_count + 1 WHERE id = 33;

-- ─── 8. XÁC NHẬN COMMENTS ─────────────────────────────────────────
SELECT '=== COMMENTS ===' AS info;
SELECT c.id, c.post_id, c.user_id, c.parent_comment_id, LEFT(c.content, 60) AS content_preview, c.like_count, c.created_at
FROM feed_post_comment c WHERE c.id BETWEEN 41 AND 56 ORDER BY c.id;