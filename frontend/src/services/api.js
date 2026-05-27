import axios from 'axios'
import { API_BASE_URL } from '../constants'
import { supabase } from '../lib/supabase'

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: { 'Content-Type': 'application/json' },
  timeout: 30000,
})

api.interceptors.request.use(
  async (config) => {
    try {
      const { data, error } = await supabase.auth.getSession()

      if (error) {
        console.error('Session error', error)
        return config
      }

      const token = data?.session?.access_token

      if (token) {
        config.headers.Authorization = `Bearer ${token}`
      }
    } catch (err) {
      console.error('Failed to attach token', err)
    }

    return config
  },
  (error) => Promise.reject(error)
)

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const { status } = error.response || {}
    const isTimeout = error.message?.includes('timeout')
    const backendMessage = error.response?.data?.message || error.response?.data?.error

    if (!isTimeout && status !== 404) {
      console.error('API error', {
        url: error.config?.url,
        status,
        method: error.config?.method,
        message: error.message,
        backendMessage,
      })
    }

    if (status === 401) {
      try {
        await supabase.auth.signOut()
        window.location.href = '/login'
      } catch (err) {
        console.error('Error during logout', err)
      }
    }

    return Promise.reject(error)
  }
)

export default api
