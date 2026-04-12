# QuotoNursery — Frontend

React + Vite frontend for the Nursery Quotation System.

## Tech Stack
- **React 18** — functional components + hooks
- **Vite** — dev server & bundler
- **Tailwind CSS** — utility-first styling
- **Axios** — HTTP client (centralised in `services/`)
- **React Router v6** — client-side routing
- **React Hook Form + Yup** — form validation
- **react-hot-toast** — notifications
- **Lucide React** — icons

## Setup

```bash
# 1. Install dependencies
npm install

# 2. Configure environment
cp .env.example .env
# Edit VITE_API_BASE_URL if your backend runs on a different port

# 3. Start dev server
npm run dev
```

Backend must be running at `http://localhost:8090/api`.

## Pages

| Route | Page | Auth |
|-------|------|------|
| `/login` | OTP phone login | Public |
| `/dashboard` | Quotations list | Protected |
| `/quotations/new` | Create quotation | Protected |
| `/quotations/:id` | View quotation | Protected |
| `/quotations/:id/edit` | Edit draft | Protected |
| `/plants` | Plant catalog | Protected |
| `/settings` | Nursery profile | Protected |
| `/public/quotations/:token` | Public share view | Public |

## Project Structure

```
src/
├── components/
│   ├── layout/          Sidebar, AppLayout
│   └── ui/              Button, Input, Modal, Badge, Card, …
├── context/
│   └── AuthContext.jsx  Global auth state + profile
├── pages/               One file per page/route
├── routes/
│   └── ProtectedRoute   Redirects unauthenticated users
├── services/
│   ├── api.js           Axios instance (JWT + 401 handling)
│   └── index.js         All API calls (authService, plantService, …)
├── utils/               formatCurrency, formatDate, getError, …
├── constants/           API_BASE_URL, ROUTES, STATUS
└── styles/              Tailwind globals
```
