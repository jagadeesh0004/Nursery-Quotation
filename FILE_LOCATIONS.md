# 📂 File Locations Reference

Quick reference for all created files and their locations.

## 📍 Backend Files

```
backend/
└── src/main/java/com/nursery/common/
    ├── response/
    │   └── ErrorResponse.java (NEW) ✨
    │       • Structured error response class
    │       • Used by all API error endpoints
    │
    └── exception/
        └── GlobalExceptionHandler.java (UPDATED) 🔄
            • Enhanced with ConstraintViolationException handling
            • Returns ErrorResponse instead of ApiResponse for errors
```

## 📍 Frontend Code Files

```
frontend/src/
│
├── services/
│   ├── api.js (UPDATED) 🔄
│   │   • Enhanced request/response interceptors
│   │   • Handles 401 redirect automatically
│   │
│   ├── toastService.js (NEW) ✨
│   │   • Centralized toast notification API
│   │   • success(), error(), warning(), info(), loading()
│   │
│   ├── errorMapper.js (NEW) ✨
│   │   • Converts errors to user-friendly messages
│   │   • Type checkers: isValidationError(), isAuthError(), etc.
│   │
│   └── index.js (UPDATED) 🔄
│       • Exports: toastService, errorMapper
│
├── hooks/
│   ├── useApiCall.js (NEW) ✨
│   │   • Hook for API calls with auto error handling
│   │   • Returns: execute, loading, error, data
│   │
│   └── useFormSubmit.js (NEW) ✨
│       • Hook for form submissions
│       • Callbacks: onSuccess, onError
│
├── utils/
│   └── index.js (UPDATED) 🔄
│       • Updated getError() to use errorMapper
│
└── components/
    └── examples/
        ├── QuotationFormExample.jsx (NEW) ✨
        │   • Basic error handling example
        │   • Shows simple form pattern
        │
        └── QuotationFormPageRefactored.jsx (NEW) ✨
            • Production-ready example
            • Shows all error handling patterns
            • Copy this structure for real components
```

## 📍 Documentation Files

```
quotation_project/ (root)
│
├── DOCUMENTATION_INDEX.md ⭐ START HERE
│   • Navigation hub for all documentation
│   • Links and recommendations by role/need
│   • Time estimates for each document
│
├── QUICK_START_PATTERNS.md
│   • 8 copy-paste code patterns
│   • 5-minute quick start
│
├── IMPLEMENTATION_CHECKLIST.md
│   • Step-by-step setup (6 phases)
│   • Before/after code examples
│   • Testing instructions
│
├── ARCHITECTURE_GUIDE.md
│   • System architecture diagram
│   • Data flow examples
│   • Component integration pattern
│
├── COMPLETE_ERROR_HANDLING_REFERENCE.md
│   • All patterns and concepts
│   • Services & hooks reference
│   • Error response examples
│
├── IMPLEMENTATION_SUMMARY.md
│   • Overview of what was built
│   • Feature checklist
│   • Common questions (FAQ)
│
├── DELIVERY_SUMMARY.md
│   • Delivery checklist
│   • File summaries
│   • What you received
│
├── backend/
│   └── BACKEND_ERROR_HANDLING.md
│       • Backend implementation guide
│       • Validation annotations
│       • Error response format
│
└── frontend/src/
    └── ERROR_HANDLING_GUIDE.md
        • Frontend implementation guide
        • Usage examples
        • Integration patterns
```

## 📊 File Counts

**Backend:** 2 files (1 new, 1 updated)
**Frontend Code:** 9 files (7 new, 2 updated)
**Documentation:** 8 files (all new)
**Total:** 19 new + 3 updated = 22 files

---

## 🎯 Where to Find What

### "I need to show a toast"
→ `frontend/src/services/toastService.js`
→ OR `QUICK_START_PATTERNS.md` Pattern #1

### "I need to handle an error"
→ `frontend/src/services/errorMapper.js`
→ OR `QUICK_START_PATTERNS.md` Pattern #2

### "I need a hook for API calls"
→ `frontend/src/hooks/useApiCall.js`
→ OR `QUICK_START_PATTERNS.md` Pattern #4

### "I need backend validation"
→ `backend/BACKEND_ERROR_HANDLING.md`
→ OR `QUICK_START_PATTERNS.md` Backend DTO Example

### "I need to update my component"
→ `frontend/src/components/examples/QuotationFormExample.jsx`
→ OR `QUICK_START_PATTERNS.md`

### "I need to understand the system"
→ `ARCHITECTURE_GUIDE.md`

### "I need step-by-step setup"
→ `IMPLEMENTATION_CHECKLIST.md`

### "I need copy-paste code"
→ `QUICK_START_PATTERNS.md`

### "I need all patterns"
→ `COMPLETE_ERROR_HANDLING_REFERENCE.md`

---

## 🗺️ Quick Access

### Import These Services
```javascript
// From frontend/src/services/index.js
import { toastService, errorMapper } from '../services'
import { quotationService, plantService } from '../services'
```

### Import These Hooks
```javascript
// From frontend/src/hooks/
import { useApiCall } from '../hooks/useApiCall'
import { useFormSubmit } from '../hooks/useFormSubmit'
```

### Import Base API
```javascript
// From frontend/src/services/
import api from '../services/api'
```

---

## 📋 File Organization

### By Type

**Services** (4 files in `frontend/src/services/`)
- api.js - Axios instance
- toastService.js - Notifications
- errorMapper.js - Error mapping
- index.js - Exports

**Hooks** (2 files in `frontend/src/hooks/`)
- useApiCall.js
- useFormSubmit.js

**Examples** (2 files in `frontend/src/components/examples/`)
- QuotationFormExample.jsx
- QuotationFormPageRefactored.jsx

**Backend** (2 files in `backend/src/main/java/com/nursery/common/`)
- response/ErrorResponse.java
- exception/GlobalExceptionHandler.java

**Documentation** (8 files in `quotation_project/` root and subdirs)
- See documentation map above

### By Purpose

**To Copy-Paste Code** 
→ `QUICK_START_PATTERNS.md` + Examples

**To Understand System**
→ `ARCHITECTURE_GUIDE.md` + Diagrams

**To Set Up Properly**
→ `IMPLEMENTATION_CHECKLIST.md` + Step-by-step

**To Implement Backend**
→ `BACKEND_ERROR_HANDLING.md` + ErrorResponse.java + GlobalExceptionHandler.java

**To Implement Frontend**
→ `ERROR_HANDLING_GUIDE.md` + Services + Hooks + Examples

---

## ✅ File Verification

### Check Backend Files Exist
```bash
# Windows PowerShell
Test-Path "backend\src\main\java\com\nursery\common\response\ErrorResponse.java"
Test-Path "backend\src\main\java\com\nursery\common\exception\GlobalExceptionHandler.java"
```

### Check Frontend Files Exist
```bash
# Windows PowerShell
Test-Path "frontend\src\services\toastService.js"
Test-Path "frontend\src\services\errorMapper.js"
Test-Path "frontend\src\hooks\useApiCall.js"
Test-Path "frontend\src\hooks\useFormSubmit.js"
```

### Check Documentation Files Exist
```bash
# Windows PowerShell
Get-ChildItem -Path "." -Filter "*.md" | Where-Object {$_.Name -match "(ERROR|ARCHITECTURE|IMPLEMENTATION|QUICK|DOCUMENTATION)"}
```

---

## 🔄 Updated vs New

### Updated Files (3)
- `backend/src/main/java/com/nursery/common/exception/GlobalExceptionHandler.java`
- `frontend/src/services/api.js`
- `frontend/src/services/index.js`
- `frontend/src/utils/index.js`

### New Files (19)
- All others listed above

---

## 🚀 Files You'll Use Most

### Day-to-Day Development
1. `toastService.js` - Show notifications
2. `errorMapper.js` - Handle errors
3. `useApiCall.js` - Make API calls
4. `QUICK_START_PATTERNS.md` - Copy patterns

### One-Time Setup
1. `GlobalExceptionHandler.java` - Backend errors
2. `ErrorResponse.java` - Error format
3. `IMPLEMENTATION_CHECKLIST.md` - Setup guide

### Reference
1. `ARCHITECTURE_GUIDE.md` - System design
2. `ERROR_HANDLING_GUIDE.md` - Frontend details
3. `BACKEND_ERROR_HANDLING.md` - Backend details

---

## 📞 Navigation Tips

**In VS Code:**
- Ctrl+P → Type filename
- Ctrl+Shift+F → Find in files
- Ctrl+G → Go to line

**Documentation:**
- Always start with `DOCUMENTATION_INDEX.md`
- It links to everything else

**Examples:**
- `QuotationFormPageRefactored.jsx` is your template
- Copy its structure for real components

---

## ✨ Start Here

1. Open: `DOCUMENTATION_INDEX.md` (in root)
2. Pick your learning path
3. Read recommended docs
4. Follow along with examples
5. Update your components

**You're all set!** 🚀

