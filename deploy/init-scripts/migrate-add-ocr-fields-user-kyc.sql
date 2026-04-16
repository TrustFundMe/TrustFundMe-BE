-- ============================================================
-- Migration: Add OCR fields to user_kyc table
-- Database: trustfundme_identity_db
-- Function: Capture OCR-extracted data from CCCD/passport for e-contract
-- ============================================================

USE trustfundme_identity_db;

SET @dbname = 'trustfundme_identity_db';
SET @tablename = 'user_kyc';

-- Add full_name_ocr
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = 'full_name_ocr');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE user_kyc ADD COLUMN full_name_ocr VARCHAR(500) NULL',
    'SELECT ''Column full_name_ocr already exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add address
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = 'address');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE user_kyc ADD COLUMN address VARCHAR(1000) NULL',
    'SELECT ''Column address already exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add workplace
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = 'workplace');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE user_kyc ADD COLUMN workplace VARCHAR(500) NULL',
    'SELECT ''Column workplace already exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add tax_id
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = 'tax_id');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE user_kyc ADD COLUMN tax_id VARCHAR(50) NULL',
    'SELECT ''Column tax_id already exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
