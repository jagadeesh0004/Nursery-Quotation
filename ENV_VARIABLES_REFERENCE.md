# Environment Variables Quick Reference

## 🎯 Quick Start

### Frontend Variables (`.env`)
| Variable | Purpose | Example | Required |
|----------|---------|---------|----------|
| `VITE_SUPABASE_URL` | Supabase project URL | `https://ymmbujskqowztgffqlmz.supabase.co` | ✅ Yes |
| `VITE_SUPABASE_ANON_KEY` | Supabase public auth key | `eyJhbGci...` | ✅ Yes |
| `VITE_API_BASE_URL` | Backend API endpoint | `http://localhost:8091/api` | ✅ Yes |
| `VITE_SUPABASE_STORAGE_PREFIX` | localStorage prefix | `sb-ymmbujskqowztgffqlmz-` | ❌ Optional (has default) |
| `VITE_PORT` | Dev server port | `5173` | ❌ Optional (defaults to 5173) |

### Backend Variables (`.env`)
| Variable | Purpose | Example | Required |
|----------|---------|---------|----------|
| `SPRING_DATASOURCE_URL` | PostgreSQL JDBC URL | `jdbc:postgresql://db.ymmbujskqowztgffqlmz.supabase.co:5432/postgres` | ✅ Yes |
| `SPRING_DATASOURCE_USERNAME` | Database user | `postgres` | ✅ Yes |
| `SPRING_DATASOURCE_PASSWORD` | Database password | `SUNS@9491944388` | ✅ Yes |
| `SUPABASE_JWKS_URI` | JWT public keys endpoint | `https://ymmbujskqowztgffqlmz.supabase.co/auth/v1/.well-known/jwks.json` | ✅ Yes |
| `SUPABASE_ISSUER` | JWT issuer | `https://ymmbujskqowztgffqlmz.supabase.co/auth/v1` | ✅ Yes |
| `SUPABASE_PROJECT_ID` | Supabase project ID | `ymmbujskqowztgffqlmz` | ✅ Yes |
| `SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI` | OAuth2 issuer URI | `https://ymmbujskqowztgffqlmz.supabase.co/auth/v1` | ✅ Yes |
| `SERVER_PORT` | Backend server port | `8091` | ❌ Optional (defaults to 8091) |
| `FRONTEND_URL` | Frontend URL for CORS | `http://localhost:5173` | ❌ Optional (has default) |
| `CORS_ALLOWED_ORIGINS` | Comma-separated allowed CORS origins | `http://localhost:3000,http://localhost:5173` | ❌ Optional (has defaults) |
| `UPLOAD_DIR` | File upload directory | `./uploads` | ❌ Optional (defaults to ./uploads) |
| `LOGOS_DIR` | Logo upload directory | `./uploads/logos` | ❌ Optional |
| `PDFS_DIR` | PDF export directory | `./uploads/pdfs` | ❌ Optional |
| `PDF_CACHE_ENABLED` | Enable PDF caching | `false` | ❌ Optional (defaults to false) |

---

## 📍 Where Variables Are Used

### Frontend Code Locations

**Supabase Setup** (`src/lib/supabase.js`):
```javascript
const supabaseUrl = import.meta.env.VITE_SUPABASE_URL
const supabaseAnonKey = import.meta.env.VITE_SUPABASE_ANON_KEY
```

**API Configuration** (`src/constants/index.js`):
```javascript
export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8090/api'
```

**Auth Context** (`src/context/AuthContext.jsx`):
```javascript
const SUPABASE_STORAGE_PREFIX = import.meta.env.VITE_SUPABASE_STORAGE_PREFIX || 'sb-ymmbujskqowztgffqlmz-'
```

**Vite Config** (`vite.config.js`):
```javascript
const port = parseInt(process.env.VITE_PORT || '5173', 10)
```

### Backend Code Locations

**Application Properties** (`src/main/resources/application.yml`):
```yaml
datasource:
  url: ${SPRING_DATASOURCE_URL:jdbc:postgresql://...}
  username: ${SPRING_DATASOURCE_USERNAME:postgres}
  password: ${SPRING_DATASOURCE_PASSWORD:SUNS@9491944388}

supabase:
  jwks-uri: ${SUPABASE_JWKS_URI:https://...}
  issuer: ${SUPABASE_ISSUER:https://...}
```

**Security Config** (`src/main/java/com/nursery/config/SecurityConfig.java`):
```java
@Value("${app.cors.allowed-origins:http://localhost:3000,http://localhost:5173}")
private String allowedOrigins;
```

**Web Config** (`src/main/java/com/nursery/config/WebConfig.java`):
```java
@Value("${app.frontend.url:http://localhost:5173}")
private String frontendUrl;
```

---

## 🔄 Environment Resolution Priority

### Frontend (Vite)
1. `.env.local` (local overrides, not committed)
2. `.env` (local configuration, not committed)
3. `.env.example` (template, committed)
4. Default values in code (fallback)

### Backend (Spring Boot)
1. System environment variables
2. `.env` file (if loaded)
3. `application.yml` defaults (`${VAR:default}`)

---

## 🛠️ Troubleshooting

### Frontend Issues

**"Supabase configuration is missing"**
- Solution: Ensure `.env` file exists and has `VITE_SUPABASE_URL` and `VITE_SUPABASE_ANON_KEY`

**"API request fails with 401"**
- Solution: Check `VITE_API_BASE_URL` points to correct backend port (usually 8091)

**"Dev server won't start"**
- Solution: If VITE_PORT is taken, change it in `.env` or use `strictPort: false` (already set)

### Backend Issues

**"Database connection refused"**
- Solution: Verify `SPRING_DATASOURCE_URL` and credentials in `.env`

**"JWT validation fails"**
- Solution: Ensure `SUPABASE_JWKS_URI` and `SUPABASE_ISSUER` match your Supabase project

**"CORS error from frontend"**
- Solution: Add frontend URL to `CORS_ALLOWED_ORIGINS` (include protocol and port)

---

## 📦 Loading Variables in Different Environments

### Docker
```dockerfile
FROM node:18
WORKDIR /app
COPY . .

# Copy .env at build time
COPY .env .env

# Install and build
RUN npm install
RUN npm run build

# Set env vars for runtime
ENV VITE_API_BASE_URL=http://api:8091/api
```

### Docker Compose
```yaml
services:
  frontend:
    environment:
      - VITE_API_BASE_URL=http://backend:8091/api
      - VITE_SUPABASE_URL=${SUPABASE_URL}
      - VITE_SUPABASE_ANON_KEY=${SUPABASE_ANON_KEY}
  
  backend:
    environment:
      - SPRING_DATASOURCE_URL=${DB_URL}
      - SPRING_DATASOURCE_USERNAME=${DB_USER}
      - SPRING_DATASOURCE_PASSWORD=${DB_PASS}
```

### GitHub Actions
```yaml
jobs:
  deploy:
    runs-on: ubuntu-latest
    env:
      VITE_SUPABASE_URL: ${{ secrets.SUPABASE_URL }}
      VITE_SUPABASE_ANON_KEY: ${{ secrets.SUPABASE_ANON_KEY }}
    steps:
      - uses: actions/checkout@v2
      - run: npm install && npm run build
```

### Kubernetes
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: app-config
data:
  VITE_API_BASE_URL: "http://api:8091/api"
  FRONTEND_URL: "http://frontend:5173"
---
apiVersion: v1
kind: Secret
metadata:
  name: app-secrets
type: Opaque
stringData:
  VITE_SUPABASE_ANON_KEY: "eyJhbGci..."
  SPRING_DATASOURCE_PASSWORD: "..."
```

---

## ✅ Verification Commands

```bash
# Frontend: Verify .env exists
ls -la frontend/.env

# Backend: Verify .env exists
ls -la backend/.env

# Check no hardcoded secrets in source code
grep -r "eyJhbGci\|SUNS@9491944388\|ymmbujskqowztgffqlmz" \
  --include="*.jsx" \
  --include="*.js" \
  --include="*.java" \
  frontend/src/ backend/src/main/java/

# Frontend: Test loading vars
cd frontend && npm run dev

# Backend: Test loading vars
cd backend && ./mvnw spring-boot:run
```

---

**Created:** April 12, 2026  
**Status:** ✅ All environment variables migrated to `.env` files

