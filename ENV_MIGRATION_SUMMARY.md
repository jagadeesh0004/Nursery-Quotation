# Environment Variables Migration Summary

## Overview
All hardcoded configuration values have been migrated from source code to `.env` files for both frontend and backend. This ensures sensitive data is not committed to version control and configuration can be easily managed per environment.

---

## 📋 What Was Changed

### ✅ Frontend Changes

#### 1. **Added to `.env` file:**
```dotenv
# Supabase Storage Prefix
VITE_SUPABASE_STORAGE_PREFIX=sb-ymmbujskqowztgffqlmz-

# Vite Dev Server Port
VITE_PORT=5173
```

#### 2. **Updated Files:**

**`frontend/src/context/AuthContext.jsx`**
- Changed hardcoded `SUPABASE_STORAGE_PREFIX = 'sb-ymmbujskqowztgffqlmz-'`
- Now reads from: `import.meta.env.VITE_SUPABASE_STORAGE_PREFIX || 'sb-ymmbujskqowztgffqlmz-'`
- Falls back to default if not set

**`frontend/vite.config.js`**
- Changed hardcoded ports `5173`
- Now reads from: `process.env.VITE_PORT` (default: `5173`)
- Applied to both `server.port` and `hmr.port`

#### 3. **Already Configured (No Changes Needed):**
- `frontend/src/lib/supabase.js` - Uses `import.meta.env.VITE_SUPABASE_URL` and `import.meta.env.VITE_SUPABASE_ANON_KEY`
- `frontend/src/constants/index.js` - Uses `import.meta.env.VITE_API_BASE_URL` with fallback

---

### ✅ Backend Changes

#### Already Properly Configured (No Changes Needed):

**`backend/src/main/resources/application.yml`**
- All values use Spring's environment variable syntax: `${VAR_NAME:default_value}`
- Examples:
  - `${SERVER_PORT:8091}`
  - `${SPRING_DATASOURCE_URL:jdbc:postgresql://...}`
  - `${FRONTEND_URL:http://localhost:5173}`

**Java Configuration Classes:**
- `WebConfig.java` - Uses `@Value("${app.frontend.url:http://localhost:5173}")`
- `SecurityConfig.java` - Uses `@Value("${app.cors.allowed-origins:...}")`

---

## 📁 Environment Files Reference

### Frontend `.env` Structure
```dotenv
# 1. SUPABASE CONFIGURATION
VITE_SUPABASE_URL=https://ymmbujskqowztgffqlmz.supabase.co
VITE_SUPABASE_ANON_KEY=eyJhbGci...

# 2. BACKEND API CONFIGURATION
VITE_API_BASE_URL=http://localhost:8091/api

# 3. SUPABASE STORAGE CONFIGURATION
VITE_SUPABASE_STORAGE_PREFIX=sb-ymmbujskqowztgffqlmz-

# 4. VITE DEV SERVER CONFIGURATION
VITE_PORT=5173
```

### Backend `.env` Structure
```dotenv
# 1. DATABASE CONFIGURATION
SPRING_DATASOURCE_URL=jdbc:postgresql://...
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=...

# 2. SUPABASE OAUTH2 CONFIGURATION
SUPABASE_JWKS_URI=https://...
SUPABASE_ISSUER=https://...
SUPABASE_PROJECT_ID=...
SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI=https://...

# 3. SERVER CONFIGURATION
SERVER_PORT=8091

# 4. FRONTEND CONFIGURATION
FRONTEND_URL=http://localhost:5173

# 5. CORS CONFIGURATION
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:5173,...

# 6. FILE UPLOAD CONFIGURATION
UPLOAD_DIR=./uploads
LOGOS_DIR=./uploads/logos
PDFS_DIR=./uploads/pdfs

# 7. PDF GENERATION CONFIGURATION
PDF_CACHE_ENABLED=false
```

---

## 🔒 Security Notes

### ✅ What's Protected:
1. **Never commit `.env` files** - They contain sensitive data
2. **Always use `.env.example`** - Template file for developers
3. **All secrets** - Supabase keys, database passwords, JWT secrets

### ✅ What's Safe to Commit:
- `.env.example` - Contains placeholders and documentation
- Configuration files with `${...}` syntax - No actual values
- Source code - Uses environment variables, not hardcoded values

### ✅ Recommended `.gitignore` Entries:
```gitignore
# Environment files (both frontend and backend)
.env
.env.local
.env.*.local

# Build directories
node_modules/
/backend/target/
/frontend/dist/

# Logs
*.log
logs/

# IDE
.vscode/
.idea/
*.swp
*.swo
```

---

## 🚀 How to Use

### Development Setup:

**Frontend:**
1. Copy `.env.example` to `.env`
2. Update values as needed
3. Dev server uses values from `.env`

**Backend:**
1. Copy `.env.example` to `.env` (or set environment variables)
2. Run Maven with environment variables loaded
3. Spring Boot automatically reads from environment

### Running Applications:

**Frontend (Development):**
```bash
cd frontend
npm install
npm run dev  # Uses VITE_PORT from .env
```

**Backend (Development):**
```bash
cd backend
# Load .env variables and run
export $(cat .env | xargs)
./mvnw spring-boot:run
```

### Production Deployment:

- **DO NOT** include `.env` files
- **DO** set environment variables in deployment platform:
  - Docker: Use `docker run -e VAR=value`
  - Kubernetes: Use ConfigMaps/Secrets
  - Cloud platforms: Use environment variable management UI

---

## 📝 Checklist

- [x] Frontend Supabase configuration moved to `.env`
- [x] Frontend API base URL uses environment variable
- [x] Frontend storage prefix uses environment variable
- [x] Frontend dev server port uses environment variable
- [x] Backend database configuration uses environment variables
- [x] Backend OAuth2 configuration uses environment variables
- [x] Backend CORS configuration uses environment variables
- [x] Backend server port uses environment variable
- [x] All `.env.example` files properly documented
- [x] No hardcoded secrets in source code
- [x] All values have sensible defaults where appropriate

---

## 🔍 Verification

To verify all hardcoded values have been migrated:

```bash
# Frontend: Check for hardcoded ports/URLs
grep -r "localhost:[0-9]\|supabase\.co\|ymmbujskqowztgffqlmz" \
  frontend/src/ \
  --exclude-dir=node_modules \
  --exclude-dir=dist

# Backend: Check for hardcoded values (should only be defaults in @Value annotations)
grep -r "localhost\|8091\|5173\|supabase\.co" \
  backend/src/main/java \
  --exclude-dir=target
```

---

## 📚 Related Documentation

- [Vite Environment Variables](https://vitejs.dev/guide/env-and-mode.html)
- [Spring Boot Application Properties](https://docs.spring.io/spring-boot/docs/current/reference/html/application-properties.html)
- [Environment Variable Best Practices](https://12factor.net/config)

---

**Last Updated:** April 12, 2026

