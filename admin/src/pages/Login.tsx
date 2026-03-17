import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'
import { api } from '../api/client'

export default function Login() {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const { login } = useAuth()
  const navigate = useNavigate()

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setLoading(true)
    setError('')
    try {
      const response = await api.auth.login(email, password)
      login(response.token)
      navigate('/')
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Authentication failed')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen bg-[#FBFBFA] flex items-center justify-center">
      <div className="bg-white border border-[#E9E9E7] rounded-lg p-8 w-full max-w-sm">
        <div className="mb-8">
          <h1 className="text-xl font-semibold text-[#37352F] tracking-tight">Verdant</h1>
          <p className="text-sm text-[#787774] mt-1">Sign in to admin</p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-xs font-medium text-[#787774] mb-1.5">
              Email
            </label>
            <input
              type="email"
              value={email}
              onChange={e => setEmail(e.target.value)}
              className="w-full px-3 py-2 border border-[#E9E9E7] rounded-md focus:ring-2 focus:ring-[#2EAADC]/30 focus:border-[#2EAADC] outline-none text-sm bg-[#FBFBFA]"
              placeholder="admin@verdant.app"
            />
          </div>

          <div>
            <label className="block text-xs font-medium text-[#787774] mb-1.5">
              Password
            </label>
            <input
              type="password"
              value={password}
              onChange={e => setPassword(e.target.value)}
              className="w-full px-3 py-2 border border-[#E9E9E7] rounded-md focus:ring-2 focus:ring-[#2EAADC]/30 focus:border-[#2EAADC] outline-none text-sm bg-[#FBFBFA]"
              placeholder="Enter your password"
            />
          </div>

          {error && (
            <div className={`text-sm px-3 py-2 rounded-md ${
              error.includes('Unable to connect') ? 'text-[#D9730D] bg-[#FBF3DB]' : 'text-[#E03E3E] bg-[#FBE4E4]'
            }`}>{error}</div>
          )}

          <button
            type="submit"
            disabled={loading || !email || !password}
            className="w-full bg-[#2EAADC] text-white py-2 rounded-md text-sm font-medium hover:bg-[#2898C4] disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
          >
            {loading ? 'Signing in...' : 'Sign In'}
          </button>
        </form>
      </div>
    </div>
  )
}
