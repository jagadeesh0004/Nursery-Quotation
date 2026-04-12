import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { Toaster } from 'react-hot-toast'
import { AuthProvider }        from './context/AuthContext'
import ProtectedRoute          from './routes/ProtectedRoute'
import AppLayout               from './components/layout/AppLayout'
import LoginPage               from './pages/LoginPage'
import AuthCallbackPage        from './pages/AuthCallbackPage'  // ADD THIS
import DashboardPage           from './pages/DashboardPage'
import PlantsPage              from './pages/PlantsPage'
import QuotationFormPage       from './pages/QuotationFormPage'
import QuotationDetailPage     from './pages/QuotationDetailPage'
import SettingsPage            from './pages/SettingsPage'
import PublicQuotationPage     from './pages/PublicQuotationPage'

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          {/* Public */}
          <Route path="/login"                    element={<LoginPage />} />
          <Route path="/auth/callback"            element={<AuthCallbackPage />} />  {/* ADD THIS */}
          <Route path="/public/quotations/:token" element={<PublicQuotationPage />} />

          {/* Protected */}
          <Route element={<ProtectedRoute />}>
            <Route element={<AppLayout />}>
              <Route path="/dashboard"            element={<DashboardPage />} />
              <Route path="/plants"               element={<PlantsPage />} />
              <Route path="/quotations/new"       element={<QuotationFormPage />} />
              <Route path="/quotations/:id/edit"  element={<QuotationFormPage />} />
              <Route path="/quotations/:id"       element={<QuotationDetailPage />} />
              <Route path="/settings"             element={<SettingsPage />} />
            </Route>
          </Route>

          {/* Fallback */}
          <Route path="/"  element={<Navigate to="/login" replace />} />
          <Route path="*"  element={<Navigate to="/login" replace />} />
        </Routes>
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