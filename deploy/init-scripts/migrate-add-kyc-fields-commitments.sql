-- ============================================================
-- Migration: Add KYC/OCR fields to Campaign Commitments Table
-- Database: trustfundme_campaign_db
-- Function: Capture full OCR data at sign time (address, workplace, taxId)
-- ============================================================

USE trustfundme_campaign_db;

SET @dbname = 'trustfundme_campaign_db';
SET @tablename = 'campaign_commitments';

-- Add issue_place
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = 'issue_place');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE campaign_commitments ADD COLUMN issue_place VARCHAR(255)',
    'SELECT ''Column issue_place already exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add issue_date
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = 'issue_date');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE campaign_commitments ADD COLUMN issue_date DATE',
    'SELECT ''Column issue_date already exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add address
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = 'address');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE campaign_commitments ADD COLUMN address VARCHAR(1000)',
    'SELECT ''Column address already exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add workplace
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = 'workplace');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE campaign_commitments ADD COLUMN workplace VARCHAR(500)',
    'SELECT ''Column workplace already exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add tax_id
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = 'tax_id');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE campaign_commitments ADD COLUMN tax_id VARCHAR(50)',
    'SELECT ''Column tax_id already exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
