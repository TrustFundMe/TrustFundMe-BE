USE trustfundme_identity_db;

-- Xóa bảng cũ nếu có
DROP TABLE IF EXISTS Users;
DROP TABLE IF EXISTS users;

-- JPA sẽ tự động tạo bảng users với cấu trúc đúng từ Entity User
-- Hoặc bạn có thể chạy script này để tạo thủ công:

-- Xóa bảng cũ nếu có
DROP TABLE IF EXISTS Users;
DROP TABLE IF EXISTS users;

CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    phone_number VARCHAR(255),
    avatar_url VARCHAR(500),
    role VARCHAR(50) NOT NULL DEFAULT 'USER',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    INDEX idx_email (email)
);
