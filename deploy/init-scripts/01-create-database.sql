-- Create database for Identity Service
CREATE DATABASE IF NOT EXISTS trustfundme_identity_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Create user and grant privileges
CREATE USER IF NOT EXISTS 'trustfundme_user'@'%' IDENTIFIED BY 'trustfundme_password';

-- Grant privileges on database
GRANT ALL PRIVILEGES ON trustfundme_identity_db.* TO 'trustfundme_user'@'%';

FLUSH PRIVILEGES;
