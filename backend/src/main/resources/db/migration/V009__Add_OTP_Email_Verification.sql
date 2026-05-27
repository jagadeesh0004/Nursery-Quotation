-- Flyway migration for OTP Email Verification
-- Version: 009
-- Description: Create email_verifications table for OTP-based email verification

CREATE TABLE IF NOT EXISTS email_verifications (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL,
  email VARCHAR(255) NOT NULL,
  otp_code VARCHAR(6) NOT NULL,
  is_verified BOOLEAN DEFAULT FALSE,
  attempts INT DEFAULT 0,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  expires_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP + INTERVAL '10 minutes'
);

-- Create indexes for fast lookups
CREATE INDEX IF NOT EXISTS idx_email_verifications_email ON email_verifications(email);
CREATE INDEX IF NOT EXISTS idx_email_verifications_user_id ON email_verifications(user_id);
CREATE INDEX IF NOT EXISTS idx_email_verifications_verified ON email_verifications(is_verified);

-- Add email_verified column to users table if it doesn't exist
ALTER TABLE users ADD COLUMN IF NOT EXISTS email_verified BOOLEAN DEFAULT FALSE;

-- Create index on email_verified for faster login checks
CREATE INDEX IF NOT EXISTS idx_users_email_verified ON users(email_verified);

