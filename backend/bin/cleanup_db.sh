#!/bin/bash

# Database cleanup script to fix the auth_method column issue
# This script should be run against your PostgreSQL database

DB_URL="jdbc:postgresql://db.ymmbujskqowztgffqlmz.supabase.co:5432/postgres?user=postgres"
DB_PASSWORD="SUNS@9491944388"

echo "This script will fix the auth_method column issue in the users table."
echo "Please have the database password ready."
echo ""
echo "Running database cleanup..."

# Connect to database and execute the fix SQL
PGPASSWORD="$DB_PASSWORD" psql -h db.ymmbujskqowztgffqlmz.supabase.co -U postgres -d postgres << EOF

-- Begin transaction
BEGIN;

-- Step 1: Drop the problematic constraint if it exists
ALTER TABLE users
DROP CONSTRAINT IF EXISTS chk_auth_method CASCADE;

-- Step 2: Make column nullable if it exists
ALTER TABLE users
ALTER COLUMN auth_method DROP NOT NULL;

-- Step 3: Update NULL values to 'OTP'
UPDATE users SET auth_method = 'OTP' WHERE auth_method IS NULL;

-- Step 4: Add NOT NULL constraint
ALTER TABLE users
ALTER COLUMN auth_method SET NOT NULL;

-- Step 5: Add CHECK constraint
ALTER TABLE users
ADD CONSTRAINT chk_auth_method CHECK (auth_method IN ('OTP', 'GOOGLE'));

-- Commit transaction
COMMIT;

SELECT 'Database cleanup completed successfully!' as result;

EOF

echo "Done! The database has been fixed. You can now restart your application."

