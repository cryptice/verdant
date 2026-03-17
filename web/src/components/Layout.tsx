import { Outlet, NavLink, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

const navItems = [
  { to: '/', label: 'My World', icon: '🌍' },
  { to: '/plants', label: 'Plants', icon: '🌱' },
  { to: '/tasks', label: 'Tasks', icon: '📋' },
  { to: '/seeds', label: 'Seeds', icon: '🫘' },
  { to: '/account', label: 'Account', icon: '👤' },
]

export function Layout() {
  const { user, loading } = useAuth()
  const navigate = useNavigate()

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-cream">
        <div className="animate-spin h-8 w-8 border-4 border-green-primary border-t-transparent rounded-full" />
      </div>
    )
  }

  if (!user) {
    navigate('/login', { replace: true })
    return null
  }

  return (
    <div className="min-h-screen bg-cream flex flex-col">
      <main className="flex-1 pb-20 max-w-2xl mx-auto w-full">
        <Outlet />
      </main>
      <nav className="fixed bottom-0 inset-x-0 bg-cream-dark border-t border-cream-dark shadow-[0_-1px_8px_rgba(0,0,0,0.06)]">
        <div className="max-w-2xl mx-auto flex">
          {navItems.map(({ to, label, icon }) => (
            <NavLink
              key={to}
              to={to}
              end={to === '/'}
              className={({ isActive }) =>
                `flex-1 flex flex-col items-center py-2 text-xs transition-colors ${
                  isActive ? 'text-green-primary font-semibold' : 'text-text-secondary'
                }`
              }
            >
              <span className="text-lg">{icon}</span>
              {label}
            </NavLink>
          ))}
        </div>
      </nav>
    </div>
  )
}
