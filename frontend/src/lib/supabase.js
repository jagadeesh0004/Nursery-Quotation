import { createClient } from '@supabase/supabase-js'

const supabaseUrl = import.meta.env.VITE_SUPABASE_URL
const supabaseAnonKey = import.meta.env.VITE_SUPABASE_ANON_KEY

// Debug logging
console.log('🔧 Supabase Configuration:')
console.log('URL:', supabaseUrl)
console.log('Anon Key Present:', !!supabaseAnonKey)
console.log('Anon Key Length:', supabaseAnonKey?.length)
console.log('Anon Key starts with:', supabaseAnonKey?.substring(0, 20) + '...')

if (!supabaseUrl || !supabaseAnonKey) {
  console.error('❌ Missing Supabase configuration!', {
    hasUrl: !!supabaseUrl,
    hasKey: !!supabaseAnonKey,
  })
}

export const supabase = createClient(supabaseUrl, supabaseAnonKey, {
  auth: {
    flowType: 'implicit',
    detectSessionInUrl: true,
    persistSession: true,
    storage: window.localStorage,
  }
})