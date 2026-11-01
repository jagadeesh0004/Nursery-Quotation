import { useEffect, useState } from 'react'
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { Toaster } from 'react-hot-toast'
import { API_BASE_URL } from './constants'
import { AuthProvider }        from './context/AuthContext'
import ProtectedRoute          from './routes/ProtectedRoute'
import AppLayout               from './components/layout/AppLayout'
import LoginPage               from './pages/LoginPage'
import AuthCallbackPage        from './pages/AuthCallbackPage'
import DashboardPage           from './pages/DashboardPage'
import QuotationsPage          from './pages/QuotationsPage'
import CustomerDetailPage      from './pages/CustomerDetailPage'
import PlantsPage              from './pages/PlantsPage'
import QuotationFormPage       from './pages/QuotationFormPage'
import QuotationDetailPage     from './pages/QuotationDetailPage'
import InvoiceListPage         from './pages/InvoiceListPage'
import InvoiceFormPage         from './pages/InvoiceFormPage'
import InvoiceDetailPage       from './pages/InvoiceDetailPage'
import SettingsPage            from './pages/SettingsPage'
import PublicQuotationPage     from './pages/PublicQuotationPage'
import MaintenancePage         from './pages/MaintenancePage'

/**
 * App.jsx
 * ─────────────────────────────────────────────────────────────
 * Main app component with comprehensive authentication flow
 * ─────────────────────────────────────────────────────────────
 */
export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <AppRoutes />
      </BrowserRouter>
      <Toaster
        position="bottom-right"
        gutter={8}
        toastOptions={{
          duration: 3500,
          style: {
            background:   '#fff',
            color:        '#1A2E1A',
            border:       '1px solid #e8f5e9',
            borderRadius: '14px',
            fontSize:     '13.5px',
            fontFamily:   "'Poppins', system-ui, sans-serif",
            fontWeight:   500,
            padding:      '10px 16px',
            boxShadow:    '0 8px 28px rgba(0,0,0,0.09)',
          },
          success: { iconTheme: { primary: '#06D001', secondary: '#fff' } },
          error:   { iconTheme: { primary: '#ef4444', secondary: '#fff' } },
        }}
      />
    </AuthProvider>
  )
}

function AppRoutes() {
  const [maintenance, setMaintenance] = useState(false)

  useEffect(() => {
    let disposed = false

    const checkHealth = async () => {
      const controller = new AbortController()
      const timeout = window.setTimeout(() => controller.abort(), 8000)
      try {
        const response = await fetch(`${API_BASE_URL}/health`, {
          cache: 'no-store',
          signal: controller.signal,
        })
        if (!response.ok) throw new Error('Server is not healthy')
      } catch {
        if (!disposed) setMaintenance(true)
      } finally {
        window.clearTimeout(timeout)
      }
    }

    void checkHealth()
    const healthTimer = window.setInterval(() => {
      if (!disposed) void checkHealth()
    }, 60000)

    return () => {
      disposed = true
      window.clearInterval(healthTimer)
    }
  }, [])

  if (maintenance) return <MaintenancePage />

  return (
    <Routes>
          {/* ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ */}
          {/* PUBLIC ROUTES (No auth required) */}
          {/* ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ */}
          <Route path="/login"                    element={<LoginPage />} />
          <Route path="/auth/callback"            element={<AuthCallbackPage />} />
          <Route path="/public/quotations/:token" element={<PublicQuotationPage />} />

          {/* ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ */}
          {/* PROTECTED ROUTES (Auth + Profile required) */}
          {/* ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ */}
          <Route element={<ProtectedRoute />}>
            <Route element={<AppLayout />}>
              <Route path="/dashboard"            element={<DashboardPage />} />
              <Route path="/customers"            element={<DashboardPage />} />
              <Route path="/customers/:id"        element={<CustomerDetailPage />} />
              <Route path="/plants"               element={<PlantsPage />} />
              <Route path="/quotations/new"       element={<QuotationFormPage />} />
              <Route path="/quotations"           element={<QuotationsPage />} />
              <Route path="/quotations/:id/edit"  element={<QuotationFormPage />} />
              <Route path="/quotations/:id"       element={<QuotationDetailPage />} />
              <Route path="/invoices"             element={<InvoiceListPage />} />
              <Route path="/invoices/new"         element={<InvoiceFormPage />} />
              <Route path="/invoices/:id/edit"    element={<InvoiceFormPage />} />
              <Route path="/invoices/:id"         element={<InvoiceDetailPage />} />
              <Route path="/settings"             element={<SettingsPage />} />
            </Route>
          </Route>

          {/* ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ */}
          {/* FALLBACK ROUTES */}
          {/* ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ */}
          <Route path="/"  element={<Navigate to="/login" replace />} />
          <Route path="*"  element={<Navigate to="/login" replace />} />
    </Routes>
  )
}
