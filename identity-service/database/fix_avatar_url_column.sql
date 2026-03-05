-- Script để tăng độ dài cột avatar_url
-- Chạy script này để fix lỗi "Data too long for column 'avatar_url'"

USE trustfundme_identity_db;

-- Tăng độ dài cột avatar_url từ 500 lên 1000
ALTER TABLE users MODIFY COLUMN avatar_url VARCHAR(1000);

-- Kiểm tra kết quả
DESCRIBE users;
