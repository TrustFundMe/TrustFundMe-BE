-- =======================================
-- Extra sample campaigns (optional QA load)
-- Requires: schema trustfundme_campaign_db (campaigns.category_id -> campaign_categories.id)
-- Safe to re-run: ON DUPLICATE KEY UPDATE.
-- IDs 11–20 avoid collision with init campaigns 1–7 and 10.
-- fund_owner_id = 3 (Fund Owner from identity seed).
-- If you only have empty tables: categories are upserted below so FK 1452 does not occur.
-- =======================================

USE trustfundme_campaign_db;

-- Must exist before campaigns (FK category_id). Mirrors init-all-databases.sql.
INSERT INTO campaign_categories (id, name, description, created_at, updated_at)
VALUES
    (1, 'Nhân đạo', 'Cứu trợ nhân đạo và hỗ trợ khẩn cấp', NOW(), NOW()),
    (2, 'Nông nghiệp', 'Phát triển nông nghiệp và hỗ trợ nông dân', NOW(), NOW()),
    (3, 'Giáo dục', 'Giáo dục, trường học và học bổng', NOW(), NOW()),
    (4, 'Y tế', 'Y tế, chữa bệnh và hỗ trợ bệnh nhi', NOW(), NOW()),
    (5, 'Môi trường', 'Bảo vệ môi trường và tái tạo rừng', NOW(), NOW()),
    (6, 'Động vật', 'Cứu hộ và chăm sóc động vật', NOW(), NOW())
ON DUPLICATE KEY UPDATE name = VALUES(name), description = VALUES(description);

INSERT INTO campaigns (
    id,
    fund_owner_id,
    approved_by_staff,
    approved_at,
    thank_message,
    balance,
    title,
    cover_image,
    description,
    category_id,
    start_date,
    end_date,
    status,
    rejection_reason,
    type,
    created_at,
    updated_at
) VALUES
    (11, 3, 2, NOW(), 'Cảm ơn bạn đã ủng hộ!', 12500000.00,
     'Nước sạch cho trẻ em vùng núi', NULL,
     'Raise funds for clean water in rural schools.', 3,
     DATE_SUB(NOW(), INTERVAL 7 DAY), DATE_ADD(NOW(), INTERVAL 23 DAY), 'APPROVED', NULL, 'ITEMIZED', NOW(), NOW()),
    (12, 3, 2, NOW(), 'Thank you for your support!', 8200000.00,
     'Bộ kit y tế cho trạm xã', NULL,
     'Provide basic medical supplies to remote health posts.', 4,
     DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_ADD(NOW(), INTERVAL 57 DAY), 'APPROVED', NULL, 'AUTHORIZED', NOW(), NOW()),
    (13, 3, NULL, NULL, NULL, 0.00,
     'Nhà tạm cho nạn nhân lũ', NULL,
     'Build temporary shelters after floods.', 1,
     NOW(), DATE_ADD(NOW(), INTERVAL 45 DAY), 'DRAFT', NULL, 'ITEMIZED', NOW(), NOW()),
    (14, 3, 2, DATE_SUB(NOW(), INTERVAL 2 DAY), 'We appreciate your help!', 31000000.00,
     'Thư viện nhỏ cho học sinh vùng cao', NULL,
     'Books and furniture for village libraries.', 3,
     DATE_SUB(NOW(), INTERVAL 14 DAY), DATE_ADD(NOW(), INTERVAL 16 DAY), 'APPROVED', NULL, 'ITEMIZED', NOW(), NOW()),
    (15, 3, NULL, NULL, NULL, 0.00,
     'Sửa bếp cộng đồng', NULL,
     'Repair and equip community kitchens.', 1,
     DATE_ADD(NOW(), INTERVAL 5 DAY), DATE_ADD(NOW(), INTERVAL 65 DAY), 'PENDING_APPROVAL', NULL, 'ITEMIZED', NOW(), NOW()),
    (16, 3, 2, DATE_SUB(NOW(), INTERVAL 30 DAY), NULL, 5600000.00,
     'Sữa học đường (đã kết thúc giai đoạn)', NULL,
     'Daily milk program for primary schools.', 3,
     DATE_SUB(NOW(), INTERVAL 90 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), 'APPROVED', NULL, 'ITEMIZED', NOW(), NOW()),
    (17, 3, 2, NOW(), 'Thank you!', 4200000.00,
     'Gói dinh dưỡng cho bà mẹ', NULL,
     'Micronutrient packs for pregnant women.', 4,
     DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_ADD(NOW(), INTERVAL 20 DAY), 'APPROVED', NULL, 'AUTHORIZED', NOW(), NOW()),
    (18, 3, 2, NOW(), 'Every donation counts.', 18900000.00,
     'Túi lương thực cứu lũ', NULL,
     'Emergency food for families affected by floods.', 1,
     DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_ADD(NOW(), INTERVAL 25 DAY), 'APPROVED', NULL, 'ITEMIZED', NOW(), NOW()),
    (19, 3, NULL, NULL, NULL, 0.00,
     'Đèn năng lượng mặt trời cho thôn', NULL,
     'Install solar street lights in off-grid villages.', 5,
     NOW(), DATE_ADD(NOW(), INTERVAL 90 DAY), 'DRAFT', NULL, 'ITEMIZED', NOW(), NOW()),
    (20, 3, 2, DATE_SUB(NOW(), INTERVAL 1 DAY), 'Thanks!', 7500000.00,
     'Tiêm chủng mở rộng vùng xa', NULL,
     'Support vaccination campaigns in remote areas.', 4,
     DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 29 DAY), 'APPROVED', NULL, 'AUTHORIZED', NOW(), NOW())
ON DUPLICATE KEY UPDATE
    title = VALUES(title),
    description = VALUES(description),
    category_id = VALUES(category_id),
    status = VALUES(status),
    balance = VALUES(balance),
    type = VALUES(type),
    updated_at = NOW();

-- One fundraising goal per campaign 11–20
INSERT INTO fundraising_goals (id, campaign_id, target_amount, description, is_active, created_at, updated_at)
VALUES
    (11, 11, 50000000.00, 'Initial goal: water systems for 5 schools', TRUE, NOW(), NOW()),
    (12, 12, 40000000.00, 'Medical kits goal', TRUE, NOW(), NOW()),
    (13, 13, 80000000.00, 'Shelter construction', TRUE, NOW(), NOW()),
    (14, 14, 55000000.00, 'Library equipment and books', TRUE, NOW(), NOW()),
    (15, 15, 35000000.00, 'Kitchen renovation', TRUE, NOW(), NOW()),
    (16, 16, 15000000.00, 'Milk program (completed phase)', TRUE, NOW(), NOW()),
    (17, 17, 12000000.00, 'Nutrition packs batch', TRUE, NOW(), NOW()),
    (18, 18, 45000000.00, 'Food packs for 500 families', TRUE, NOW(), NOW()),
    (19, 19, 70000000.00, 'Solar lights phase 1', TRUE, NOW(), NOW()),
    (20, 20, 25000000.00, 'Vaccination campaign', TRUE, NOW(), NOW())
ON DUPLICATE KEY UPDATE
    target_amount = VALUES(target_amount),
    description = VALUES(description),
    updated_at = NOW();

INSERT INTO campaign_follows (campaign_id, user_id, followed_at)
VALUES
    (11, 4, NOW()),
    (12, 4, NOW()),
    (14, 4, NOW()),
    (18, 4, NOW())
ON DUPLICATE KEY UPDATE followed_at = VALUES(followed_at);

SELECT COUNT(*) AS seed_campaigns_total FROM campaigns WHERE id BETWEEN 11 AND 20;
SELECT id, title, category_id, status, balance, type FROM campaigns WHERE id BETWEEN 11 AND 20 ORDER BY id;
