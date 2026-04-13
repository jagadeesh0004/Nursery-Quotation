/**
 * components/ui/Spinner.jsx
 * ─────────────────────────────────────────────────────────────
 * Reusable loading spinner component
 * ─────────────────────────────────────────────────────────────
 */

export function Spinner({ size = 'md', message = 'Loading...' }) {
  const sizeClasses = {
    sm: 'w-8 h-8 border-2',
    md: 'w-12 h-12 border-4',
    lg: 'w-16 h-16 border-4',
  }

  return (
    <div className="flex flex-col items-center justify-center gap-4">
      <div
        className={`${sizeClasses[size]} border-gray-200 border-t-[#5CB338] rounded-full animate-spin`}
      />
      {message && <p className="text-gray-600 text-sm font-medium">{message}</p>}
    </div>
  )
}

export function FullScreenSpinner({ message = 'Loading...' }) {
  return (
    <div className="min-h-screen w-full flex items-center justify-center bg-surface">
      <Spinner size="lg" message={message} />
    </div>
  )
}

