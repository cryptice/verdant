import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { mcpApi } from '../api/mcp'

export function McpConnections() {
  const { i18n } = useTranslation()
  const sv = i18n.language.startsWith('sv')
  const queryClient = useQueryClient()
  const query = useQuery({ queryKey: ['mcp-connections'], queryFn: mcpApi.connections })
  const revoke = useMutation({ mutationFn: mcpApi.revoke, onSuccess: () => queryClient.invalidateQueries({ queryKey: ['mcp-connections'] }) })
  return <main className="page-body">
    <h1>{sv ? 'AI-anslutningar' : 'AI connections'}</h1>
    <p>{sv ? 'Dessa appar har läsåtkomst till den angivna organisationen. Återkallning stoppar åtkomsten direkt.' : 'These apps have read access to the specified organization. Revoking stops access immediately.'}</p>
    {(query.isError || revoke.isError) && <p role="alert">{sv ? 'Kunde inte hantera anslutningarna. Försök igen.' : 'Could not manage connections. Try again.'}</p>}
    {query.isLoading && <p>{sv ? 'Laddar…' : 'Loading…'}</p>}
    {query.data?.length === 0 && <p>{sv ? 'Inga aktiva anslutningar.' : 'No active connections.'}</p>}
    {query.data?.map(connection => <article key={connection.id} style={{ borderTop: '1px solid var(--color-rule)', padding: '20px 0' }}>
      <strong>{connection.clientName}</strong><p>{connection.organizationName}</p>
      <p>{sv ? 'Ansluten' : 'Connected'}: {new Date(connection.createdAt).toLocaleDateString(i18n.language)}</p>
      <button disabled={revoke.isPending} onClick={() => revoke.mutate(connection.id)}>{sv ? 'Återkalla åtkomst' : 'Revoke access'}</button>
    </article>)}
  </main>
}
