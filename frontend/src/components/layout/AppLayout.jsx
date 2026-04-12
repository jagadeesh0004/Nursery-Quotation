import { Outlet } from 'react-router-dom'
import Sidebar from './Sidebar'

export default function AppLayout() {
  return (
    <div className="flex min-h-screen bg-green-50/40">
      <Sidebar />
      <main className="flex-1 lg:ml-64 min-w-0 overflow-x-hidden">
        <div className="p-6 pt-20 lg:p-8 max-w-5xl mx-auto">
          <Outlet />
        </div>
      </main>
    </div>
  )
}
