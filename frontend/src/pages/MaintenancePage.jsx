import { useEffect, useRef, useState } from 'react'
import { API_BASE_URL } from '../constants'

const INTERVAL_SECONDS = 60
const HEALTH_URL = `${API_BASE_URL}/health`

export default function MaintenancePage() {
  const [secondsLeft, setSecondsLeft] = useState(INTERVAL_SECONDS)
  const [checking, setChecking] = useState(false)
  const [statusTitle, setStatusTitle] = useState('Next check soon')
  const [statusMessage, setStatusMessage] = useState('Watching for the server...')
  const timerRef = useRef(null)
  const checkingRef = useRef(false)

  async function checkServer() {
    if (checkingRef.current) return

    checkingRef.current = true
    setChecking(true)
    setStatusTitle('Checking server...')
    setStatusMessage('Hang tight')

    const controller = new AbortController()
    const timeout = window.setTimeout(() => controller.abort(), 8000)

    try {
      const response = await fetch(HEALTH_URL, {
        cache: 'no-store',
        signal: controller.signal,
      })

      if (!response.ok) throw new Error('Server is not healthy')

      setStatusTitle('Back online')
      setStatusMessage('Taking you back to the application...')
      window.location.assign('/')
    } catch {
      setSecondsLeft(INTERVAL_SECONDS)
      setStatusTitle('Still growing...')
      setStatusMessage('The server is not ready yet')
    } finally {
      window.clearTimeout(timeout)
      checkingRef.current = false
      setChecking(false)
    }
  }

  useEffect(() => {
    timerRef.current = window.setInterval(() => {
      setSecondsLeft((current) => {
        if (current <= 1) {
          void checkServer()
          return INTERVAL_SECONDS
        }
        return current - 1
      })
    }, 1000)

    const initialCheck = window.setTimeout(() => void checkServer(), 1500)

    return () => {
      window.clearInterval(timerRef.current)
      window.clearTimeout(initialCheck)
    }
  }, [])

  const progress = 1 - secondsLeft / INTERVAL_SECONDS

  return (
    <main className="min-h-screen flex items-center justify-center p-6 bg-gradient-to-b from-[#f4faf4] to-[#e4f2e5] text-[#264030]">
      <section className="w-full max-w-[440px] text-center bg-white border border-[#cfe6d1] rounded-[20px] px-8 py-10 shadow-[0_8px_30px_rgba(46,125,50,0.08)] animate-fade-up">
        <svg className="w-[140px] h-[140px] mx-auto overflow-visible" viewBox="0 0 120 120" aria-hidden="true">
          <path className="stroke-[#4caf50] stroke-[5] [stroke-linecap:round] fill-none animate-[maintenance-grow_2s_ease-out_forwards]" strokeDasharray="80" strokeDashoffset="80" d="M60 84 C60 66 60 52 60 36" />
          <ellipse className="fill-[#81c784] animate-[maintenance-pop_.7s_cubic-bezier(.3,1.5,.5,1)_1.3s_forwards]" cx="45" cy="58" rx="17" ry="8" transform="rotate(-35 45 58)" />
          <ellipse className="fill-[#a5d6a7] animate-[maintenance-pop_.7s_cubic-bezier(.3,1.5,.5,1)_1.7s_forwards]" cx="75" cy="48" rx="17" ry="8" transform="rotate(35 75 48)" />
          <ellipse className="fill-[#66bb6a] animate-[maintenance-pop_.7s_cubic-bezier(.3,1.5,.5,1)_2.1s_forwards]" cx="60" cy="30" rx="8" ry="16" />
          <ellipse fill="#8d6e63" cx="60" cy="86" rx="24" ry="5" />
          <path fill="#c98b63" d="M38 88 H82 L77 112 Q76 116 72 116 H48 Q44 116 43 112 Z" />
          <rect fill="#b57a54" x="35" y="84" width="50" height="8" rx="3" />
        </svg>

        <h1 className="text-2xl font-bold text-[#2e7d32] mt-2 mb-2">We're tending the garden</h1>
        <p className="text-[15px] leading-relaxed text-[#6a8070]">
          Nursery Quotation is taking a short break for maintenance.
          <br />
          Your quotations and invoices are safe. We'll be back shortly.
        </p>

        <div className="mt-6 flex items-center justify-center gap-3.5 rounded-[14px] bg-[#e8f5e9] p-3.5">
          <div className={`relative w-[52px] h-[52px] flex-none ${checking ? 'animate-spin' : ''}`}>
            <svg className="w-[52px] h-[52px] -rotate-90" viewBox="0 0 44 44">
              <circle cx="22" cy="22" r="19" fill="none" stroke="#c8e2ca" strokeWidth="3" />
              <circle
                cx="22"
                cy="22"
                r="19"
                fill="none"
                stroke="#4caf50"
                strokeWidth="3"
                strokeLinecap="round"
                strokeDasharray="119.38"
                strokeDashoffset={119.38 * progress}
              />
            </svg>
            <span className="absolute inset-0 grid place-items-center text-sm font-bold text-[#2e7d32]">
              {checking ? '...' : secondsLeft}
            </span>
          </div>
          <div className="text-left text-[13px] leading-relaxed text-[#6a8070]">
            <strong className="block text-sm text-[#2e7d32]">{statusTitle}</strong>
            <span>{statusMessage}</span>
          </div>
        </div>

        <button
          type="button"
          onClick={() => void checkServer()}
          className="mt-[18px] rounded-[10px] bg-[#4caf50] px-6 py-2.5 text-sm font-semibold text-white transition-colors hover:bg-[#2e7d32]"
        >
          Check now
        </button>
        <p className="mt-4 text-xs text-[#93a898]">This page checks automatically</p>
      </section>
    </main>
  )
}
