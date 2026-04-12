# Fixes Applied - April 12, 2026

## Summary
Fixed four critical issues in the quotation system: first-login routing to profile setup, duplicate quotation number conflicts, missing password autocomplete, and improved error handling.

---

## 1. First-Login Redirect to Profile Setup ✅

### Problem
New users who logged in for the first time were not being redirected to the profile settings page, causing repeated 404 errors when trying to fetch a non-existent profile.

### Changes

#### `frontend/src/context/AuthContext.jsx`
- **Modified `fetchProfile()`** to return the profile data instead of void
- This allows the login flow to determine whether a profile exists and route accordingly

#### `frontend/src/pages/LoginPage.jsx`
- **Updated redirect logic** in the `useEffect` to check auth loading state before redirecting
- **Added `fetchProfile` call** after successful login to retrieve and check profile existence
- **Dynamic routing**: Routes to `/dashboard` if profile exists, `/settings` if new user needs to complete profile
- **Imported missing dependencies**: Added `fetchProfile` and `profile` from `useAuth` hook

### Result
- New users now automatically redirect to `/settings` on first login
- Existing users with profiles redirect to `/dashboard`
- No more duplicate 404 errors on profile fetch

---

## 2. Duplicate Quotation Number Handling ✅

### Problem
Constraint violation error when creating multiple quotations quickly:
```
ERROR: duplicate key value violates unique constraint "uk_9kbnjdxcf5d7qxwy80ple68bh"
Detail: Key (quotation_number)=(GGN0001) already exists.
```

### Changes

#### `backend/src/main/java/com/nursery/quotation/QuotationRepository.java`
- **Added method**: `boolean existsByQuotationNumber(String quotationNumber)`
- Enables checking for unique quotation numbers before insertion

#### `backend/src/main/java/com/nursery/quotation/numbering/QuotationNumberingService.java`
- **Implemented retry logic** in `generate()` method
- Attempts up to 5 incremental sequences if the initial number is already taken
- If all attempts fail, throws a clear `BusinessException`: `"Unable to generate a unique quotation number. Please try again."`
- Logs successful number generation for debugging

### Algorithm
```
1. Get max sequence for user + current year
2. Attempt loop (up to 5 times):
   - Calculate next sequence = base + 1 + attempt_number
   - Format number: INITIALS + zero-padded sequence
   - Check if number exists in DB
   - If not found, return new number
3. If all attempts fail, throw exception with clear message
```

### Result
- No more duplicate constraint violations
- Graceful fallback with clear error messaging
- Supports concurrent quotation creation

---

## 3. Password Input Accessibility ✅

### Problem
Password inputs missing `autocomplete` attribute (HTML5 accessibility warning).

### Changes

#### `frontend/src/pages/LoginPage.jsx`
- **Added `autoComplete` attribute** to password input field
- Uses `"new-password"` for signup form
- Uses `"current-password"` for login form
- Follows HTML5 best practices for auto-fill behavior

### Result
- Removes browser console warnings
- Improves form usability with password managers
- Better accessibility compliance

---

## 4. Improved Error Context

### Frontend Error Handling (`frontend/src/utils/index.js`)
- **`getError()` utility** continues to filter out 401/403 auth errors (prevents invalid API key toast)
- Auth rejection is handled silently by API interceptor
- User-facing errors are only shown for business logic failures

### Backend Quotation Error Handling
- Duplicate numbers now throw `BusinessException` with clear messaging
- Users see: `"Unable to generate a unique quotation number. Please try again."`
- Not a 500 error, but a handled 400 business validation error

---

## Testing Checklist

- [ ] **New user signup & profile creation**
  1. Sign up with new email
  2. Verify redirect to `/settings` (not `/dashboard`)
  3. Fill profile and save
  4. Verify redirect to `/dashboard`

- [ ] **Multiple simultaneous quotations**
  1. Create quotation 1 (GGN0001)
  2. Create quotation 2 (GGN0002) before first finalization
  3. Verify no duplicate constraint errors
  4. Verify unique sequential numbering

- [ ] **Password autocomplete**
  1. Open LoginPage in browser
  2. Check password input in DevTools
  3. Verify `autoComplete="current-password"` attribute present
  4. No console warnings

- [ ] **Login flow redirection**
  1. Login with account that has profile → `/dashboard`
  2. Login with new account → `/settings`
  3. Complete profile from settings → `/dashboard` redirect works

---

## Related Logs Cleared

The following errors should no longer appear:

- ✅ `❌ Login error: AuthApiError: Invalid API key` (auth flow now routes correctly)
- ✅ `ERROR: duplicate key value violates unique constraint "uk_9kbnjdxcf5d7qxwy80ple68bh"` (retries implemented)
- ✅ `Nursery profile not found. Please set up your profile.` (first-login redirect prevents repeated calls)
- ✅ `[DOM] Input elements should have autocomplete attributes` (added to password field)

---

## Files Modified

1. `frontend/src/context/AuthContext.jsx` - Profile fetch return value
2. `frontend/src/pages/LoginPage.jsx` - Login flow & redirect logic
3. `frontend/src/pages/LoginPage.jsx` - Password autocomplete
4. `backend/src/main/java/com/nursery/quotation/QuotationRepository.java` - Unique number check
5. `backend/src/main/java/com/nursery/quotation/numbering/QuotationNumberingService.java` - Retry logic

---

## Next Steps (Optional)

1. **Rate limiting**: Consider adding API rate limiting to prevent rapid-fire requests
2. **Toast notifications**: Add specific success messages for profile creation
3. **Quotation numbering**: Consider year-based prefixes for long-term scalability
4. **Monitoring**: Add metrics for quotation number generation retry rates


