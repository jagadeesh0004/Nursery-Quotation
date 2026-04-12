# PowerShell script to fix the auth_method column issue
# Run this script on Windows to clean up the database

# Database credentials
$DB_HOST = "db.ymmbujskqowztgffqlmz.supabase.co"
$DB_PORT = 5432
$DB_USER = "postgres"
$DB_PASSWORD = "SUNS@9491944388"
$DB_NAME = "postgres"

# SQL commands to execute
$SQL_COMMANDS = @"
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

SELECT 'Database cleanup completed successfully!' as result;
"@

Write-Host "Starting database cleanup..."
Write-Host ""

try {
    # Create PostgreSQL connection string
    $connectionString = "Server=$DB_HOST;Port=$DB_PORT;User Id=$DB_USER;Password=$DB_PASSWORD;Database=$DB_NAME;"

    # Use psql command line tool (requires PostgreSQL client tools installed)
    $env:PGPASSWORD = $DB_PASSWORD

    $SQL_COMMANDS | psql -h $DB_HOST -U $DB_USER -d $DB_NAME

    if ($LASTEXITCODE -eq 0) {
        Write-Host ""
        Write-Host "✓ Database cleanup completed successfully!" -ForegroundColor Green
        Write-Host "You can now restart your application."
    } else {
        Write-Host ""
        Write-Host "✗ Error occurred during database cleanup" -ForegroundColor Red
        Write-Host "Exit code: $LASTEXITCODE"
    }
}
catch {
    Write-Host "Error: $_" -ForegroundColor Red
    Write-Host ""
    Write-Host "Make sure PostgreSQL client tools (psql) are installed and in your PATH"
}
finally {
    Remove-Item env:PGPASSWORD -ErrorAction SilentlyContinue
}

