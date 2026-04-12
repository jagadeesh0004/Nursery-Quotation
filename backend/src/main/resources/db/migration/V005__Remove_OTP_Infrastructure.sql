 -- V005__Remove_OTP_Infrastructure.sql
-- Remove OTP-related tables and columns (using Google OAuth only)

-- ============================================================
-- Drop OTP table
-- ============================================================
DROP TABLE IF EXISTS otps CASCADE;

-- ============================================================
-- Remove phone_number column from users table
-- ============================================================
ALTER TABLE users DROP COLUMN IF EXISTS phone_number;

-- Drop indexes that referenced phone_number
DROP INDEX IF EXISTS idx_users_phone;

-- ============================================================
-- Update users table to add Google/Email-based columns
-- ============================================================
-- Add email and google_id columns if they don't exist
ALTER TABLE users
ADD COLUMN IF NOT EXISTS email VARCHAR(100) UNIQUE,
ADD COLUMN IF NOT EXISTS google_id VARCHAR(255) UNIQUE;

-- Create indexes for efficient lookups
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_google_id ON users(google_id);

