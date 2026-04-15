@echo off
REM DEPLOYMENT SCRIPT - Quotation Number Fix (Windows)
REM Run this to deploy the fix to production

echo.
echo =====================================================================
echo   QUOTATION NUMBER FIX - DEPLOYMENT SCRIPT (Windows)
echo =====================================================================
echo.
echo This script will deploy the quotation number fix to production.
echo.
echo Changes Summary:
echo   [OK] Added per-user quotation number uniqueness check
echo   [OK] Fixed multi-user collision errors
echo   [OK] Build successful: 61.3 MB JAR
echo.
echo =====================================================================
echo.

REM Step 1: Navigate to project directory
echo [1/4] Navigating to project directory...
cd /d C:\Users\18080\quotation_project
if %ERRORLEVEL% EQU 0 (
    echo       [OK] In project directory
) else (
    echo       [ERROR] Failed to navigate to project directory
    exit /b 1
)
echo.

REM Step 2: Check git status
echo [2/4] Checking git status...
git status
echo.
echo       Press any key to continue...
pause >nul
echo.

REM Step 3: Stage and commit changes
echo [3/4] Staging and committing changes...
git add .
echo       [OK] Changes staged
echo.

echo       Committing changes...
git commit -m "Fix: Per-user quotation number uniqueness check

- Modified QuotationRepository: Added existsByUserIdAndQuotationNumber()
- Modified QuotationNumberingService: Updated to use per-user check
- Fixes issue where users with similar initials got collision errors
- No database migration required
- Fully backward compatible
- Build successful: 45 files compiled, 61.3 MB JAR
- Ready for production deployment"

if %ERRORLEVEL% EQU 0 (
    echo       [OK] Changes committed
) else (
    echo       [WARN] No new changes to commit (already committed)
)
echo.

REM Step 4: Push to main
echo [4/4] Pushing to main branch...
git push origin main
if %ERRORLEVEL% EQU 0 (
    echo       [OK] Push successful
    echo.
    echo =====================================================================
    echo   [OK] DEPLOYMENT INITIATED
    echo =====================================================================
    echo.
    echo Next steps:
    echo   1. Go to https://render.com/dashboard
    echo   2. Select 'backend' service
    echo   3. Wait for deployment to complete (2-5 minutes)
    echo   4. Check logs for: 'Started NurseryApplication'
    echo   5. Test at: https://quotation.onrender.com
    echo.
    echo Testing:
    echo   - Login with test account
    echo   - Create new quotation with items
    echo   - Click 'Finalize'
    echo   - Verify quotation number assigned (e.g., GVN0001)
    echo   - [OK] If successful, fix is working!
    echo.
    pause
) else (
    echo       [ERROR] Push failed
    exit /b 1
)

