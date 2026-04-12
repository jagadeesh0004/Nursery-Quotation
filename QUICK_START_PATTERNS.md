# 🚀 QUICK START CARD

## Copy-Paste These Patterns Into Your Code

### 1️⃣ Simple API Call
```javascript
import { toastService, errorMapper } from '../services'

try {
  const res = await quotationService.list()
  setData(res.data.data)
} catch (error) {
  toastService.error(errorMapper.getToastMessage(error))
}
```

### 2️⃣ With Loading State
```javascript
const toastId = toastService.loading('Loading...')
try {
  const res = await apiCall()
  toastService.dismiss(toastId)
  toastService.success('Done!')
  // use res.data
} catch (error) {
  toastService.dismiss(toastId)
  toastService.error(errorMapper.getToastMessage(error))
}
```

### 3️⃣ Form Submission
```javascript
const [errors, setErrors] = useState({})

const handleSubmit = async (e) => {
  e.preventDefault()
  setErrors({})
  
  // Local validation
  const errs = {}
  if (!name.trim()) errs.name = 'Required'
  if (Object.keys(errs).length > 0) {
    setErrors(errs)
    toastService.error('Please fix errors')
    return
  }

  const toastId = toastService.loading('Saving...')
  try {
    await apiCall({ name })
    toastService.dismiss(toastId)
    toastService.success('✅ Saved!')
  } catch (error) {
    toastService.dismiss(toastId)
    
    const mapped = errorMapper.mapError(error)
    
    if (errorMapper.isValidationError(error)) {
      const fieldErrors = {}
      mapped.errors.forEach(err => {
        const [field, msg] = err.split(':')
        fieldErrors[field.trim()] = msg.trim()
      })
      setErrors(fieldErrors)
    }
    
    toastService.error(mapped.message)
  }
}

return (
  <form onSubmit={handleSubmit}>
    <input value={name} onChange={e => {
      setName(e.target.value)
      delete errors.name
    }} />
    {errors.name && <p className="text-red-500">{errors.name}</p>}
    <button type="submit">Save</button>
  </form>
)
```

### 4️⃣ Using Hook
```javascript
import { useApiCall } from '../hooks/useApiCall'

const { execute, loading, error } = useApiCall(quotationService.create)

const handleCreate = async () => {
  const result = await execute(data, {
    successMessage: 'Created!',
    errorMessage: 'Failed'
  })
  if (result) navigate('/list')
}
```

### 5️⃣ Validation Errors Only
```javascript
try {
  await apiCall()
} catch (error) {
  if (errorMapper.isValidationError(error)) {
    const { errors } = errorMapper.mapError(error)
    // Handle field errors
  }
}
```

### 6️⃣ Auth Error
```javascript
try {
  await apiCall()
} catch (error) {
  if (errorMapper.isAuthError(error)) {
    // Already handled by interceptor, just show message
    toastService.error('Session expired. Please login again.')
  }
}
```

### 7️⃣ Server Error
```javascript
try {
  await apiCall()
} catch (error) {
  if (errorMapper.isServerError(error)) {
    toastService.error('Server error. Try again later.')
  }
}
```

### 8️⃣ Network Error
```javascript
try {
  await apiCall()
} catch (error) {
  const mapped = errorMapper.mapError(error)
  if (mapped.isNetwork) {
    toastService.error('Check your internet connection')
  }
}
```

---

## 🎯 Key Imports

```javascript
// Error handling
import { toastService, errorMapper } from '../services'

// Hooks
import { useApiCall } from '../hooks/useApiCall'
import { useFormSubmit } from '../hooks/useFormSubmit'

// Services
import { quotationService, plantService } from '../services'
```

---

## 📋 Always Do

✅ Local validation first  
✅ Show loading state  
✅ Dismiss loading before showing result  
✅ Map errors to user-friendly messages  
✅ Log errors in catch block  
✅ Show field errors in forms  

---

## ❌ Never Do

❌ Show raw exception messages  
❌ Forget to dismiss loading toast  
❌ Mix error handling patterns  
❌ Ignore validation errors  
❌ Skip error logging  

---

## 📚 Full Docs

- `COMPLETE_ERROR_HANDLING_REFERENCE.md` - All patterns
- `IMPLEMENTATION_CHECKLIST.md` - Step-by-step
- `ARCHITECTURE_GUIDE.md` - System design
- `frontend/src/ERROR_HANDLING_GUIDE.md` - Frontend details
- `backend/BACKEND_ERROR_HANDLING.md` - Backend details

---

## 🔧 Backend DTO Example

```java
public class CreateQuotationRequest {
    @NotBlank(message = "Customer name is required")
    private String customerName;
    
    @NotBlank(message = "Phone is required")
    private String customerPhone;
    
    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    private BigDecimal price;
    
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;
}
```

Remember: Add `@Valid` in controller!

```java
@PostMapping
public ResponseEntity<ApiResponse<QuotationDTO>> create(
    @Valid @RequestBody CreateQuotationRequest request
) {
    // Validation happens automatically
}
```

---

## 🎪 Toast Variations

```javascript
// Success
toastService.success('Quotation created!')

// Error
toastService.error('Failed to save')

// Warning
toastService.warning('This action cannot be undone')

// Info
toastService.info('Processing...')

// Loading (returns ID)
const id = toastService.loading('Saving...')
toastService.dismiss(id)  // Later, dismiss it

// Promise-based
toastService.promise(
  apiCall(),
  {
    loading: 'Saving...',
    success: 'Saved!',
    error: 'Failed'
  }
)
```

---

## 🚦 Error Type Checkers

```javascript
errorMapper.isValidationError(error)    // 400 with field errors
errorMapper.isAuthError(error)          // 401 Unauthorized
errorMapper.isForbiddenError(error)     // 403 Forbidden
errorMapper.isNotFoundError(error)      // 404 Not Found
errorMapper.isServerError(error)        // 500+ Server error
```

---

## 📱 Example Component

```javascript
import { useState } from 'react'
import { toastService, errorMapper, quotationService } from '../services'

export default function CreateQuotation() {
  const [name, setName] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError(null)

    if (!name.trim()) {
      setError('Name is required')
      toastService.error('Please enter a name')
      return
    }

    setLoading(true)
    const toastId = toastService.loading('Creating...')

    try {
      await quotationService.create({ name })
      toastService.dismiss(toastId)
      toastService.success('✅ Created!')
      setName('')
    } catch (err) {
      toastService.dismiss(toastId)
      toastService.error(errorMapper.getToastMessage(err))
      setError(errorMapper.mapError(err).message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      <div>
        <input
          value={name}
          onChange={(e) => {
            setName(e.target.value)
            setError(null)
          }}
          placeholder="Quotation name"
          className={`border ${error ? 'border-red-500' : 'border-gray-300'}`}
        />
        {error && <p className="text-red-500 text-sm mt-1">{error}</p>}
      </div>
      <button
        type="submit"
        disabled={loading}
        className="px-4 py-2 bg-blue-600 text-white rounded disabled:opacity-50"
      >
        {loading ? 'Creating...' : 'Create'}
      </button>
    </form>
  )
}
```

---

## 🎯 One-Liner Reference

```javascript
// Show toast
toastService.success('msg')              // Green
toastService.error(msg)                  // Red

// Get error message
errorMapper.getToastMessage(error)       // User-friendly

// Check error type
errorMapper.isValidationError(error)     // Bool
errorMapper.isAuthError(error)           // Bool

// Use hook
const { execute, loading } = useApiCall(fn)
await execute(data)

// With options
await execute(data, { successMessage: 'Done!' })
```

---

Done! Start with pattern #1 and go from there. 🚀

