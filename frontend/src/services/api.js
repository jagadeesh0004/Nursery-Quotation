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
  timeout: 30000,  // ✅ Increased from 15s to 30s for slower backends
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
    const isTimeout = error.message.includes('timeout')
    const backendMessage = error.response?.data?.message || error.response?.data?.error

    console.error('❌ API Error:', {
      url: error.config?.url,
      status,
      method: error.config?.method,
      message: error.message,
      backendMessage,  // ✅ Log backend error message
      isTimeout,
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

    // ✅ Handle timeout errors gracefully
    if (isTimeout) {
      console.warn('⏱️ Request timeout - operation may still succeed on backend')
      // Don't show aggressive error here - let consumer handle it
      // This prevents cascading retry loops
    }

    // ✅ Log 500 errors with backend message
    if (status === 500) {
      console.error('🔴 Backend error (500):', {
        url: error.config?.url,
        backendMessage: backendMessage || 'No error details from backend',
        fullResponse: error.response?.data,
      })
    }

    // Note: Error toasts are handled by the API consumer, not here globally
    // This allows more granular control per request

    return Promise.reject(error)
  }
)

export default api