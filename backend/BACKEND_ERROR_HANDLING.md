# Backend Error Handling Guide - Spring Boot

Complete guide for implementing structured error responses and exception handling in your Spring Boot API.

## Overview

All API errors are caught by `GlobalExceptionHandler` and converted to structured JSON responses that the frontend can easily parse.

## Error Response Format

All errors follow this structure:

```json
{
  "success": false,
  "message": "Validation failed",
  "errors": ["field: error message"],
  "timestamp": "2024-04-12T10:30:00Z"
}
```

## HTTP Status Codes

| Code | Scenario | Message |
|------|----------|---------|
| 200 | Success | Data returned |
| 400 | Validation/Business error | "Validation failed" |
| 401 | Unauthorized | "Invalid credentials" |
| 403 | Access denied | "You don't have permission..." |
| 404 | Resource not found | "Quotation not found with id: 123" |
| 500 | Server error | "Something went wrong..." |

## Usage Examples

### 1. Validation Errors

When using `@Valid` on request body:

```java
@PostMapping
public ResponseEntity<ApiResponse<QuotationDTO>> create(
    @Valid @RequestBody CreateQuotationRequest request
) {
    // If validation fails:
    // → GlobalExceptionHandler catches MethodArgumentNotValidException
    // → Returns 400 with:
    // {
    //   "success": false,
    //   "message": "Validation failed",
    //   "errors": [
    //     "customerName: Customer name is required",
    //     "price: Price must be greater than 0"
    //   ]
    // }
    
    Quotation quotation = quotationService.create(request);
    return ResponseEntity.ok(ApiResponse.ok("Quotation created", quotation));
}
```

Add validation annotations to request DTOs:

```java
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

### 2. Business Logic Errors

Throw custom exceptions in service layer:

```java
@Service
public class QuotationService {
    
    public Quotation create(CreateQuotationRequest request) {
        // Validate business logic
        if (request.getPrice() <= 0) {
            throw new BusinessException("Price must be greater than 0");
        }
        
        if (request.getQuantity() < 1) {
            throw new BusinessException("Quantity must be at least 1");
        }
        
        // Proceed with creation
        Quotation quotation = new Quotation();
        // ... set properties
        return quotationRepository.save(quotation);
    }
    
    public Quotation getById(Long id) {
        return quotationRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Quotation", id));
    }
}
```

### 3. Security Errors

Handled automatically:

```java
@GetMapping("/{id}")
@PreAuthorize("@quotationService.isOwner(#id, authentication.principal.id)")
public ResponseEntity<ApiResponse<QuotationDTO>> getQuotation(@PathVariable Long id) {
    // If @PreAuthorize fails:
    // → GlobalExceptionHandler catches AccessDeniedException
    // → Returns 403 with:
    // {
    //   "success": false,
    //   "message": "You don't have permission to access this resource"
    // }
    
    Quotation quotation = quotationService.getById(id);
    return ResponseEntity.ok(ApiResponse.ok(quotation));
}
```

### 4. Path/Query Parameter Validation

Use `@Valid` on path/query parameters:

```java
@GetMapping("/{id}")
public ResponseEntity<ApiResponse<QuotationDTO>> get(
    @PathVariable @Min(1) Long id
) {
    // If id < 1:
    // → GlobalExceptionHandler catches ConstraintViolationException
    // → Returns 400 with validation error
    
    return ResponseEntity.ok(ApiResponse.ok(quotationService.getById(id)));
}
```

## GlobalExceptionHandler Mapping

```
MethodArgumentNotValidException  → 400 + field errors
ConstraintViolationException     → 400 + validation errors
BusinessException                → 400 + message
ResourceNotFoundException         → 404 + message
BadCredentialsException          → 401 + "Invalid credentials"
AccessDeniedException            → 403 + "You don't have permission..."
Exception (generic)              → 500 + "Something went wrong..."
```

## Creating Custom Exceptions

```java
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
    
    public ResourceNotFoundException(String resource, Object id) {
        super(resource + " not found with id: " + id);
    }
}
```

Then throw them:

```java
throw new BusinessException("Price must be greater than 0");
throw new ResourceNotFoundException("Quotation", id);
```

## Best Practices

### ✅ DO

1. **Validate at entry point** - Use `@Valid` on controllers
2. **Use meaningful messages** - "Price must be greater than 0" not "Invalid price"
3. **Include field names** - "customerName: required" is better than just "required"
4. **Log errors** - Always log exceptions for debugging
5. **Return structured responses** - Always use ErrorResponse or ApiResponse
6. **Handle all status codes** - 400, 401, 403, 404, 500

### ❌ DON'T

1. **Expose stack traces** - Never send raw exception details to client
2. **Use generic messages** - Be specific about what went wrong
3. **Forget authentication** - Always validate user permissions
4. **Mix error formats** - Keep all errors in same structure
5. **Swallow exceptions** - Always log and handle properly

## Testing Error Responses

Create a test endpoint:

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
        }
    }
}
```

Test with:

```bash
# 400 Bad Request
curl http://localhost:8090/api/test/error/validation

# 404 Not Found
curl http://localhost:8090/api/test/error/notfound

# 403 Forbidden
curl http://localhost:8090/api/test/error/forbidden

# 500 Server Error
curl http://localhost:8090/api/test/error/server
```

## File References

- **GlobalExceptionHandler**: `backend/src/main/java/com/nursery/common/exception/GlobalExceptionHandler.java`
- **ErrorResponse**: `backend/src/main/java/com/nursery/common/response/ErrorResponse.java`
- **ApiResponse**: `backend/src/main/java/com/nursery/common/response/ApiResponse.java`
- **Custom Exceptions**: `backend/src/main/java/com/nursery/common/exception/`

## Frontend Integration

The frontend's `errorMapper.js` will parse these responses and convert to user-friendly messages.

See `frontend/src/ERROR_HANDLING_GUIDE.md` for frontend implementation details.

