-- ============================================================
-- Migration: Add Campaign Commitments Table
-- Database: trustfundme_campaign_db
-- Function: Record digital signature for campaign responsibility
-- ============================================================

USE trustfundme_campaign_db;

CREATE TABLE IF NOT EXISTS campaign_commitments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    campaign_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    id_number VARCHAR(50) NOT NULL,
    phone_number VARCHAR(20),
    content TEXT NOT NULL,
    signature_url TEXT,
    ip_address VARCHAR(50),
    status VARCHAR(50) DEFAULT 'SIGNED',
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    INDEX idx_campaign_id (campaign_id),
    INDEX idx_user_id (user_id),
    CONSTRAINT fk_commitment_campaign FOREIGN KEY (campaign_id) REFERENCES campaigns(id) ON DELETE CASCADE
);
