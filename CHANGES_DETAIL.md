# Line-by-Line Changes - Quick Reference

## File 1: frontend/src/context/AuthContext.jsx

### Change 1: Make fetchProfile return data (Line 154-173)

```javascript
// BEFORE (Line 154-173):
const fetchProfile = useCallback(async () => {
  if (!session?.user || !session?.access_token) {
    setProfile(null)
    return  // ← No return value
  }
  try {
    const res = await profileService.get()
    setProfile(res.data.data)
  } catch (err) {
    // ... error handling ...
    setProfile(null)
  }
}, [session?.user, session?.access_token])

// AFTER:
const fetchProfile = useCallback(async () => {
  if (!session?.user || !session?.access_token) {
    setProfile(null)
    return null  // ← Returns null
  }
  try {
    const res = await profileService.get()
    setProfile(res.data.data)
    return res.data.data  // ← Returns profile data
  } catch (err) {
    if (err.response?.status === 404) {
      setProfile(null)
      return null  // ← Returns null for new users
    } else if (err.response?.status === 401) {
      console.log('Profile fetch returned 401; backend rejected token')
    } else {
      console.log('Profile fetch error:', err.message)
      setProfile(null)
    }
    return null  // ← Returns null on error
  }
}, [session?.user, session?.access_token])
```

---

## File 2: frontend/src/pages/LoginPage.jsx

### Change 1: Add dynamic routing based on profile (Line 18-26)

```javascript
// BEFORE (Line 18-25):
const { isAuthenticated, login, refreshSession } = useAuth()

// Redirect if already logged in
useEffect(() => {
  if (isAuthenticated) {
    navigate('/dashboard', { replace: true })
  }
}, [isAuthenticated, navigate])

// AFTER:
const { isAuthenticated, login, refreshSession, fetchProfile, profile, loading: authLoading } = useAuth()

// Redirect if already logged in
useEffect(() => {
  if (authLoading) return  // ← Wait for auth to load
  if (isAuthenticated) {
    const target = profile ? '/dashboard' : '/settings'  // ← Dynamic route
    navigate(target, { replace: true })
  }
}, [isAuthenticated, profile, authLoading, navigate])
```

### Change 2: Fetch profile after login and route accordingly (Line 47-59)

```javascript
// BEFORE (Line 47-59):
if (!result.success) {
  console.error('❌ Login error:', result.error)
  toast.error(result.error?.message || 'Failed to sign in')
} else if (result.session) {
  console.log('✅ Login successful:', email)
  console.log('✅ Session token:', result.session.access_token?.substring(0, 20) + '...')
  toast.success('Signed in successfully')
  navigate('/dashboard', { replace: true })  // ← Always go to dashboard
} else {
  await refreshSession()
  console.warn('⚠️ Login response has no session or error:', result)
  toast.error('Login failed: No session returned')
}

// AFTER:
if (!result.success) {
  console.error('❌ Login error:', result.error)
  toast.error(result.error?.message || 'Failed to sign in')
} else if (result.session) {
  console.log('✅ Login successful:', email)
  console.log('✅ Session token:', result.session.access_token?.substring(0, 20) + '...')
  const profileData = await fetchProfile()  // ← Fetch profile
  const target = profileData ? '/dashboard' : '/settings'  // ← Dynamic route
  toast.success('Signed in successfully')
  navigate(target, { replace: true })
} else {
  await refreshSession()
  console.warn('⚠️ Login response has no session or error:', result)
  toast.error('Login failed: No session returned')
}
```

### Change 3: Add autocomplete to password input (Line 145-149)

```javascript
// BEFORE (Line 145-149):
<input
  type={showPassword ? 'text' : 'password'}
  value={password}
  onChange={(e) => setPassword(e.target.value)}
  placeholder="••••••••"
  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-[#5CB338] focus:border-transparent pr-10"
  disabled={loading}
/>

// AFTER:
<input
  type={showPassword ? 'text' : 'password'}
  value={password}
  onChange={(e) => setPassword(e.target.value)}
  placeholder="••••••••"
  autoComplete={showSignup ? 'new-password' : 'current-password'}  // ← NEW LINE
  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-[#5CB338] focus:border-transparent pr-10"
  disabled={loading}
/>
```

---

## File 3: backend/src/main/java/com/nursery/quotation/QuotationRepository.java

### Change 1: Add existence check method (Line 39)

```java
// BEFORE (Line 34-38):
@Query("SELECT COALESCE(MAX(q.sequence), 0) FROM Quotation q WHERE q.userId = :userId AND q.year = :year")
int findMaxSequenceForUserAndYear(UUID userId, int year);

// ─── Analytics ───────────────────────────────────────────────

// AFTER:
@Query("SELECT COALESCE(MAX(q.sequence), 0) FROM Quotation q WHERE q.userId = :userId AND q.year = :year")
int findMaxSequenceForUserAndYear(UUID userId, int year);

boolean existsByQuotationNumber(String quotationNumber);  // ← NEW LINE

// ─── Analytics ───────────────────────────────────────────────
```

---

## File 4: backend/src/main/java/com/nursery/quotation/numbering/QuotationNumberingService.java

### Change 1: Implement retry logic in generate() method (Line 30-51)

```java
// BEFORE (Line 30-44):
public GeneratedNumber generate(UUID userId) {
  int currentYear = Year.now().getValue();

  NurseryProfile profile = profileRepository.findByUserId(userId)
          .orElseThrow(() -> new BusinessException(
                  "Nursery profile not set up. Please complete your profile before finalizing quotations."));

  int nextSequence = quotationRepository.findMaxSequenceForUserAndYear(userId, currentYear) + 1;

  String initials = profile.getInitials().toUpperCase();
  String number = String.format("%s%04d", initials, nextSequence);

  log.debug("Generated quotation number: {} for userId: {}", number, userId);
  return new GeneratedNumber(number, currentYear, nextSequence);
}

// AFTER:
public GeneratedNumber generate(UUID userId) {
  int currentYear = Year.now().getValue();

  NurseryProfile profile = profileRepository.findByUserId(userId)
          .orElseThrow(() -> new BusinessException(
                  "Nursery profile not set up. Please complete your profile before finalizing quotations."));

  int baseSequence = quotationRepository.findMaxSequenceForUserAndYear(userId, currentYear);
  String initials = profile.getInitials().toUpperCase();

  // ← NEW: Retry logic (5 attempts)
  int attempt = 0;
  while (attempt < 5) {
    int nextSequence = baseSequence + 1 + attempt;
    String number = String.format("%s%04d", initials, nextSequence);

    if (!quotationRepository.existsByQuotationNumber(number)) {
      log.debug("Generated quotation number: {} for userId: {}", number, userId);
      return new GeneratedNumber(number, currentYear, nextSequence);
    }

    attempt++;
  }

  throw new BusinessException("Unable to generate a unique quotation number. Please try again.");
}
```

---

## Summary of Changes

| File | Type | Lines | Purpose |
|------|------|-------|---------|
| AuthContext.jsx | Modified | 154-173 | Return profile from fetchProfile() |
| LoginPage.jsx | Modified | 18-26 | Add dynamic routing with authLoading check |
| LoginPage.jsx | Modified | 47-59 | Fetch profile after login & determine route |
| LoginPage.jsx | Modified | 145-149 | Add autocomplete attribute |
| QuotationRepository.java | Added | 39 | Add existsByQuotationNumber() method |
| QuotationNumberingService.java | Modified | 30-51 | Implement retry loop for unique numbers |

**Total**: 6 changes across 4 files
**Total lines modified**: ~40 lines
**New functionality**: Retry logic for quotation numbers, dynamic user routing


