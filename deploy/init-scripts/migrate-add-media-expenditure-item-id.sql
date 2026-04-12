-- Migration: add expenditure_item_id to media table
-- Run this on an EXISTING DB that was created before this column was added.
-- Safe to run multiple times (IF NOT EXISTS guard).

USE trustfundme_media_db;

SET @col_exists = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = 'trustfundme_media_db'
      AND TABLE_NAME   = 'media'
      AND COLUMN_NAME  = 'expenditure_item_id'
);

SET @sql = IF(@col_exists = 0,
    'ALTER TABLE media ADD COLUMN expenditure_item_id BIGINT NULL AFTER expenditure_id',
    'SELECT ''expenditure_item_id already exists — skipping'' AS info'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add index if not already present
SET @idx_exists = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = 'trustfundme_media_db'
      AND TABLE_NAME   = 'media'
      AND INDEX_NAME   = 'idx_media_expenditure_item_id'
);

SET @idx_sql = IF(@idx_exists = 0,
    'ALTER TABLE media ADD INDEX idx_media_expenditure_item_id (expenditure_item_id)',
    'SELECT ''index already exists — skipping'' AS info'
);

PREPARE idx_stmt FROM @idx_sql;
EXECUTE idx_stmt;
DEALLOCATE PREPARE idx_stmt;
