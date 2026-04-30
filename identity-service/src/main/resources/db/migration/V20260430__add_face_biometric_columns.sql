-- =====================================================
-- Migration: Add face biometric columns to user_kyc
-- Purpose: Store face descriptor vector, liveness metadata,
--          and face mesh sample points for anti-spoofing
-- Date: 2026-04-30
-- =====================================================

ALTER TABLE user_kyc 
ADD COLUMN IF NOT EXISTS face_descriptor JSON DEFAULT NULL 
    COMMENT 'Face descriptor vector (128-dim) for face matching/comparison';

ALTER TABLE user_kyc 
ADD COLUMN IF NOT EXISTS liveness_metadata JSON DEFAULT NULL 
    COMMENT 'Liveness proof: steps completed (turn_left, turn_right, look_straight), timestamp, duration';

ALTER TABLE user_kyc 
ADD COLUMN IF NOT EXISTS face_mesh_sample JSON DEFAULT NULL 
    COMMENT 'Sample of 50 key 3D face mesh landmark points for future reference';
