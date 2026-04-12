# Production-Level Error Handling & Toast Notifications

A complete implementation guide for error handling and user-friendly notifications in your React + Spring Boot stack.

## Table of Contents

1. [Backend Setup](#backend-setup)
2. [Frontend Setup](#frontend-setup)
3. [Error Mapping](#error-mapping)
4. [Usage Patterns](#usage-patterns)
5. [Testing Errors](#testing-errors)

---

## Backend Setup

### 1. Structured Error Response (ErrorResponse.java)

Location: `backend/src/main/java/com/nursery/common/response/ErrorResponse.java`

All API errors return a structured JSON response:

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

### 2. Global Exception Handler (GlobalExceptionHandler.java)

Location: `backend/src/main/java/com/nursery/common/exception/GlobalExceptionHandler.java`

Handles all exceptions globally:

- **400 Bad Request**: Validation errors, business logic errors
- **401 Unauthorized**: Invalid credentials, expired tokens
- **403 Forbidden**: Access denied
- **404 Not Found**: Resource not found
- **500 Server Error**: Unexpected errors

Example with Spring Data validation:

```java
@PostMapping
public ResponseEntity<ApiResponse<QuotationDTO>> create(
    @Valid @RequestBody CreateQuotationRequest request
) {
    // If validation fails, GlobalExceptionHandler catches MethodArgumentNotValidException
    // Returns: 400 with structured error response
    Quotation quotation = quotationService.create(request);
    return ResponseEntity.ok(ApiResponse.ok("Quotation created", quotation));
}
```

### 3. Custom Exceptions

Create and use custom exceptions in your service layer:

```java
// Throws 400 Bad Request
if (quotation.getPrice() <= 0) {
    throw new BusinessException("Price must be greater than 0");
}

// Throws 404 Not Found
quotationRepository.findById(id)
    .orElseThrow(() -> new ResourceNotFoundException("Quotation", id));
```

---

## Frontend Setup

### 1. Toast Service (`services/toastService.js`)

Centralized toast notification service:

```javascript
import { toastService } from '../services'

// Success
toastService.success('Item created successfully')

// Error
toastService.error('Failed to save item')

// Warning
toastService.warning('This action cannot be undone')

// Info
toastService.info('Processing your request')

// Loading (returns toastId for later dismissal)
const id = toastService.loading('Saving...')
toastService.dismiss(id)

// Promise-based
toastService.promise(
  apiCall(),
  {
    loading: 'Saving...',
    success: 'Saved!',
    error: 'Failed to save'
  }
)
```

### 2. Error Mapper (`services/errorMapper.js`)

Converts backend errors to user-friendly messages:

```javascript
import { errorMapper } from '../services'

try {
  await apiCall()
} catch (error) {
  const mapped = errorMapper.mapError(error)
  
  // Returns:
  // {
  //   message: "Price must be greater than 0",
  //   errors: ["price: Price must be greater than 0"],
  //   statusCode: 400,
  //   isNetwork: false
  // }

  if (errorMapper.isValidationError(error)) {
    // Show field-level errors
  } else if (errorMapper.isAuthError(error)) {
    // Handle 401
  } else if (errorMapper.isServerError(error)) {
    // Show generic error message
  }
}
```

### 3. API Interceptor (services/api.js)

Global request/response interceptor:

```javascript
// Automatically:
// ✅ Attaches auth token to all requests
// ✅ Logs all API calls
// ✅ Handles 401 (redirects to login)
// ❌ Does NOT automatically show error toasts (let components decide)
```

---

## Usage Patterns

### Pattern 1: Simple API Call with Toast

```javascript
import { quotationService, toastService, errorMapper } from '../services'

async function handleCreate(data) {
  try {
    await quotationService.create(data)
    toastService.success('Quotation created!')
    navigate('/quotations')
  } catch (error) {
    toastService.error(errorMapper.getToastMessage(error))
  }
}
```

### Pattern 2: Using useApiCall Hook

```javascript
import { useApiCall } from '../hooks/useApiCall'
import { quotationService } from '../services'

function MyComponent() {
  const { execute, loading, error, data } = useApiCall(quotationService.create)

  const handleSubmit = async () => {
    const result = await execute(formData, {
      successMessage: 'Quotation created!',
      errorMessage: 'Failed to create quotation'
    })
    
    if (result) {
      navigate('/quotations')
    }
  }

  return (
    <>
      <button onClick={handleSubmit} disabled={loading}>
        {loading ? 'Creating...' : 'Create'}
      </button>
      {error && <p className="text-red-500">{error.message}</p>}
    </>
  )
}
```

### Pattern 3: Form Submission with Validation

```javascript
import { useState } from 'react'
import { toastService, errorMapper } from '../services'
import { quotationService } from '../services'

function QuotationForm() {
  const [formData, setFormData] = useState({})
  const [errors, setErrors] = useState({})
  const [loading, setLoading] = useState(false)

  const handleSubmit = async (e) => {
    e.preventDefault()
    setErrors({}) // Clear previous errors

    // Local validation
    const validationErrors = validateForm()
    if (Object.keys(validationErrors).length > 0) {
      setErrors(validationErrors)
      toastService.error('Please fix the errors below')
      return
    }

    setLoading(true)
    const toastId = toastService.loading('Creating...')

    try {
      await quotationService.create(formData)
      toastService.dismiss(toastId)
      toastService.success('Created successfully!')
      resetForm()
    } catch (error) {
      toastService.dismiss(toastId)
      const mapped = errorMapper.mapError(error)

      // Show field errors if validation error
      if (errorMapper.isValidationError(error)) {
        const fieldErrors = {}
        mapped.errors.forEach((err) => {
          const [field] = err.split(':')
          fieldErrors[field] = err
        })
        setErrors(fieldErrors)
      }

      toastService.error(mapped.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <form onSubmit={handleSubmit}>
      <input
        value={formData.name}
        onChange={(e) => setFormData({ ...formData, name: e.target.value })}
      />
      {errors.name && <p className="text-red-500">{errors.name}</p>}
      <button type="submit" disabled={loading}>
        {loading ? 'Saving...' : 'Save'}
      </button>
    </form>
  )
}
```

### Pattern 4: Using useFormSubmit Hook

```javascript
import { useFormSubmit } from '../hooks/useFormSubmit'
import { quotationService } from '../services'

function QuotationForm() {
  const { submit } = useFormSubmit(quotationService.create)

  const handleSubmit = async (e) => {
    e.preventDefault()

    try {
      await submit(formData, {
        successMessage: 'Quotation created!',
        onSuccess: (data) => {
          navigate('/quotations')
        }
      })
    } catch (error) {
      // Error is already toasted by the hook
    }
  }

  return (
    <form onSubmit={handleSubmit}>
      {/* form fields */}
    </form>
  )
}
```

### Pattern 5: List Operations with Error Handling

```javascript
import { useState, useEffect } from 'react'
import { quotationService, toastService, errorMapper } from '../services'

function QuotationsList() {
  const [quotations, setQuotations] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    loadQuotations()
  }, [])

  const loadQuotations = async () => {
    try {
      const response = await quotationService.list()
      setQuotations(response.data.data)
    } catch (error) {
      console.error('Failed to load quotations:', error)
      toastService.error('Failed to load quotations')
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
      setQuotations((prev) => prev.filter((q) => q.id !== id))
    } catch (error) {
      toastService.dismiss(toastId)
      toastService.error(errorMapper.getToastMessage(error))
    }
  }

  if (loading) return <div>Loading...</div>

  return (
    <div>
      {quotations.map((q) => (
        <div key={q.id}>
          <p>{q.customerName}</p>
          <button onClick={() => handleDelete(q.id)}>Delete</button>
        </div>
      ))}
    </div>
  )
}
```

---

## Error Handling Checklist

When implementing error handling:

- [ ] Validate form inputs locally first (fast feedback)
- [ ] Send validation errors to backend (double validation)
- [ ] Catch backend validation errors and show in form
- [ ] Show loading state during API calls
- [ ] Dismiss loading toast before showing result toast
- [ ] Handle 401 (already automatic via interceptor)
- [ ] Don't show raw backend errors to users
- [ ] Test network disconnection scenarios
- [ ] Test timeout scenarios
- [ ] Avoid duplicate toasts (use loading toast pattern)

---

## Testing Errors

### Simulate Backend Errors

To test error handling, create a test endpoint in your controller:

```java
@GetMapping("/test-error/{type}")
public ResponseEntity<?> testError(@PathVariable String type) {
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
            return ResponseEntity.ok("No error");
    }
}
```

Test in browser console:

```javascript
// Simulate network error
fetch('http://invalid-url/api/test').catch(e => console.log(e))

// Simulate timeout (in services/api.js, temporarily set timeout: 100)
api.get('/quotations/list')
```

---

## File Structure

```
frontend/
  src/
    services/
      api.js                    # Axios instance + interceptors
      toastService.js           # Toast notifications
      errorMapper.js            # Error mapping utility
      index.js                  # Exports all services
    hooks/
      useApiCall.js             # Hook for API calls with error handling
      useFormSubmit.js           # Hook for form submission
    components/
      examples/
        QuotationFormExample.jsx # Complete example

backend/
  src/main/java/com/nursery/
    common/
      exception/
        GlobalExceptionHandler.java   # Global error handler
        BusinessException.java        # Custom exception
        ResourceNotFoundException.java # Custom exception
      response/
        ErrorResponse.java      # Structured error response
        ApiResponse.java        # Success response
```

---

## Best Practices

1. **Always validate locally first** - Provides instant feedback
2. **Use loading state** - Show user something is happening
3. **Avoid duplicate toasts** - Use loading toast + dismiss pattern
4. **Map all errors** - Convert backend errors to user-friendly messages
5. **Log for debugging** - Keep error details in console
6. **Handle each error type** - 401, 403, 400, 404, 500 need different handling
7. **Test offline** - What happens when user loses network?
8. **Use consistent patterns** - All forms should handle errors the same way

---

## References

- [react-hot-toast Documentation](https://react-hot-toast.com/)
- [Axios Interceptors](https://axios-http.com/docs/interceptors)
- [Spring Boot Exception Handling](https://spring.io/blog/2013/11/01/exception-handling-in-spring-mvc)

