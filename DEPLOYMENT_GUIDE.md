# Deployment Instructions - Quotation System Fixes

## 🚀 Pre-Deployment

### Backend Build
```bash
cd backend
mvn clean package -DskipTests
```
Expected output: `[INFO] BUILD SUCCESS`

### Frontend Build
```bash
cd frontend
npm install  # If you haven't already
npm run build
```
Expected output: `✓ built in X.XXs`

---

## 📦 Deployment Steps

### 1. Stop Current Services
```bash
# Stop backend (if running on port 8091)
# Stop frontend dev server (if running on port 5173)
```

### 2. Deploy Backend
```bash
# Replace old JAR with new build
cp backend/target/backend-1.0.0.jar /path/to/deployment/

# Restart backend service
java -jar backend/target/backend-1.0.0.jar
```

### 3. Deploy Frontend
```bash
# Copy built dist folder to web server
cp -r frontend/dist/* /path/to/web/root/

# Or if using Node.js server:
npm run preview  # Test locally first
```

### 4. Verify Database Consistency
```bash
# No migration needed (no schema changes)
# Existing quotation_number records will remain valid
```

---

## ✅ Post-Deployment Testing

### Test 1: New User Signup Flow
1. Open http://localhost:5173 (or your domain)
2. Click "Sign Up"
3. Create account with test email/password
4. **Expected**: Redirected to `/settings` (profile setup page)
5. Fill in nursery details and save
6. **Expected**: Redirected to `/dashboard`

### Test 2: First-Time Login
1. Log out (if still logged in)
2. Log back in with same user
3. **Expected**: Redirected directly to `/dashboard`

### Test 3: Rapid Quotation Creation
1. From dashboard, create new quotation
2. Fill in customer details and add items
3. Finalize quotation (GGN0001)
4. Create another quotation immediately
5. Finalize quotation (GGN0002)
6. **Expected**: Both quotations have unique numbers, no errors

### Test 4: Password Field
1. Open DevTools (F12)
2. Inspect password input
3. **Expected**: Should see `autocomplete="current-password"` for login
4. **Expected**: Should see `autocomplete="new-password"` for signup

### Test 5: Browser Logs
1. Open DevTools Console
2. Look for errors
3. **Expected**: No "[DOM] Input elements should have autocomplete" warnings
4. **Expected**: No "Invalid API key" toasts (authentication errors should fail silently)

---

## 🔍 Monitoring After Deployment

### Logs to Watch For (Normal)
- `Generated quotation number: XXX0001 for userId: ...` (debug level)
- `🔐 Attempting login with: user@email.com` (console log)
- `✅ Session restored: ...` (console log)

### Logs to Alert On (Errors)
- `ERROR: duplicate key value violates unique constraint` (should not appear)
- `Nursery profile not found` appearing multiple times (should appear once per session)
- `Unable to generate a unique quotation number` (retry logic exhausted)

### Metrics to Monitor
- Database query performance (number existence checks)
- Session creation success rate (should be >99%)
- Average quotation creation time (should be <2s)

---

## 🔄 Rollback Plan

If issues occur after deployment:

### Quick Rollback
```bash
# Revert to previous JAR
cp /path/to/backup/backend-1.0.0.jar.old backend-1.0.0.jar
java -jar backend-1.0.0.jar

# Revert frontend (clear cache)
# Restore previous dist folder or rebuild from last commit
```

### Database State
- No migration was performed, so database remains unchanged
- Old and new code can coexist safely
- No data loss risk

---

## 📝 Commit Messages for VCS

```
Backend:
  Fix: Implement duplicate quotation number retry logic
  - Add existsByQuotationNumber() to QuotationRepository
  - Retry up to 5 times with incremental sequences
  - Throw clear error message on failure

Frontend:
  Fix: First-login redirect to profile setup page
  - Return profile data from fetchProfile()
  - Dynamic routing based on profile existence
  - Route new users to /settings, returning users to /dashboard
  
  Fix: Add autocomplete attribute to password input
  - new-password for signup form
  - current-password for login form
```

---

## 🆘 Troubleshooting

### Issue: Users still see 404 profile errors
- **Cause**: Cache not cleared, old compiled code running
- **Fix**: Clear browser cache (Ctrl+Shift+Delete), restart backend, rebuild frontend

### Issue: Duplicate number errors still appearing
- **Cause**: Old JARs still running in background
- **Fix**: Kill all Java processes, verify only one backend instance running (netstat -ano | findstr :8091)

### Issue: Password autocomplete not working
- **Cause**: Form submission modifying input attributes dynamically
- **Fix**: Check browser password manager settings, clear browser cache

### Issue: Quotation numbers skipping sequences
- **Cause**: Retry logic incrementing past failed attempts
- **Status**: Expected behavior (e.g., if 0001-0003 taken, next is 0004)
- **Fix**: No fix needed, this is correct

---

## 📞 Support

For issues or questions about these changes, refer to:
- `FIXES_APPLIED.md` - Detailed technical documentation
- Backend logs: `~/quotation_project/backend/target/logs/`
- Frontend console: Browser DevTools → Console tab


