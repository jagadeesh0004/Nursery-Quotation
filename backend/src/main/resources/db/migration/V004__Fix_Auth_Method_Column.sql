-- Flyway migration: V004__Fix_Auth_Method_Column.sql
-- Fix the auth_method column that failed to migrate properly

-- Check if the column exists and has nulls
DO $$
BEGIN
    IF to_regclass('public.users') IS NULL THEN
        RETURN;
    END IF;

    -- If column doesn't exist, add it
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'users' AND column_name = 'auth_method'
    ) THEN
        ALTER TABLE users
        ADD COLUMN auth_method VARCHAR(20);
    END IF;

    -- Update any NULL values to 'OTP'
    UPDATE users SET auth_method = 'OTP' WHERE auth_method IS NULL;

    -- Try to add the constraint if it doesn't exist
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_schema = 'public' AND table_name = 'users' AND constraint_name = 'chk_auth_method'
    ) THEN
        ALTER TABLE users
        ADD CONSTRAINT chk_auth_method CHECK (auth_method IN ('OTP', 'GOOGLE'));
    END IF;

    -- Make the column NOT NULL
    ALTER TABLE users
    ALTER COLUMN auth_method SET NOT NULL;

END $$;

