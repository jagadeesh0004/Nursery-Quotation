# Implementation Checklist & Setup Guide

Complete step-by-step guide to integrate the production-level error handling system.

## ✅ Phase 1: Backend Setup (5 minutes)

### Backend Files Created/Modified

- [x] **ErrorResponse.java** - Structured error response class
  - Location: `backend/src/main/java/com/nursery/common/response/ErrorResponse.java`
  - Purpose: Standardized error format for all API errors

- [x] **GlobalExceptionHandler.java** - Updated exception handling
  - Location: `backend/src/main/java/com/nursery/common/exception/GlobalExceptionHandler.java`
  - Changes:
    - Handles MethodArgumentNotValidException (request body validation)
    - Handles ConstraintViolationException (path/query param validation)
    - Returns structured ErrorResponse for all errors
    - Maps HTTP status codes: 400, 401, 403, 404, 500

### Backend Validation Annotations

Use these annotations in your DTOs:

```java
import jakarta.validation.constraints.*;

public class CreateQuotationRequest {
    @NotBlank(message = "Customer name is required")
    private String customerName;
    
    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    private BigDecimal price;
    
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;
}
```

Remember to add `@Valid` in controller:

```java
@PostMapping
public ResponseEntity<ApiResponse<QuotationDTO>> create(
    @Valid @RequestBody CreateQuotationRequest request
) {
    // Validation happens automatically
}
```

---

## ✅ Phase 2: Frontend Setup (10 minutes)

### Step 1: Verify Dependencies

Check `frontend/package.json` - you should already have:

```json
{
  "dependencies": {
    "axios": "^1.6.7",
    "react-hot-toast": "^2.4.1",
    "react": "^18.2.0"
  }
}
```

If missing, run:
```bash
npm install react-hot-toast axios
```

### Step 2: Frontend Files Created

Files have been created in:

```
frontend/src/
├── services/
│   ├── api.js                    # ✅ Updated - Global interceptors
│   ├── toastService.js           # ✅ New - Toast API
│   ├── errorMapper.js            # ✅ New - Error mapping
│   └── index.js                  # ✅ Updated - Exports
├── hooks/
│   ├── useApiCall.js             # ✅ New - API call hook
│   └── useFormSubmit.js          # ✅ New - Form submission hook
├── utils/
│   └── index.js                  # ✅ Updated - Uses errorMapper
└── components/
    └── examples/
        ├── QuotationFormExample.jsx           # ✅ New - Basic example
        └── QuotationFormPageRefactored.jsx    # ✅ New - Real-world example
```

### Step 3: Verify Toast Provider

Your `App.jsx` already has the Toaster component (no changes needed):

```jsx
<Toaster
  position="bottom-right"
  gutter={8}
  toastOptions={{
    duration: 3500,
    style: { /* ... */ },
    success: { iconTheme: { primary: '#06D001', secondary: '#fff' } },
    error:   { iconTheme: { primary: '#ef4444', secondary: '#fff' } },
  }}
/>
```

---

## ✅ Phase 3: Integration into Existing Components

### Option A: Quick Update to QuotationFormPage

Update your existing `QuotationFormPage.jsx` to use the new error handling:

**Before:**
```javascript
import toast from 'react-hot-toast'
import { quotationService, plantService } from '../services'
import { getError } from '../utils'

// In useEffect
plantService.list()
  .then(r => setPlants(r.data.data || []))
  .catch((err) => {
    const message = getError(err)
    if (message) toast.error(message)
  })
```

**After:**
```javascript
import { toastService, errorMapper, quotationService, plantService } from '../services'

// In useEffect
const loadPlants = async () => {
  try {
    const res = await plantService.list()
    setPlants(res.data.data || [])
  } catch (error) {
    toastService.error(errorMapper.getToastMessage(error))
    console.error('Failed to load plants:', error)
  }
}

loadPlants()
```

**For form submission:**

**Before:**
```javascript
async function handleSave(shouldFinalize = false) {
  const currentErrs = validate(shouldFinalize)
  if (Object.keys(currentErrs).length > 0) {
    toast.error('Please fix the highlighted errors')
    return
  }
  
  setSaving(true)
  try {
    if (isEdit) res = await quotationService.update(id, payload)
    else        res = await quotationService.create(payload)
    toast.success('Saved!')
  } catch (err) {
    const message = getError(err)
    if (message) toast.error(message)
  }
  finally { setSaving(false) }
}
```

**After:**
```javascript
async function handleSave(shouldFinalize = false) {
  const currentErrs = validate(shouldFinalize)
  if (Object.keys(currentErrs).length > 0) {
    toastService.error('Please fix the errors below')
    return
  }
  
  setSaving(true)
  const toastId = toastService.loading('Saving...')
  
  try {
    if (isEdit) res = await quotationService.update(id, payload)
    else        res = await quotationService.create(payload)
    
    toastService.dismiss(toastId)
    toastService.success('✅ Saved successfully!')
    navigate(`/quotations/${res.data.data?.id || id}`)
  } catch (error) {
    toastService.dismiss(toastId)
    
    const mapped = errorMapper.mapError(error)
    
    // Handle validation errors
    if (errorMapper.isValidationError(error)) {
      const fieldErrors = {}
      mapped.errors.forEach(err => {
        const [field, msg] = err.split(':')
        fieldErrors[field.trim()] = msg.trim()
      })
      setErrors(fieldErrors)
      toastService.error(mapped.errors[0] || 'Validation failed')
    } else {
      toastService.error(mapped.message)
    }
  } finally {
    setSaving(false)
  }
}
```

### Option B: Use the Refactored Example

Copy logic from `QuotationFormPageRefactored.jsx` to your actual `QuotationFormPage.jsx`.

---

## ✅ Phase 4: Testing Error Scenarios

### Create Test Endpoint (Backend)

Add to any controller:

```java
@RestController
@RequestMapping("/api/test")
public class TestErrorController {
    
    @GetMapping("/error/{type}")
    public void testError(@PathVariable String type) {
        switch(type) {
            case "validation":
                throw new BusinessException("Price must be greater than 0");
            case "notfound":
                throw new ResourceNotFoundException("Quotation", 999);
            case "forbidden":
                throw new AccessDeniedException("You cannot access this");
            case "server":
                throw new RuntimeException("Unexpected error");
            default:
                throw new RuntimeException("Unknown test type");
        }
    }
}
```

### Test in Browser

```bash
# Test validation error (400)
curl http://localhost:8090/api/test/error/validation

# Test not found (404)
curl http://localhost:8090/api/test/error/notfound

# Test forbidden (403)
curl http://localhost:8090/api/test/error/forbidden

# Test server error (500)
curl http://localhost:8090/api/test/error/server
```

### Test in Frontend Console

```javascript
// Import services
import { quotationService, errorMapper, toastService } from '../services'

// Test async error handling
(async () => {
  try {
    // This should fail (doesn't exist)
    await quotationService.getOne(99999)
  } catch (error) {
    const mapped = errorMapper.mapError(error)
    console.log('Error:', mapped)
    toastService.error(mapped.message)
  }
})()
```

---

## ✅ Phase 5: Component Examples

### Example 1: Simple List with Delete

```javascript
import { useState, useEffect } from 'react'
import { toastService, errorMapper, quotationService } from '../services'

function QuotationsList() {
  const [quotations, setQuotations] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    loadQuotations()
  }, [])

  const loadQuotations = async () => {
    try {
      const res = await quotationService.list()
      setQuotations(res.data.data)
    } catch (error) {
      toastService.error(errorMapper.getToastMessage(error))
      console.error('Load error:', error)
    } finally {
      setLoading(false)
    }
  }

  const handleDelete = async (id) => {
    const toastId = toastService.loading('Deleting...')
    try {
      await quotationService.delete(id)
      toastService.dismiss(toastId)
      toastService.success('Quotation deleted')
      setQuotations(prev => prev.filter(q => q.id !== id))
    } catch (error) {
      toastService.dismiss(toastId)
      const mapped = errorMapper.mapError(error)
      
      if (errorMapper.isForbiddenError(error)) {
        toastService.error('You cannot delete this quotation')
      } else {
        toastService.error(mapped.message)
      }
    }
  }

  if (loading) return <p>Loading...</p>

  return (
    <div>
      {quotations.map(q => (
        <div key={q.id}>
          <p>{q.customerName}</p>
          <button onClick={() => handleDelete(q.id)}>Delete</button>
        </div>
      ))}
    </div>
  )
}
```

### Example 2: Form with Field-Level Errors

```javascript
import { useState } from 'react'
import { useFormSubmit } from '../hooks/useFormSubmit'
import { toastService, errorMapper, quotationService } from '../services'

function QuotationForm() {
  const [formData, setFormData] = useState({ name: '', price: '' })
  const [errors, setErrors] = useState({})
  const { submit } = useFormSubmit(quotationService.create)

  const handleSubmit = async (e) => {
    e.preventDefault()
    setErrors({})

    try {
      await submit(formData, {
        successMessage: 'Quotation created!',
        onSuccess: (data) => {
          console.log('Created:', data)
          setFormData({ name: '', price: '' })
        }
      })
    } catch (error) {
      const mapped = errorMapper.mapError(error)
      
      // Map backend field errors to form fields
      if (mapped.errors) {
        const fieldErrors = {}
        mapped.errors.forEach(err => {
          const [field, message] = err.split(':')
          fieldErrors[field.trim().toLowerCase()] = message.trim()
        })
        setErrors(fieldErrors)
      }
    }
  }

  return (
    <form onSubmit={handleSubmit}>
      <input
        value={formData.name}
        onChange={e => {
          setFormData({ ...formData, name: e.target.value })
          delete errors.name
        }}
        className={errors.name ? 'border-red-500' : ''}
      />
      {errors.name && <p className="text-red-500">{errors.name}</p>}
      
      <input
        value={formData.price}
        onChange={e => {
          setFormData({ ...formData, price: e.target.value })
          delete errors.price
        }}
        className={errors.price ? 'border-red-500' : ''}
      />
      {errors.price && <p className="text-red-500">{errors.price}</p>}
      
      <button type="submit">Save</button>
    </form>
  )
}
```

### Example 3: Using useApiCall Hook

```javascript
import { useApiCall } from '../hooks/useApiCall'
import { quotationService } from '../services'

function QuotationEditor({ id }) {
  const { execute, loading, error, data } = useApiCall(
    (payload) => quotationService.update(id, payload)
  )

  const handleUpdate = async () => {
    const result = await execute(formData, {
      successMessage: 'Quotation updated!',
      errorMessage: 'Failed to update'
    })
    
    if (result) {
      console.log('Updated:', result)
    }
  }

  return (
    <>
      <button onClick={handleUpdate} disabled={loading}>
        {loading ? 'Updating...' : 'Update'}
      </button>
      {error && <p className="text-red-500">{error.message}</p>}
    </>
  )
}
```

---

## ✅ Phase 6: Best Practices Checklist

When implementing error handling across your app:

- [ ] Always use `@Valid` on request DTOs
- [ ] Add meaningful validation messages in annotations
- [ ] Use `toastService` for user notifications
- [ ] Use `errorMapper` to parse errors
- [ ] Show loading state during API calls
- [ ] Dismiss loading toast before showing result
- [ ] Never show raw exception messages to users
- [ ] Handle each error type (401, 403, 400, 404, 500)
- [ ] Log errors in console for debugging
- [ ] Test offline scenarios
- [ ] Test timeout scenarios
- [ ] Use consistent error handling across all components

---

## ✅ Documentation Files Created

1. **COMPLETE_ERROR_HANDLING_REFERENCE.md** - Quick reference guide
2. **frontend/src/ERROR_HANDLING_GUIDE.md** - Frontend detailed guide
3. **backend/BACKEND_ERROR_HANDLING.md** - Backend detailed guide

---

## ✅ Code Files Created/Modified

### Backend
- ✅ `backend/src/main/java/com/nursery/common/response/ErrorResponse.java` (NEW)
- ✅ `backend/src/main/java/com/nursery/common/exception/GlobalExceptionHandler.java` (UPDATED)

### Frontend
- ✅ `frontend/src/services/api.js` (UPDATED)
- ✅ `frontend/src/services/toastService.js` (NEW)
- ✅ `frontend/src/services/errorMapper.js` (NEW)
- ✅ `frontend/src/services/index.js` (UPDATED)
- ✅ `frontend/src/hooks/useApiCall.js` (NEW)
- ✅ `frontend/src/hooks/useFormSubmit.js` (NEW)
- ✅ `frontend/src/utils/index.js` (UPDATED)
- ✅ `frontend/src/components/examples/QuotationFormExample.jsx` (NEW)
- ✅ `frontend/src/components/examples/QuotationFormPageRefactored.jsx` (NEW)

---

## 🚀 Next Steps

1. **Review the examples** in `frontend/src/components/examples/`
2. **Update QuotationFormPage** to use new error handling
3. **Gradually update other components** using the patterns shown
4. **Add validation annotations** to your request DTOs
5. **Test error scenarios** using the test endpoint

---

## 📞 Quick Reference

### To show a toast:
```javascript
toastService.success('Done!')
toastService.error('Failed')
```

### To handle API errors:
```javascript
try {
  await apiCall()
} catch (error) {
  toastService.error(errorMapper.getToastMessage(error))
}
```

### To show validation errors:
```javascript
const mapped = errorMapper.mapError(error)
if (mapped.errors) {
  mapped.errors.forEach(err => console.log(err))
}
```

### To check error type:
```javascript
if (errorMapper.isValidationError(error)) { /* ... */ }
if (errorMapper.isAuthError(error)) { /* ... */ }
if (errorMapper.isForbiddenError(error)) { /* ... */ }
```

---

## 📚 Full Documentation

- See `frontend/src/ERROR_HANDLING_GUIDE.md` for complete frontend guide
- See `backend/BACKEND_ERROR_HANDLING.md` for complete backend guide
- See `COMPLETE_ERROR_HANDLING_REFERENCE.md` for quick reference

