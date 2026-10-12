import { useState } from 'react'
import { Navigate, useLocation } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { useAuth } from '../auth/AuthContext'
import { mcpLoginUrl } from '../auth/loginReturnTo'
import { mcpApi } from '../api/mcp'

export function McpAuthorizePage() {
  const { user, token, loading } = useAuth()
  const location = useLocation()
  const { i18n } = useTranslation()
  const sv = i18n.language.startsWith('sv')
  const request = new URLSearchParams(location.search).get('request') ?? ''
  const valid = /^[0-9a-f-]{36}$/i.test(request)
  const details = useQuery({ queryKey: ['mcp-authorization', request], queryFn: () => mcpApi.request(request), enabled: valid && !!token })
  const [selection, setSelection] = useState<number | null>(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  if (loading) return null
  if (!token) return <Navigate to={mcpLoginUrl(location.pathname, location.search)} replace />
  const orgId = selection ?? user?.organizations[0]?.orgId
  async function consent(approve: boolean) {
    setBusy(true); setError('')
    try {
      const result = approve ? await mcpApi.approve(request, orgId!) : await mcpApi.deny(request)
      window.location.assign(result.redirectUrl)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Authorization failed')
      setBusy(false)
    }
  }
  return <main className="page-body" style={{ maxWidth: 560, margin: '60px auto', padding: 24 }}>
    <h1>{sv ? 'Anslut till Verdant' : 'Connect to Verdant'}</h1>
    {(!valid || details.isError) && <p role="alert">{sv ? 'Förfrågan har gått ut eller är ogiltig. Starta anslutningen igen från din AI-app.' : 'This request expired or is invalid. Start the connection again from your AI app.'}</p>}
    {details.data && <>
      <p><strong>{details.data.clientName}</strong> {sv ? 'begär läsåtkomst till en av dina organisationer.' : 'requests read access to one of your organizations.'}</p>
      <p>{sv ? 'Anslutningens adress:' : 'Connection address:'} <strong>{details.data.callbackOrigin}</strong></p>
      <p>{sv ? 'Kontrollera att du känner igen appen och adressen. Appnamnet anges av den som begär anslutningen.' : 'Check that you recognize the app and address. The app name is supplied by the requesting client.'}</p>
      <label htmlFor="mcp-org">{sv ? 'Organisation' : 'Organization'}</label>
      <select id="mcp-org" value={orgId ?? ''} onChange={e => setSelection(Number(e.target.value))} style={{ display: 'block', width: '100%', margin: '12px 0 24px', padding: 12 }}>
        {user?.organizations.map(org => <option key={org.orgId} value={org.orgId}>{org.orgEmoji} {org.orgName}</option>)}
      </select>
      <p>{sv ? 'Tillåt att appen läser odlingsplaner, uppgifter, arter, frölager och registrerade säsongsresultat. Den kan inte ändra eller radera något.' : 'Allow the app to read planting plans, tasks, species, seed inventory and recorded season results. It cannot change or delete anything.'}</p>
      <p>{sv ? 'Du kan återkalla åtkomsten under Konto → AI-anslutningar.' : 'You can revoke access under Account → AI connections.'}</p>
      {!orgId && <p>{sv ? 'Du behöver tillhöra en organisation för att ansluta.' : 'Join an organization before connecting.'}</p>}
      {error && <p role="alert">{error}</p>}
      <div style={{ display: 'flex', flexWrap: 'wrap', gap: 16, marginTop: 24 }}>
        <button className="btn-primary" disabled={busy || !orgId} onClick={() => consent(true)}>{sv ? 'Tillåt läsåtkomst' : 'Allow read access'}</button>
        <button className="btn-secondary" disabled={busy} onClick={() => consent(false)}>{sv ? 'Avbryt' : 'Cancel'}</button>
      </div>
    </>}
    {details.isLoading && <p>{sv ? 'Laddar…' : 'Loading…'}</p>}
  </main>
}
