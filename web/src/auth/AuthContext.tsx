import { createContext, useContext, useState, useEffect, useCallback, useRef, type ReactNode } from 'react'
import { useNavigate } from 'react-router-dom'
import { api, setOnUnauthorized, type UserResponse } from '../api/client'

interface AuthState {
  user: UserResponse | null
  token: string | null
  loading: boolean
  login: (token: string, user: UserResponse) => void
  logout: () => void
  refreshUser: () => Promise<void>
}

const AuthContext = createContext<AuthState | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(() => localStorage.getItem('verdant_token'))
  const currentToken = useRef(token)
  const [user, setUser] = useState<UserResponse | null>(null)
  const [loading, setLoading] = useState(!!token)
  const navigate = useNavigate()

  const logout = useCallback(() => {
    currentToken.current = null
    localStorage.removeItem('verdant_token')
    setToken(null)
    setUser(null)
    setLoading(false)
    navigate('/login')
  }, [navigate])

  useEffect(() => {
    setOnUnauthorized(logout)
    return () => setOnUnauthorized(null)
  }, [logout])

  useEffect(() => {
    const syncSession = (event: StorageEvent) => {
      if (event.key !== null && event.key !== 'verdant_token') return
      const nextToken = localStorage.getItem('verdant_token')
      if (nextToken === currentToken.current) return
      currentToken.current = nextToken
      setToken(nextToken)
      setUser(null)
      setLoading(!!nextToken)
    }
    window.addEventListener('storage', syncSession)
    return () => window.removeEventListener('storage', syncSession)
  }, [])

  useEffect(() => {
    if (!token) { setLoading(false); return }
    let active = true
    const isCurrent = () => active && currentToken.current === token
    api.user.me()
      .then(user => { if (isCurrent()) setUser(user) })
      .catch(() => { if (isCurrent()) logout() })
      .finally(() => { if (isCurrent()) setLoading(false) })
    return () => { active = false }
  }, [token, logout])

  const login = useCallback((t: string, u: UserResponse) => {
    currentToken.current = t
    localStorage.removeItem('admin_token')
    localStorage.setItem('verdant_token', t)
    setToken(t)
    setUser(u)
    setLoading(false)
  }, [])

  const refreshUser = useCallback(async () => {
    const token = currentToken.current
    const u = await api.user.me()
    if (token && currentToken.current === token) setUser(u)
  }, [])

  return (
    <AuthContext.Provider value={{ user, token, loading, login, logout, refreshUser }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
