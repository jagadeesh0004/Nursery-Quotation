-- SQL Fix Script for auth_method Column Issue
-- Execute this script directly in your PostgreSQL database client (pgAdmin, DBeaver, psql, etc.)
-- Database: postgres at db.ymmbujskqowztgffqlmz.supabase.co

-- Begin transaction for safety
BEGIN;

-- Step 1: Check current column status and drop problematic constraint if exists
ALTER TABLE public.users
DROP CONSTRAINT IF EXISTS chk_auth_method CASCADE;

-- Step 2: Make column nullable if it exists and is NOT NULL
ALTER TABLE public.users
ALTER COLUMN auth_method DROP NOT NULL;

-- Step 3: Update any NULL values to 'OTP' (default auth method)
UPDATE public.users
SET auth_method = 'OTP'
WHERE auth_method IS NULL;

-- Step 4: Set the column to NOT NULL after all values are set
ALTER TABLE public.users
ALTER COLUMN auth_method SET NOT NULL;

-- Step 5: Add the CHECK constraint to ensure only valid values
ALTER TABLE public.users
ADD CONSTRAINT chk_auth_method CHECK (auth_method IN ('OTP', 'GOOGLE'));

-- Verify the changes
SELECT
    column_name,
    data_type,
    is_nullable,
    column_default
FROM information_schema.columns
WHERE table_name = 'users' AND column_name = 'auth_method';

-- Show row count to verify data integrity
SELECT COUNT(*) as total_users, COUNT(auth_method) as auth_method_filled FROM public.users;

-- Commit the transaction
COMMIT;

-- Result message
SELECT 'Database cleanup completed successfully! The auth_method column has been fixed.' as completion_message;

