import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { FullScreenSpinner } from '../components/ui/Spinner'

export default function ProtectedRoute() {
  const { isAuthenticated, loading, profile, profileLoaded } = useAuth()
  const location = useLocation()

  if (loading || (!profileLoaded && isAuthenticated)) {
    return <FullScreenSpinner message="Loading your data..." />
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />
  }

  if (profile === null && location.pathname !== '/settings') {
    return <Navigate to="/settings" replace />
  }

  return <Outlet />
}
