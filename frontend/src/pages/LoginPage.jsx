/**
 * LoginPage - Email/Password authentication via Supabase
 */
import { useEffect, useState } from 'react'
import { Sprout, Eye, EyeOff } from 'lucide-react'
import toast from 'react-hot-toast'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { supabase } from '../lib/supabase'

export default function LoginPage() {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [loading, setLoading] = useState(false)
  const [showSignup, setShowSignup] = useState(false)
  const navigate = useNavigate()
  const { isAuthenticated, login, refreshSession, profile, loading: authLoading } = useAuth()

  useEffect(() => {
    if (authLoading) return
    if (isAuthenticated) {
      const target = profile ? '/dashboard' : '/settings'
      navigate(target, { replace: true })
    }
  }, [isAuthenticated, profile, authLoading, navigate])

  const handleLogin = async (e) => {
    e.preventDefault()
    if (!email || !password) {
      toast.error('Please fill in all fields')
      return
    }

    setLoading(true)
    try {
      const result = await login(email, password)

      if (!result.success) {
        toast.error(result.error?.message || 'Failed to sign in')
      } else if (result.session) {
        toast.success('Signed in successfully')
        const defaultTarget = profile ? '/dashboard' : '/settings'
        navigate(defaultTarget, { replace: true })
      } else {
        await refreshSession()
        toast.error('Login failed: No session returned')
      }
    } catch (err) {
      toast.error(err.message || 'An error occurred during sign-in')
    } finally {
      setLoading(false)
    }
  }

  const handleSignup = async (e) => {
    e.preventDefault()
    if (!email || !password) {
      toast.error('Please fill in all fields')
      return
    }
    if (password.length < 6) {
      toast.error('Password must be at least 6 characters')
      return
    }
    setLoading(true)
    try {
      const { data, error } = await supabase.auth.signUp({
        email,
        password,
      })
      if (error) {
        if (error.message && error.message.toLowerCase().includes('already registered')) {
          toast.error('User already registered. Please log in.')
          setShowSignup(false)
          setPassword('')
        } else {
          toast.error(error.message || 'Failed to sign up')
        }
      } else if (data?.user) {
        toast.success('Account created! Please log in.')
        setShowSignup(false)
        setPassword('')
      }
    } catch (err) {
      toast.error(err.message || 'An error occurred during sign-up')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen flex bg-surface overflow-y-auto">
      <div className="flex-1 flex items-center justify-center p-4 sm:p-6 my-4 sm:my-8">
        <div className="w-full max-w-md">
          <div className="flex items-center gap-2 justify-center mb-8 sm:mb-12">
            <div className="w-10 h-10 sm:w-12 sm:h-12 rounded-xl bg-[#5CB338] flex items-center justify-center">
              <Sprout className="w-5 h-5 sm:w-6 sm:h-6 text-white" />
            </div>
            <span className="text-2xl sm:text-3xl font-bold text-ink">QuotoNursery</span>
          </div>

          <div className="bg-white rounded-lg border border-gray-200 p-6 sm:p-10 shadow-sm">
            <h2 className="text-xl sm:text-2xl font-bold text-ink mb-2">
              {showSignup ? 'Create Account' : 'Welcome back'}
            </h2>
            <p className="text-sm text-muted mb-8">
              {showSignup
                ? 'Sign up to get started'
                : 'Sign in with your email and password'}
            </p>

            <form onSubmit={showSignup ? handleSignup : handleLogin} className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-ink mb-2">Email</label>
                <input
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="your@email.com"
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-[#5CB338] focus:border-transparent"
                  disabled={loading}
                  autoComplete="username"
                />
              </div>

              <div className="mt-6">
                <label className="block text-sm font-medium text-ink mb-2">Password</label>
                <div className="relative">
                  <input
                    type={showPassword ? 'text' : 'password'}
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    placeholder="********"
                    autoComplete={showSignup ? 'new-password' : 'current-password'}
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-[#5CB338] focus:border-transparent pr-10"
                    disabled={loading}
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword(!showPassword)}
                    className="absolute inset-y-0 right-0 pr-3 flex items-center text-gray-400 hover:text-gray-600 disabled:opacity-50 transition-colors"
                    disabled={loading}
                    tabIndex="-1"
                  >
                    {showPassword ? (
                      <EyeOff className="w-5 h-5" />
                    ) : (
                      <Eye className="w-5 h-5" />
                    )}
                  </button>
                </div>
              </div>

              <button
                type="submit"
                disabled={loading}
                className="w-full flex items-center justify-center gap-2 px-4 py-3 bg-[#5CB338] text-white rounded-lg hover:bg-[#4a9530] disabled:bg-gray-300 disabled:cursor-not-allowed transition-colors font-medium"
              >
                {loading ? (
                  <>
                    <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
                    <span>{showSignup ? 'Creating...' : 'Signing in...'}</span>
                  </>
                ) : (
                  <span>{showSignup ? 'Create Account' : 'Sign In'}</span>
                )}
              </button>
            </form>

            <div className="mt-6 text-center">
              <p className="text-sm text-muted">
                {showSignup ? 'Already have an account?' : "Don't have an account?"}{' '}
                <button
                  onClick={() => {
                    setShowSignup(!showSignup)
                    setPassword('')
                  }}
                  disabled={loading}
                  className="text-[#5CB338] font-medium hover:underline disabled:opacity-50"
                >
                  {showSignup ? 'Sign In' : 'Sign Up'}
                </button>
              </p>
            </div>

            <p className="text-xs text-muted text-center mt-6">
              By signing in, you agree to our Terms of Service and Privacy Policy
            </p>
          </div>
        </div>
      </div>
    </div>
  )
}
