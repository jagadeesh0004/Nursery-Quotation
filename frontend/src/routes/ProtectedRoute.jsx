/**
 * routes/ProtectedRoute.jsx
 * ─────────────────────────────────────────────────────────────
 * Comprehensive authentication guard with proper loading sequence:
 *
 * 1. WAIT for profile to load completely
 * 2. THEN check authentication status
 * 3. THEN redirect based on profile existence
 * 4. FINALLY render routes
 * ─────────────────────────────────────────────────────────────
 */
import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { FullScreenSpinner } from '../components/ui/Spinner'

export default function ProtectedRoute() {
  const { isAuthenticated, loading, profile, isNewSession, profileLoaded } = useAuth()
  const location = useLocation()

  console.log('🔐 ProtectedRoute state:', {
    loading,
    profileLoaded,
    isAuthenticated,
    profile: profile?.nurseryName || 'null',
    isNewSession,
    pathname: location.pathname,
  })

  // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  // STATE 1: LOADING - Wait for profile to load completely
  // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  if (loading || !profileLoaded) {
    console.log('⏳ Waiting for profile to load...')
    return <FullScreenSpinner message="Loading your data..." />
  }

  // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  // STATE 2: DATA LOADED - Now check authentication
  // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  if (!isAuthenticated) {
    console.log('🚫 Not authenticated → redirecting to /login')
    return <Navigate to="/login" replace />
  }

  // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  // STATE 3: AUTHENTICATED - Check if profile exists
  // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  // If user is authenticated but has NO profile:
  // - Redirect to /settings ONLY if not already there
  // - This applies to BOTH new users and existing users without profile
  if (profile === null && location.pathname !== '/settings') {
    console.log('📌 No profile found → redirecting to /settings for setup')
    return <Navigate to="/settings" replace />
  }

  // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  // STATE 4: ALL GOOD - Render protected routes
  // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  console.log('✅ All checks passed → rendering protected route')
  return <Outlet />
}
