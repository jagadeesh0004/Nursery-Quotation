import { createContext, useContext, useState, useEffect, useCallback } from 'react'
import { supabase } from '../lib/supabase'
import { profileService } from '../services'

const AuthContext = createContext(null)

const SUPABASE_STORAGE_PREFIX = import.meta.env.VITE_SUPABASE_STORAGE_PREFIX || 'sb-ymmbujskqowztgffqlmz-'

function clearSupabaseStorage() {
  Object.keys(localStorage)
    .filter((key) => key.startsWith(SUPABASE_STORAGE_PREFIX))
    .forEach((key) => localStorage.removeItem(key))
}

function decodeJwtPayload(token) {
  try {
    const payload = token.split('.')[1]
    const json = atob(payload.replace(/-/g, '+').replace(/_/g, '/'))
    return JSON.parse(json)
  } catch (err) {
    console.warn('⚠️ Failed to decode JWT payload:', err)
    return null
  }
}

function decodeJwtHeader(token) {
  try {
    const header = token.split('.')[0]
    const json = atob(header.replace(/-/g, '+').replace(/_/g, '/'))
    return JSON.parse(json)
  } catch (err) {
    console.warn('⚠️ Failed to decode JWT header:', err)
    return null
  }
}

export function AuthProvider({ children }) {
  // ── Auth state from Supabase ──────────────────────────────
  const [session, setSession] = useState(null)
  const [user, setUser] = useState(null)
  const [profile, setProfile] = useState(null)
  const [loading, setLoading] = useState(true)
  const [isNewSession, setIsNewSession] = useState(false) // Track if just logged in

  const syncSession = useCallback(async () => {
    const { data: { session: currentSession }, error } = await supabase.auth.getSession()

    console.log('🔍 Checking for existing session:', {
      hasSession: !!currentSession,
      hasError: !!error,
      email: currentSession?.user?.email,
      errorMsg: error?.message,
    })

    if (error) {
      console.error('❌ Error getting session:', error)
      clearSupabaseStorage()
      setSession(null)
      setUser(null)
      setProfile(null)
      return null
    }

    if (currentSession) {
      console.log('✅ Session restored:', {
        user: currentSession.user?.email,
        hasToken: !!currentSession.access_token,
        tokenExpiry: currentSession.expires_at,
      })

      const header = decodeJwtHeader(currentSession.access_token || '')
      if (header) {
        console.log('🔐 JWT header (debug):', {
          alg: header.alg,
          kid: header.kid,
          typ: header.typ,
        })
      }

      const claims = decodeJwtPayload(currentSession.access_token || '')
      if (claims) {
        console.log('🔐 JWT claims (debug):', {
          iss: claims.iss,
          aud: claims.aud,
          sub: claims.sub,
          exp: claims.exp,
        })
      }
    } else {
      console.log('ℹ️  No session found')
    }

    setSession(currentSession)
    setUser(currentSession?.user ?? null)
    if (!currentSession?.user) {
      setProfile(null)
    }

    return currentSession
  }, [])

   // ── Initialize session on app load ─────────────────────────
   useEffect(() => {
     let mounted = true

     const initializeAuth = async () => {
       try {
         await syncSession()
         if (mounted) setLoading(false)
       } catch (err) {
         console.error('❌ Error initializing auth:', err)
         // Clear any invalid session data
         clearSupabaseStorage()
         if (mounted) setLoading(false)
       }
     }

     // Initialize on mount
     initializeAuth()

      // Listen for auth state changes (login, logout, token refresh)
      const { data: { subscription } } = supabase.auth.onAuthStateChange(
        async (_event, currentSession) => {
          console.log('🔄 Auth state changed:', {
            event: _event,
            hasSession: !!currentSession,
            email: currentSession?.user?.email,
          })

          if (mounted) {
            // Validate session has a valid token
            if (currentSession?.access_token) {
              setSession(currentSession)
              if (currentSession?.user) {
                setUser(currentSession.user)
              }
              // Mark as new session only on SIGNED_IN event (login), not on page reload
              if (_event === 'SIGNED_IN') {
                setIsNewSession(true)
              }
            } else {
              // No valid token, clear session
              setSession(null)
              setUser(null)
              setProfile(null)
              setIsNewSession(false)
            }
            setLoading(false)
          }
        }
      )

     return () => {
       mounted = false
       subscription?.unsubscribe()
     }
   }, [])

  // ── Fetch nursery profile when session changes ───────────
  const fetchProfile = useCallback(async () => {
    if (!session?.user || !session?.access_token) {
      setProfile(null)
      return null
    }
     try {
       const res = await profileService.get()
       setProfile(res.data.data)
       // Reset isNewSession after profile is fetched (either found or null)
       setIsNewSession(false)
       return res.data.data
    } catch (err) {
      if (err.response?.status === 404) {
        // New user has no profile yet
        setProfile(null)
        // Reset isNewSession even if profile not found (so redirect only happens once)
        setIsNewSession(false)
        return null
      } else if (err.response?.status === 401) {
        // Backend rejected token; keep session for debugging
        console.log('Profile fetch returned 401; backend rejected token')
      } else {
        console.log('Profile fetch error:', err.message)
        setProfile(null)
      }
      // Reset isNewSession on any fetch completion
      setIsNewSession(false)
      return null
    }
  }, [session?.user, session?.access_token])

  useEffect(() => {
    fetchProfile()
  }, [fetchProfile])

  // ── Logout action ─────────────────────────────────────────
  const logout = useCallback(async () => {
    try {
      await supabase.auth.signOut()
    } catch (err) {
      console.error('Error signing out:', err)
    }
    setSession(null)
    setUser(null)
    setProfile(null)
    // Clear localStorage to ensure clean state
    clearSupabaseStorage()
  }, [])

  // ── Login with email/password ──────────────────────────────
  const login = useCallback(async (email, password) => {
    try {
      const { data, error } = await supabase.auth.signInWithPassword({
        email,
        password,
      })

      if (error) {
        throw error
      }

      const sessionAfterLogin = await syncSession()
      return { success: true, data, session: sessionAfterLogin }
    } catch (err) {
      return { success: false, error: err }
    }
  }, [syncSession])

  // ── Token from session (for API calls) ────────────────────
  const token = session?.access_token || null

   return (
     <AuthContext.Provider
       value={{
         session,
         user,
         profile,
         token,
         isAuthenticated: !!session?.access_token,
         loading,
         isNewSession,
         logout,
         login,
         refreshSession: syncSession,
         fetchProfile,
       }}
     >
       {children}
     </AuthContext.Provider>
   )
}

// eslint-disable-next-line react-refresh/only-export-components
export const useAuth = () => useContext(AuthContext)
