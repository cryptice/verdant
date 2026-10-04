import { useState, useCallback } from 'react'
import { useQueryClient } from '@tanstack/react-query'

interface AuthState {
  token: string | null
  isAuthenticated: boolean
}

export function useAuth() {
  const queryClient = useQueryClient()
  const [auth, setAuth] = useState<AuthState>(() => {
    const token = localStorage.getItem('admin_token')
    return { token, isAuthenticated: !!token }
  })

  const login = useCallback((token: string) => {
    queryClient.clear()
    localStorage.removeItem('verdant_token')
    localStorage.setItem('admin_token', token)
    setAuth({ token, isAuthenticated: true })
  }, [queryClient])

  const logout = useCallback(() => {
    queryClient.clear()
    localStorage.removeItem('admin_token')
    setAuth({ token: null, isAuthenticated: false })
  }, [queryClient])

  return { ...auth, login, logout }
}
