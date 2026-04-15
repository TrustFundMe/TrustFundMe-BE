-- ============================================================
-- Migration: Add kyc_verified column to users table
-- Database: trustfundme_identity_db
-- Function: Differentiate between auth verification and KYC verification
-- ============================================================

USE trustfundme_identity_db;

SET @dbname = 'trustfundme_identity_db';
SET @tablename = 'users';

-- Add kyc_verified
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = 'kyc_verified');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE users ADD COLUMN kyc_verified BOOLEAN NOT NULL DEFAULT FALSE',
    'SELECT ''Column kyc_verified already exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
