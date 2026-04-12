import { useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { supabase } from '../lib/supabase'

export default function AuthCallbackPage() {
  const navigate = useNavigate()

  useEffect(() => {
    console.log('Full URL:', window.location.href)

    const { data: { subscription } } = supabase.auth.onAuthStateChange((event, session) => {
      console.log('Callback event:', event, !!session)
      if (event === 'SIGNED_IN' && session) {
        navigate('/dashboard', { replace: true })
      } else if (event === 'SIGNED_OUT') {
        navigate('/login', { replace: true })
      }
    })

    // Fallback for already-resolved session
    supabase.auth.getSession().then(({ data: { session } }) => {
      console.log('getSession result:', !!session)
      if (session) navigate('/dashboard', { replace: true })
    })

    return () => subscription.unsubscribe()
  }, [navigate])

  return (
    <div className="min-h-screen flex items-center justify-center bg-surface">
      <div className="flex flex-col items-center gap-4">
        <div className="w-8 h-8 border-2 border-gray-300 border-t-[#5CB338] rounded-full animate-spin" />
        <p className="text-sm text-muted">Signing you in...</p>
      </div>
    </div>
  )
}