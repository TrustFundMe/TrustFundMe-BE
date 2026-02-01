-- =======================================
-- Seed fake campaigns for testing
-- Run after init-all-databases.sql (DB + schema + sample users must exist).
-- Safe to run multiple times (ON DUPLICATE KEY UPDATE).
-- fund_owner_id = 3 = user "Fund Owner" from identity sample data.
-- =======================================

USE trustfundme_campaign_db;

-- Campaigns (ids 10–19 to avoid conflict with init sample 1, 2)
INSERT INTO campaigns (
    id, fund_owner_id, approved_by_staff, approved_at, thank_message, balance,
    title, description, category, start_date, end_date, status, created_at, updated_at
) VALUES
    (10, 7, 2, NOW(), 'Cảm ơn bạn đã ủng hộ!', 12500.00,
     'Water For All Children', 'Raise funds for clean water in rural schools.', 'Education',
     DATE_SUB(NOW(), INTERVAL 7 DAY), DATE_ADD(NOW(), INTERVAL 23 DAY), 'ACTIVE', NOW(), NOW()),
    (11, 3, 2, NOW(), 'Thank you for your support!', 8200.00,
     'Medical Kits for Rural Clinics', 'Provide basic medical supplies to remote health posts.', 'Healthcare',
     DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_ADD(NOW(), INTERVAL 57 DAY), 'ACTIVE', NOW(), NOW()),
    (12, 3, NULL, NULL, NULL, 0.00,
     'Emergency Shelter for Flood Victims', 'Build temporary shelters after floods.', 'Disaster Relief',
     NOW(), DATE_ADD(NOW(), INTERVAL 45 DAY), 'DRAFT', NOW(), NOW()),
    (13, 3, 2, DATE_SUB(NOW(), INTERVAL 2 DAY), 'We appreciate your help!', 31000.00,
     'School Library for Rural Kids', 'Books and furniture for village libraries.', 'Education',
     DATE_SUB(NOW(), INTERVAL 14 DAY), DATE_ADD(NOW(), INTERVAL 16 DAY), 'ACTIVE', NOW(), NOW()),
    (14, 3, NULL, NULL, NULL, 0.00,
     'Community Kitchen Renovation', 'Repair and equip community kitchens.', 'Community',
     DATE_ADD(NOW(), INTERVAL 5 DAY), DATE_ADD(NOW(), INTERVAL 65 DAY), 'PENDING', NOW(), NOW()),
    (15, 3, 2, DATE_SUB(NOW(), INTERVAL 5 DAY), NULL, 5600.00,
     'Milk for Primary Students', 'Daily milk program for primary schools.', 'Education',
     DATE_SUB(NOW(), INTERVAL 30 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), 'CLOSED', NOW(), NOW()),
    (16, 3, 2, NOW(), 'Thank you!', 4200.00,
     'Nutrition Packs for Mothers', 'Micronutrient packs for pregnant women.', 'Healthcare',
     DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_ADD(NOW(), INTERVAL 20 DAY), 'PAUSED', NOW(), NOW()),
    (17, 3, 2, NOW(), 'Every donation counts.', 18900.00,
     'Flood Relief Food Packs', 'Emergency food for families affected by floods.', 'Disaster Relief',
     DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_ADD(NOW(), INTERVAL 25 DAY), 'ACTIVE', NOW(), NOW()),
    (18, 3, NULL, NULL, NULL, 0.00,
     'Solar Lights for Villages', 'Install solar street lights in off-grid villages.', 'Community',
     NOW(), DATE_ADD(NOW(), INTERVAL 90 DAY), 'DRAFT', NOW(), NOW()),
    (19, 7, 2, DATE_SUB(NOW(), INTERVAL 1 DAY), 'Thanks!', 750.00,
     'Vaccination Drive for Children', 'Support vaccination campaigns in remote areas.', 'Healthcare',
     DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 29 DAY), 'ACTIVE', NOW(), NOW())
ON DUPLICATE KEY UPDATE
    title = VALUES(title),
    description = VALUES(description),
    category = VALUES(category),
    status = VALUES(status),
    balance = VALUES(balance),
    updated_at = NOW();

-- Fundraising goals (one per campaign, ids 10–19)
INSERT INTO fundraising_goals (id, campaign_id, target_amount, description, is_active, created_at, updated_at)
VALUES
    (10, 10, 20000.00, 'Initial goal for water project', TRUE, NOW(), NOW()),
    (11, 11, 15000.00, 'Medical kits goal', TRUE, NOW(), NOW()),
    (12, 12, 50000.00, 'Shelter construction', TRUE, NOW(), NOW()),
    (13, 13, 40000.00, 'Library equipment and books', TRUE, NOW(), NOW()),
    (14, 14, 25000.00, 'Kitchen renovation', TRUE, NOW(), NOW()),
    (15, 15, 10000.00, 'Milk program 3 months', TRUE, NOW(), NOW()),
    (16, 16, 8000.00, 'Nutrition packs batch', TRUE, NOW(), NOW()),
    (17, 17, 30000.00, 'Food packs for 500 families', TRUE, NOW(), NOW()),
    (18, 18, 60000.00, 'Solar lights phase 1', TRUE, NOW(), NOW()),
    (19, 19, 5000.00, 'Vaccination campaign', TRUE, NOW(), NOW())
ON DUPLICATE KEY UPDATE
    target_amount = VALUES(target_amount),
    updated_at = NOW();

-- Optional: campaign follows (user 4 follows some campaigns)
INSERT INTO campaign_follows (campaign_id, user_id, followed_at)
VALUES
    (10, 4, NOW()),
    (11, 4, NOW()),
    (13, 4, NOW()),
    (17, 4, NOW())
ON DUPLICATE KEY UPDATE followed_at = VALUES(followed_at);

-- Done
SELECT COUNT(*) AS campaigns_created FROM campaigns;
SELECT id, title, category, status, balance FROM campaigns ORDER BY id;
