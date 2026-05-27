import { createContext, useContext, useState, useEffect, useCallback, useRef } from 'react'
import { supabase } from '../lib/supabase'
import { profileService } from '../services'

const AuthContext = createContext(null)

const SUPABASE_STORAGE_PREFIX =
  import.meta.env.VITE_SUPABASE_STORAGE_PREFIX || 'sb-ymmbujskqowztgffqlmz-'

function clearSupabaseStorage() {
  Object.keys(localStorage)
    .filter((key) => key.startsWith(SUPABASE_STORAGE_PREFIX))
    .forEach((key) => localStorage.removeItem(key))
}

export function AuthProvider({ children }) {
  const [session, setSession] = useState(null)
  const [user, setUser] = useState(null)
  const [profile, setProfile] = useState(null)
  const [loading, setLoading] = useState(true)
  const [isNewSession, setIsNewSession] = useState(false)
  const [profileLoaded, setProfileLoaded] = useState(false)
  const lastFetchedToken = useRef(null)

  const syncSession = useCallback(async () => {
    const {
      data: { session: currentSession },
      error,
    } = await supabase.auth.getSession()

    if (error) {
      console.error('Error getting session', error)
      clearSupabaseStorage()
      setSession(null)
      setUser(null)
      setProfile(null)
      return null
    }

    setSession(currentSession)
    setUser(currentSession?.user ?? null)

    if (!currentSession?.user) {
      setProfile(null)
    }

    return currentSession
  }, [])

  useEffect(() => {
    let mounted = true

    const initializeAuth = async () => {
      try {
        await syncSession()
      } catch (err) {
        console.error('Error initializing auth', err)
        clearSupabaseStorage()
      } finally {
        if (mounted) setLoading(false)
      }
    }

    initializeAuth()

    const {
      data: { subscription },
    } = supabase.auth.onAuthStateChange(async (event, currentSession) => {
      if (!mounted) return

      if (currentSession?.access_token) {
        setSession(currentSession)
        setUser(currentSession.user ?? null)
        if (event === 'SIGNED_IN') {
          setIsNewSession(true)
        }
      } else {
        setSession(null)
        setUser(null)
        setProfile(null)
        setIsNewSession(false)
        setProfileLoaded(false)
      }

      setLoading(false)
    })

    return () => {
      mounted = false
      subscription?.unsubscribe()
    }
  }, [syncSession])

  const fetchProfile = useCallback(async () => {
    if (!session?.access_token) {
      setProfile(null)
      setIsNewSession(false)
      return null
    }

    if (lastFetchedToken.current === session.access_token) {
      return null
    }

    lastFetchedToken.current = session.access_token
    setProfileLoaded(false)

    try {
      const res = await profileService.get()
      setProfile(res.data.data)
      setIsNewSession(false)
      setProfileLoaded(true)
      return res.data.data
    } catch (err) {
      if (
        err.response?.status !== 404 &&
        err.response?.status !== 401 &&
        !err.message?.includes('timeout')
      ) {
        console.error('Profile fetch error', err)
      }

      setProfile(null)
      setIsNewSession(false)
      setProfileLoaded(true)
      return null
    }
  }, [session?.access_token])

  useEffect(() => {
    fetchProfile()
  }, [fetchProfile])

  const logout = useCallback(async () => {
    try {
      await supabase.auth.signOut()
    } catch (err) {
      console.error('Error signing out', err)
    }

    setSession(null)
    setUser(null)
    setProfile(null)
    setIsNewSession(false)
    setProfileLoaded(false)
    clearSupabaseStorage()
  }, [])

  const login = useCallback(async (email, password) => {
    try {
      const { data, error } = await supabase.auth.signInWithPassword({
        email,
        password,
      })

      if (error) throw error

      const sessionAfterLogin = await syncSession()
      return { success: true, data, session: sessionAfterLogin }
    } catch (err) {
      return { success: false, error: err }
    }
  }, [syncSession])

  const forceRefreshProfile = useCallback(async () => {
    lastFetchedToken.current = null
    return fetchProfile()
  }, [fetchProfile])

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
        profileLoaded,
        logout,
        login,
        refreshSession: syncSession,
        fetchProfile,
        forceRefreshProfile,
      }}
    >
      {children}
    </AuthContext.Provider>
  )
}

// eslint-disable-next-line react-refresh/only-export-components
export const useAuth = () => useContext(AuthContext)
