/**
 * routes/ProtectedRoute.jsx
 * ─────────────────────────────────────────────────────────────
 * Redirects unauthenticated users to /login.
 * Waits for session to load before deciding to redirect.
 * ─────────────────────────────────────────────────────────────
 */
import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

export default function ProtectedRoute() {
  const { isAuthenticated, loading, profile } = useAuth()
  const location = useLocation()

  // Wait for session to load - don't redirect while loading
  if (loading) {
    console.log('⏳ Session loading...')
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="text-center">
          <div className="w-12 h-12 border-4 border-gray-200 border-t-[#5CB338] rounded-full animate-spin mx-auto mb-4" />
          <p className="text-gray-600">Loading...</p>
        </div>
      </div>
    )
  }

  if (!isAuthenticated) {
    console.log('🚫 Not authenticated, redirecting to login')
    return <Navigate to="/login" replace />
  }

  // After loading is done, if profile is null and user is not on settings, redirect to settings
  if (!loading && profile === null && location.pathname !== '/settings') {
    console.log('📌 New user with no profile, redirecting to settings')
    return <Navigate to="/settings" replace />
  }

  console.log('✅ Authenticated, rendering protected route')
  return <Outlet />
}
