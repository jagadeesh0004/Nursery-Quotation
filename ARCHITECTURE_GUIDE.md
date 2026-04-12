# Production Error Handling Architecture

## System Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              FRONTEND (React + Vite)                         │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────┐  │
│  │                        REACT COMPONENTS                              │  │
│  │  (QuotationFormPage, PlantsPage, etc.)                               │  │
│  └────────────────────────────┬─────────────────────────────────────────┘  │
│                               │                                             │
│  ┌────────────────────────────▼─────────────────────────────────────────┐  │
│  │                    ERROR HANDLING LAYER                              │  │
│  │                                                                       │  │
│  │  ┌──────────────────┐  ┌──────────────────┐  ┌─────────────────┐   │  │
│  │  │ toastService.js  │  │ errorMapper.js   │  │ useApiCall.js   │   │  │
│  │  │                  │  │                  │  │ useFormSubmit.js │  │  │
│  │  │ • success()      │  │ • mapError()     │  │                 │   │  │
│  │  │ • error()        │  │ • getToastMsg()  │  │ • Hooks for     │   │  │
│  │  │ • warning()      │  │ • isAuthError()  │  │   auto-handling │   │  │
│  │  │ • info()         │  │ • isValidation.. │  │                 │   │  │
│  │  │ • loading()      │  │ • isServerError()│  │                 │   │  │
│  │  └──────────────────┘  └──────────────────┘  └─────────────────┘   │  │
│  └────────────────────┬─────────────────────┬─────────────────────────┘  │
│                       │                     │                             │
│  ┌────────────────────▼─────────────────────▼─────────────────────────┐  │
│  │                      AXIOS API INSTANCE                            │  │
│  │                        (api.js)                                    │  │
│  │                                                                    │  │
│  │  ┌──────────────────────────────────────────────────────────┐    │  │
│  │  │ Request Interceptor                                      │    │  │
│  │  │ ✓ Attach auth token from Supabase                        │    │  │
│  │  │ ✓ Log all requests                                       │    │  │
│  │  └──────────────────────────────────────────────────────────┘    │  │
│  │                                                                    │  │
│  │  ┌──────────────────────────────────────────────────────────┐    │  │
│  │  │ Response Interceptor                                     │    │  │
│  │  │ ✓ Log all responses                                      │    │  │
│  │  │ ✓ Handle 401 → Redirect to login                         │    │  │
│  │  │ ✓ Pass errors to consuming component                     │    │  │
│  │  └──────────────────────────────────────────────────────────┘    │  │
│  └────────────────────────┬────────────────────────────────────────┘  │
│                           │                                             │
└───────────────────────────┼─────────────────────────────────────────────┘
                            │ HTTP
                            │ (GET/POST/PUT/DELETE)
                            │
┌───────────────────────────┼─────────────────────────────────────────────┐
│                           ▼                                              │
│             BACKEND (Spring Boot 3.2.5)                                 │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │                    SPRING CONTROLLERS                            │  │
│  │  (@PostMapping, @PutMapping, @DeleteMapping, etc.)              │  │
│  │                                                                  │  │
│  │  Request Body Validation: @Valid @RequestBody DTO               │  │
│  └────────────────────────┬─────────────────────────────────────────┘  │
│                           │                                             │
│  ┌────────────────────────▼─────────────────────────────────────────┐  │
│  │                  SERVICE LAYER                                    │  │
│  │  (Business Logic)                                                │  │
│  │                                                                  │  │
│  │  if (invalidData) {                                              │  │
│  │    throw new BusinessException("User-friendly message")          │  │
│  │  }                                                               │  │
│  └────────────────────────┬─────────────────────────────────────────┘  │
│                           │                                             │
│  ┌────────────────────────▼─────────────────────────────────────────┐  │
│  │           GLOBAL EXCEPTION HANDLER                               │  │
│  │  (GlobalExceptionHandler.java)                                   │  │
│  │                                                                  │  │
│  │  ┌─────────────────────────────────────────────────────────┐   │  │
│  │  │ Exception Type              HTTP Status  Response       │   │  │
│  │  ├─────────────────────────────────────────────────────────┤   │  │
│  │  │ MethodArgumentNotValidException    400  Validation errs │   │  │
│  │  │ ConstraintViolationException       400  Validation errs │   │  │
│  │  │ BusinessException                 400  Custom message   │   │  │
│  │  │ ResourceNotFoundException          404  Not found msg   │   │  │
│  │  │ BadCredentialsException            401  Invalid creds   │   │  │
│  │  │ AccessDeniedException              403  No permission   │   │  │
│  │  │ Exception (generic)                500  Generic error   │   │  │
│  │  └─────────────────────────────────────────────────────────┘   │  │
│  └────────────────────────┬─────────────────────────────────────────┘  │
│                           │                                             │
│  ┌────────────────────────▼─────────────────────────────────────────┐  │
│  │              STRUCTURED ERROR RESPONSE                           │  │
│  │  (ErrorResponse.java or ApiResponse.java)                        │  │
│  │                                                                  │  │
│  │  {                                                               │  │
│  │    "success": false,                                             │  │
│  │    "message": "Validation failed",                               │  │
│  │    "errors": [                                                   │  │
│  │      "price: Price must be greater than 0",                      │  │
│  │      "quantity: Quantity must be positive"                       │  │
│  │    ],                                                            │  │
│  │    "timestamp": "2024-04-12T10:30:00Z"                           │  │
│  │  }                                                               │  │
│  └────────────────────┬─────────────────────────────────────────────┘  │
│                       │                                                 │
└───────────────────────┼─────────────────────────────────────────────────┘
                        │ HTTP Response
                        │
        ┌───────────────▼──────────────┐
        │  Network to Frontend        │
        │  ← JSON Response ←          │
        └───────────────┬──────────────┘
                        │
        ┌───────────────▼──────────────────────────────┐
        │  Frontend Response Interceptor               │
        │  (Logs response, checks status, handles 401) │
        └───────────────┬──────────────────────────────┘
                        │
        ┌───────────────▼──────────────────────────────┐
        │  Component Error Handler                    │
        │  (Calls errorMapper.mapError())              │
        │  (Shows toastService toast)                  │
        │  (Updates form field errors if validation)   │
        └───────────────┬──────────────────────────────┘
                        │
        ┌───────────────▼──────────────────────────────┐
        │  User Sees:                                 │
        │  ✓ Toast notification with message          │
        │  ✓ Field-level errors highlighted           │
        │  ✓ Loading state cleared                    │
        └────────────────────────────────────────────┘
```

---

## Data Flow Examples

### Example 1: Validation Error Flow

```
User fills form with invalid data (price = -100)
         ↓
Clicks "Save" button
         ↓
Local validation: ✓ Passes (or fails → show toast + return)
         ↓
Sends POST /api/quotations with { price: -100, ... }
         ↓
Backend validation: @DecimalMin(value = "0.01")
         ↓
MethodArgumentNotValidException caught by GlobalExceptionHandler
         ↓
Returns 400 with ErrorResponse:
{
  "success": false,
  "message": "Validation failed",
  "errors": ["price: Price must be greater than 0"]
}
         ↓
Frontend Response Interceptor passes error through
         ↓
Component catch block calls errorMapper.mapError()
         ↓
Detects isValidationError → Parse field errors
         ↓
Show toast: "Price must be greater than 0"
Show field highlight on price input
         ↓
User sees clean error message + highlighted field
```

### Example 2: Authentication Error Flow

```
User token expires
         ↓
Makes request with old/missing token
         ↓
Backend: @PreAuthorize or Spring Security rejects
         ↓
Returns 401 Unauthorized
         ↓
Frontend Response Interceptor detects status === 401
         ↓
Calls supabase.auth.signOut()
Redirects to /login
         ↓
User sees login page
```

### Example 3: Server Error Flow

```
User submits valid form
         ↓
Backend service throws RuntimeException
         ↓
GlobalExceptionHandler catches generic Exception
         ↓
Returns 500 with:
{
  "success": false,
  "message": "Something went wrong. Please try again later"
}
         ↓
Frontend catches error
         ↓
errorMapper.isServerError() → true
         ↓
Show user-friendly message (not raw exception)
Log full error to console for debugging
         ↓
User sees: "Server error. Please try again later."
Developer sees full error in console
```

---

## Component Integration Pattern

```javascript
// BEFORE: Basic error handling
try {
  await api.post('/endpoint', data)
  toast.success('Done')
} catch (err) {
  toast.error(err?.response?.data?.message || 'Error')
}

// AFTER: Production error handling
const handleSubmit = async () => {
  setErrors({})                          // 1. Clear previous errors
  const validationErrors = validate()    // 2. Local validation
  if (Object.keys(validationErrors).length > 0) {
    setErrors(validationErrors)           // 3. Show field errors
    toastService.error('Please fix errors')
    return
  }

  setSaving(true)
  const toastId = toastService.loading('Saving...')  // 4. Loading state

  try {
    const res = await api.post('/endpoint', data)     // 5. API call
    toastService.dismiss(toastId)                     // 6. Dismiss loading
    toastService.success('✅ Saved!')                  // 7. Success toast
    // 8. Navigate or update state
  } catch (error) {
    toastService.dismiss(toastId)                     // 9. Dismiss loading
    const mapped = errorMapper.mapError(error)

    // 10. Handle different error types
    if (errorMapper.isValidationError(error)) {
      const fieldErrors = {}
      mapped.errors.forEach(err => {
        const [field, msg] = err.split(':')
        fieldErrors[field.trim()] = msg.trim()
      })
      setErrors(fieldErrors)
    }

    toastService.error(mapped.message)                // 11. Error toast
  } finally {
    setSaving(false)                                  // 12. Clear loading state
  }
}
```

---

## Files & Their Responsibilities

### Frontend

| File | Purpose |
|------|---------|
| `services/api.js` | Axios instance + request/response interceptors |
| `services/toastService.js` | Toast notification API |
| `services/errorMapper.js` | Error parsing & user-friendly messages |
| `hooks/useApiCall.js` | Auto error handling for API calls |
| `hooks/useFormSubmit.js` | Auto error handling for form submissions |
| `utils/index.js` | Utility functions (uses errorMapper) |

### Backend

| File | Purpose |
|------|---------|
| `GlobalExceptionHandler.java` | Catches all exceptions, returns structured errors |
| `ErrorResponse.java` | Structured error response class |
| `BusinessException.java` | Custom exception for business logic errors |
| `ResourceNotFoundException.java` | Custom exception for 404 errors |

---

## Error Type Handling Summary

| Error Type | HTTP | Handler | Frontend Action |
|------------|------|---------|-----------------|
| Validation | 400 | MethodArgumentNotValidException | Parse fields, highlight inputs |
| Business Logic | 400 | BusinessException | Show message toast |
| Unauthorized | 401 | Spring Security | Logout, redirect to login |
| Forbidden | 403 | AccessDeniedException | Show permission denied message |
| Not Found | 404 | ResourceNotFoundException | Show not found message |
| Server Error | 500 | Generic Exception | Show generic error message |
| Network Error | N/A | Axios | Show network error message |
| Timeout | N/A | Axios | Show timeout message |

---

## Key Features Implemented

✅ **Global Error Handling**
- Single source of truth for error mapping
- Consistent error messages across app

✅ **Structured Error Responses**
- Backend returns JSON in predictable format
- Frontend can parse reliably

✅ **User-Friendly Messages**
- No raw Java exceptions shown
- Clear, actionable error messages

✅ **Field-Level Validation**
- Validation errors mapped to form fields
- User knows exactly what's wrong

✅ **Multiple Error Types**
- Validation, auth, permission, not found, server errors
- Each handled appropriately

✅ **Loading States**
- Toast loading shows progress
- Dismissed before showing result
- No duplicate toasts

✅ **Automatic Token Injection**
- Auth header added to all requests
- 401 handled automatically

✅ **Request/Response Logging**
- All API calls logged in console
- Helps with debugging

✅ **Network Error Handling**
- Timeout errors detected
- Offline detection
- User-friendly messages

✅ **Scalable & Maintainable**
- Reusable hooks
- Consistent patterns
- Easy to add to new components

---

## Testing Checklist

- [ ] Test validation errors (400)
- [ ] Test unauthorized (401)
- [ ] Test forbidden (403)
- [ ] Test not found (404)
- [ ] Test server error (500)
- [ ] Test network disconnect
- [ ] Test request timeout
- [ ] Test successful request
- [ ] Test form with multiple field errors
- [ ] Test loading state

---

## Next Steps

1. **Review all documentation** - Start with `COMPLETE_ERROR_HANDLING_REFERENCE.md`
2. **Study the examples** - Look at `QuotationFormExample.jsx` and `QuotationFormPageRefactored.jsx`
3. **Update existing components** - Use patterns from examples
4. **Add validation annotations** - Update your DTOs with `@NotBlank`, `@Min`, etc.
5. **Test error scenarios** - Use test endpoint to verify error handling
6. **Monitor in production** - Check browser console for logged errors

---

## Support & Debugging

### Check if error handling works:
```javascript
// In browser console
import { errorMapper } from './src/services'

// Simulate API error
const mockError = new Error('Test')
mockError.response = {
  status: 400,
  data: {
    message: 'Validation failed',
    errors: ['price: Price must be greater than 0']
  }
}

console.log(errorMapper.mapError(mockError))
```

### See logged API calls:
- Open browser DevTools → Console
- Look for ✅ and ❌ logs
- Each shows URL, method, status

### Verify interceptor is active:
- Any API call should have `Authorization: Bearer <token>`
- Check Network tab → Headers → Authorization

### Test backend error response:
```bash
curl -X GET http://localhost:8090/api/test/error/validation
# Should return structured error JSON
```

