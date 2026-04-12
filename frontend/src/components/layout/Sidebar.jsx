/**
 * Sidebar — fixed left nav with Poppins, green-800 active state,
 * green-100 background highlights.
 */
import { useState } from 'react'
import { NavLink, useNavigate } from 'react-router-dom'
import { LayoutDashboard, Leaf, Settings, LogOut, Menu, X, Sprout } from 'lucide-react'
import { useAuth } from '../../context/AuthContext'
import { getInitials, cx } from '../../utils'
import toast from 'react-hot-toast'

const NAV = [
  { to: '/dashboard', icon: LayoutDashboard, label: 'Dashboard' },
  { to: '/plants',    icon: Leaf,            label: 'Plants Master' },
  { to: '/settings',  icon: Settings,        label: 'Settings' },
]

export default function Sidebar() {
  const { profile, user, logout } = useAuth()
  const navigate = useNavigate()
  const [open, setOpen] = useState(false)

  const displayName  = profile?.nurseryName || 'My Nursery'
  const displayPhone = profile?.phone || user?.phoneNumber || '—'

  function handleLogout() {
    logout()
    toast.success('Logged out successfully')
    navigate('/login')
  }

  function NavContent() {
    return (
      <div className="flex flex-col h-full bg-white">
        {/* Logo */}
        <div className="flex items-center gap-3 px-5 h-16 border-b border-gray-100 flex-shrink-0">
          <div className="w-9 h-9 rounded-xl bg-[#5CB338] flex items-center justify-center shadow-sm">
            <Sprout className="w-5 h-5 text-white" />
          </div>
          <span className="text-lg font-bold text-green-900 tracking-tight">QuotoNursery</span>
        </div>

        {/* Nav items */}
        <nav className="flex-1 px-3 py-5 space-y-1 overflow-y-auto scroll-thin">
          {NAV.map(({ to, icon: Icon, label }) => (
            <NavLink key={to} to={to} onClick={() => setOpen(false)}
              className={({ isActive }) => cx(
                'flex items-center gap-3 px-3.5 py-2.5 rounded-xl text-sm font-medium transition-all duration-150',
                isActive
                  ? 'bg-[#5CB338] text-white shadow-sm'
                  : 'text-muted hover:bg-green-100 hover:text-[#4d962e]'
              )}
            >
              <Icon className="w-4 h-4 flex-shrink-0" />
              {label}
            </NavLink>
          ))}
        </nav>

        {/* User profile removed from sidebar */}
      </div>
    )
  }

  return (
    <>
      {/* Mobile hamburger */}
      <button onClick={() => setOpen(true)}
        className="lg:hidden fixed top-4 left-4 z-30 w-9 h-9 bg-white rounded-xl border border-gray-200 shadow-sm flex items-center justify-center"
        aria-label="Open menu">
        <Menu className="w-4 h-4 text-gray-700" />
      </button>

      {/* Mobile drawer */}
      {open && (
        <div className="lg:hidden fixed inset-0 z-40">
          <div className="absolute inset-0 bg-black/30 backdrop-blur-sm animate-fade-in" onClick={() => setOpen(false)} />
          <aside className="relative w-64 h-full shadow-lg animate-slide-in">
            <button onClick={() => setOpen(false)}
              className="absolute top-3.5 right-3.5 z-10 w-7 h-7 rounded-lg bg-gray-100 flex items-center justify-center">
              <X className="w-3.5 h-3.5 text-gray-500" />
            </button>
            <NavContent />
          </aside>
        </div>
      )}

      {/* Desktop sidebar */}
      <aside className="hidden lg:flex flex-col w-64 h-screen fixed left-0 top-0 z-20 border-r border-gray-100">
        <NavContent />
      </aside>
    </>
  )
}
