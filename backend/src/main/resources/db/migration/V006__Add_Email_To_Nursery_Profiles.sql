-- V006__Add_Email_To_Nursery_Profiles.sql
-- Add email column to nursery_profiles table for login and communication

-- ============================================================
-- Add email column to nursery_profiles table
-- ============================================================
ALTER TABLE nursery_profiles
ADD COLUMN IF NOT EXISTS email VARCHAR(255) NULL;

-- ============================================================
-- Add UNIQUE constraint on email (safe - won't fail if already exists)
-- ============================================================
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_schema = 'public'
          AND table_name = 'nursery_profiles'
          AND constraint_name = 'uq_nursery_profiles_email'
    ) THEN
        ALTER TABLE nursery_profiles
        ADD CONSTRAINT uq_nursery_profiles_email UNIQUE (email);
    END IF;
END $$;

-- ============================================================
-- Create index for email lookups (improves query performance)
-- ============================================================
CREATE INDEX IF NOT EXISTS idx_nursery_profiles_email ON nursery_profiles(email);

-- ============================================================
-- Add NOT NULL constraint to email (requires populating existing rows)
-- Note: This step requires data. Uncomment after populating email values
-- ============================================================
-- ALTER TABLE nursery_profiles
-- ALTER COLUMN email SET NOT NULL;

