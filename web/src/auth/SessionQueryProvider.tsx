import { useEffect, useState, type ReactNode } from 'react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { useAuth } from './AuthContext'
import { useOrg } from './OrgContext'

function QuerySession({ children }: { children: ReactNode }) {
  const [client] = useState(() => new QueryClient({
    defaultOptions: {
      queries: {
        retry: (failureCount, error) => {
          if (error instanceof Error && 'status' in error) return false
          return failureCount < 2
        },
        staleTime: 30_000,
      },
    },
  }))

  useEffect(() => () => client.clear(), [client])

  return <QueryClientProvider client={client}>{children}</QueryClientProvider>
}

/** Each login and organization gets its own cache and mounted page state. */
export function SessionQueryProvider({ children }: { children: ReactNode }) {
  const { token } = useAuth()
  const { activeOrg, loading } = useOrg()
  // OrgProvider synchronizes the request header before mounting query consumers.
  if (loading) return null
  return <QuerySession key={`${token ?? 'guest'}:${activeOrg?.orgId ?? 'none'}`}>{children}</QuerySession>
}
