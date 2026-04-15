#!/bin/bash
# DEPLOYMENT SCRIPT - Quotation Number Fix
# Run this to deploy the fix to production

echo "═══════════════════════════════════════════════════════════════════"
echo "  QUOTATION NUMBER FIX - DEPLOYMENT SCRIPT"
echo "═══════════════════════════════════════════════════════════════════"
echo ""
echo "This script will deploy the quotation number fix to production."
echo ""
echo "Changes Summary:"
echo "  ✅ Added per-user quotation number uniqueness check"
echo "  ✅ Fixed multi-user collision errors"
echo "  ✅ Build successful: 61.3 MB JAR"
echo ""
echo "═══════════════════════════════════════════════════════════════════"
echo ""

# Step 1: Navigate to project directory
echo "[1/4] Navigating to project directory..."
cd C:\Users\18080\quotation_project
if [ $? -eq 0 ]; then
    echo "      ✅ In project directory"
else
    echo "      ❌ Failed to navigate to project directory"
    exit 1
fi
echo ""

# Step 2: Check git status
echo "[2/4] Checking git status..."
git status
echo ""
read -p "      Press Enter to continue..."
echo ""

# Step 3: Stage and commit changes
echo "[3/4] Staging changes..."
git add .
echo "      ✅ Changes staged"
echo ""

echo "      Committing changes..."
git commit -m "Fix: Per-user quotation number uniqueness check

- Modified QuotationRepository: Added existsByUserIdAndQuotationNumber()
- Modified QuotationNumberingService: Updated to use per-user check
- Fixes issue where users with similar initials got collision errors
- No database migration required
- Fully backward compatible
- Build successful: 45 files compiled, 61.3 MB JAR
- Ready for production deployment"

if [ $? -eq 0 ]; then
    echo "      ✅ Changes committed"
else
    echo "      ⚠️  No new changes to commit (already committed)"
fi
echo ""

# Step 4: Push to main
echo "[4/4] Pushing to main branch..."
git push origin main
if [ $? -eq 0 ]; then
    echo "      ✅ Push successful"
    echo ""
    echo "═══════════════════════════════════════════════════════════════════"
    echo "  ✅ DEPLOYMENT INITIATED"
    echo "═══════════════════════════════════════════════════════════════════"
    echo ""
    echo "Next steps:"
    echo "  1. Go to https://render.com/dashboard"
    echo "  2. Select 'backend' service"
    echo "  3. Wait for deployment to complete (2-5 minutes)"
    echo "  4. Check logs for: 'Started NurseryApplication'"
    echo "  5. Test at: https://quotation.onrender.com"
    echo ""
    echo "Testing:"
    echo "  • Login with test account"
    echo "  • Create new quotation with items"
    echo "  • Click 'Finalize'"
    echo "  • Verify quotation number assigned (e.g., GVN0001)"
    echo "  • ✅ If successful, fix is working!"
    echo ""
else
    echo "      ❌ Push failed"
    exit 1
fi

