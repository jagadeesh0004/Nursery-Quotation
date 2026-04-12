import axios from 'axios'
import { API_BASE_URL } from '../constants'
import { supabase } from '../lib/supabase'

/**
 * Axios instance with global interceptors for:
 * - Automatic token injection
 * - Global error handling with toasts
 * - Loading states
 * - Request/response logging
 */

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: { 'Content-Type': 'application/json' },
  timeout: 15000,
})

// ─── REQUEST INTERCEPTOR ──────────────────────────────────────────────────────
// Attaches auth token to every request

api.interceptors.request.use(
  async (config) => {
    try {
      const { data, error } = await supabase.auth.getSession()

      if (error) {
        console.error('❌ Session error:', error)
        return config
      }

      const token = data?.session?.access_token

      if (token) {
        config.headers.Authorization = `Bearer ${token}`
        console.log('🔐 Token attached:', {
          url: config.url,
          method: config.method,
        })
      }
    } catch (err) {
      console.error('❌ Failed to attach token:', err)
    }

    return config
  },
  (error) => Promise.reject(error)
)

// ─── RESPONSE INTERCEPTOR ─────────────────────────────────────────────────────
// Handles success & error responses globally

api.interceptors.response.use(
  // Success response
  (response) => {
    console.log('✅ API Success:', {
      url: response.config.url,
      status: response.status,
      method: response.config.method,
    })
    return response
  },

  // Error response
  async (error) => {
    const { status } = error.response || {}

    console.error('❌ API Error:', {
      url: error.config?.url,
      status,
      method: error.config?.method,
      message: error.message,
    })

    // Handle 401 - Redirect to login
    if (status === 401) {
      console.warn('🔐 Session expired - redirecting to login')
      try {
        await supabase.auth.signOut()
        window.location.href = '/login'
      } catch (err) {
        console.error('Error during logout:', err)
      }
    }

    // Note: Error toasts are handled by the API consumer, not here globally
    // This allows more granular control per request

    return Promise.reject(error)
  }
)

export default api