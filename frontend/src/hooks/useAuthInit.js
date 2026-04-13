/**
 * hooks/useAuthInit.js
 * ─────────────────────────────────────────────────────────────
 * Hook to initialize authentication on app load
 * Handles session restoration and profile loading
 * ─────────────────────────────────────────────────────────────
 */
import { useEffect } from 'react'
import { useAuth } from '../context/AuthContext'

export function useAuthInit() {
  const { loading, profileLoaded, isAuthenticated, session, refreshSession } = useAuth()

  useEffect(() => {
    // On mount, ensure session is refreshed
    if (!session) {
      refreshSession()
    }
  }, [])

  return {
    isInitialized: !loading && profileLoaded,
    isLoading: loading || !profileLoaded,
    isAuthenticated,
  }
}

