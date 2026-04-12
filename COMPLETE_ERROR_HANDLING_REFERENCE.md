# Complete Error Handling & Toast System - Quick Reference

## 🚀 Quick Start

### 1. Basic Error Handling
```javascript
import { toastService, errorMapper } from '../services'

try {
  await api.post('/data', payload)
  toastService.success('Success!')
} catch (error) {
  toastService.error(errorMapper.getToastMessage(error))
}
```

### 2. With Loading State
```javascript
const toastId = toastService.loading('Saving...')
try {
  await apiCall()
  toastService.dismiss(toastId)
  toastService.success('Done!')
} catch (error) {
  toastService.dismiss(toastId)
  toastService.error(errorMapper.getToastMessage(error))
}
```

### 3. With Form Errors
```javascript
const mapped = errorMapper.mapError(error)

if (errorMapper.isValidationError(error)) {
  // Show field errors
  mapped.errors.forEach(err => {
    const [field, message] = err.split(':')
    setErrors(prev => ({ ...prev, [field.trim()]: message.trim() }))
  })
}
```

---

## 📚 Services Available

### toastService
```javascript
import { toastService } from '../services'

toastService.success(message, options)     // Green toast
toastService.error(message, options)       // Red toast
toastService.warning(message, options)     // Yellow toast
toastService.info(message, options)        // Blue toast
toastService.loading(message)              // Returns toastId
toastService.dismiss(toastId)              // Dismiss by ID
```

### errorMapper
```javascript
import { errorMapper } from '../services'

// Get main message
const message = errorMapper.getToastMessage(error)

// Get detailed mapping
const mapped = errorMapper.mapError(error)
// Returns: { message, errors[], statusCode, isNetwork }

// Check error type
errorMapper.isAuthError(error)              // 401
errorMapper.isForbiddenError(error)         // 403
errorMapper.isValidationError(error)        // 400 with errors
errorMapper.isNotFoundError(error)          // 404
errorMapper.isServerError(error)            // 500+
```

---

## 🎯 Hooks

### useApiCall - Auto error handling for API calls
```javascript
import { useApiCall } from '../hooks/useApiCall'

const { execute, loading, error, data } = useApiCall(apiFunction)

const result = await execute(payload, {
  successMessage: 'Created!',
  errorMessage: 'Failed',
  showToast: true  // default
})
```

### useFormSubmit - For form submissions
```javascript
import { useFormSubmit } from '../hooks/useFormSubmit'

const { submit } = useFormSubmit(apiFunction)

await submit(formData, {
  successMessage: 'Saved!',
  onSuccess: (data) => navigate('/list'),
  onError: (error) => console.log(error)
})
```

---

## 🏗️ File Structure

```
frontend/src/
├── services/
│   ├── api.js                 # Axios + interceptors
│   ├── toastService.js        # Toast API
│   ├── errorMapper.js         # Error mapping
│   └── index.js               # Exports
├── hooks/
│   ├── useApiCall.js          # API call hook
│   └── useFormSubmit.js       # Form submission hook
├── components/
│   └── examples/
│       ├── QuotationFormExample.jsx
│       └── QuotationFormPageRefactored.jsx
└── ERROR_HANDLING_GUIDE.md

backend/src/main/java/com/nursery/common/
├── exception/
│   ├── GlobalExceptionHandler.java
│   ├── BusinessException.java
│   └── ResourceNotFoundException.java
└── response/
    ├── ErrorResponse.java
    └── ApiResponse.java
```

---

## 📋 Common Patterns

### Pattern 1: Simple API Call
```javascript
try {
  const response = await quotationService.list()
  setQuotations(response.data.data)
} catch (error) {
  toastService.error(errorMapper.getToastMessage(error))
}
```

### Pattern 2: With Validation Response
```javascript
try {
  await quotationService.create(data)
  toastService.success('Created!')
} catch (error) {
  if (errorMapper.isValidationError(error)) {
    const { errors } = errorMapper.mapError(error)
    errors.forEach(err => console.log(err))
  } else {
    toastService.error(errorMapper.getToastMessage(error))
  }
}
```

### Pattern 3: With Loading State
```javascript
const handleSubmit = async () => {
  setLoading(true)
  const toastId = toastService.loading('Processing...')
  
  try {
    await apiCall()
    toastService.dismiss(toastId)
    toastService.success('Done!')
  } catch (error) {
    toastService.dismiss(toastId)
    toastService.error(errorMapper.getToastMessage(error))
  } finally {
    setLoading(false)
  }
}
```

### Pattern 4: Multiple Fields with Errors
```javascript
try {
  await api.post('/endpoint', data)
} catch (error) {
  const mapped = errorMapper.mapError(error)
  const fieldErrors = {}
  
  mapped.errors?.forEach(err => {
    const [field, message] = err.split(':')
    fieldErrors[field.trim()] = message.trim()
  })
  
  setErrors(fieldErrors)
  
  // Show first error in toast
  if (mapped.errors?.length > 0) {
    toastService.error(mapped.errors[0])
  }
}
```

---

## 🔍 Error Response Examples

### Success Response (200)
```json
{
  "success": true,
  "message": "Quotation created",
  "data": { "id": 1, "customerName": "John" },
  "timestamp": "2024-04-12T10:30:00Z"
}
```

### Validation Error (400)
```json
{
  "success": false,
  "message": "Validation failed",
  "errors": [
    "price: Price must be greater than 0",
    "quantity: Quantity must be positive"
  ],
  "timestamp": "2024-04-12T10:30:00Z"
}
```

### Unauthorized (401)
```json
{
  "success": false,
  "message": "Invalid credentials",
  "timestamp": "2024-04-12T10:30:00Z"
}
```

### Forbidden (403)
```json
{
  "success": false,
  "message": "You don't have permission to access this resource",
  "timestamp": "2024-04-12T10:30:00Z"
}
```

### Not Found (404)
```json
{
  "success": false,
  "message": "Quotation not found with id: 999",
  "timestamp": "2024-04-12T10:30:00Z"
}
```

### Server Error (500)
```json
{
  "success": false,
  "message": "Something went wrong. Please try again later",
  "timestamp": "2024-04-12T10:30:00Z"
}
```

---

## ✅ Validation Checklist

When implementing error handling:

- [ ] Use `@Valid` on request bodies
- [ ] Add meaningful validation messages
- [ ] Handle each HTTP status code
- [ ] Show loading state during API calls
- [ ] Dismiss loading toast before showing result
- [ ] Don't show raw backend errors
- [ ] Log errors in console for debugging
- [ ] Test with network disconnected
- [ ] Test with timeout scenarios
- [ ] Show field-level errors in forms

---

## 🐛 Debugging

### Enable API Logging
API interceptors already log all requests/responses:
```
✅ API Success: { url, status, method }
❌ API Error: { url, status, method, message }
```

### Check Error Object
```javascript
try {
  await apiCall()
} catch (error) {
  console.log('Full error:', error)
  console.log('Mapped error:', errorMapper.mapError(error))
}
```

### Test Error Scenarios
```javascript
// In browser console:

// Simulate offline
fetch('http://invalid-url/api/test').catch(e => console.log(e))

// Simulate timeout (set timeout: 100 in api.js first)
fetch('http://localhost:8090/api/plants')

// Simulate server error
fetch('http://localhost:8090/api/test/error/server')
```

---

## 🚀 Best Practices

1. **Validate Locally First** - Instant user feedback
2. **Use Loading Toast** - Dismiss before showing result
3. **Avoid Duplicate Toasts** - Use loading pattern
4. **Map All Errors** - Convert to user-friendly messages
5. **Log for Debugging** - Keep error details in console
6. **Handle by Status** - 401, 403, 400, 404, 500 differ
7. **Test Offline** - What happens without internet?
8. **Use Consistent Patterns** - All forms handle errors same way

---

## 📖 Full Documentation

- **Frontend**: See `frontend/src/ERROR_HANDLING_GUIDE.md`
- **Backend**: See `backend/BACKEND_ERROR_HANDLING.md`
- **Examples**: See `frontend/src/components/examples/`

