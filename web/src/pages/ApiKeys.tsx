import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { apiKeysApi, type CreatedApiKey } from '../api/apiKeys'
import { Field, Masthead } from '../components/faltet'

const LABEL_STYLE: React.CSSProperties = {
  display: 'block',
  fontFamily: 'var(--font-mono)',
  fontSize: 9,
  letterSpacing: 1.4,
  textTransform: 'uppercase',
  color: 'var(--color-forest)',
  opacity: 0.7,
  marginBottom: 4,
}

const VALIDITY_DAYS = [30, 90, 365]

export function ApiKeys() {
  const { t, i18n } = useTranslation()
  const queryClient = useQueryClient()
  const [name, setName] = useState('Claude Code')
  const [validDays, setValidDays] = useState(90)
  const [created, setCreated] = useState<CreatedApiKey | null>(null)
  const [copied, setCopied] = useState(false)
  const keys = useQuery({ queryKey: ['api-keys'], queryFn: apiKeysApi.list })
  const refresh = () => queryClient.invalidateQueries({ queryKey: ['api-keys'] })
  const create = useMutation({
    mutationFn: () => apiKeysApi.create(name.trim(), validDays),
    onSuccess: result => { setCreated(result); setCopied(false); refresh() },
  })
  const revoke = useMutation({ mutationFn: apiKeysApi.revoke, onSuccess: refresh })
  const date = (value: string) => new Date(value).toLocaleDateString(i18n.language)

  const copy = async () => {
    if (!created) return
    try {
      await navigator.clipboard.writeText(created.token)
      setCopied(true)
    } catch {
      setCopied(false)
    }
  }

  return (
    <div>
      <Masthead left={t('nav.account')} center={t('apiKeys.masthead')} />
      <div className="page-body" style={{ paddingBottom: 80, maxWidth: 720 }}>
        <h1>{t('apiKeys.title')}</h1>
        <p>{t('apiKeys.intro')}</p>

        <div className="stack-mobile" style={{ display: 'grid', gridTemplateColumns: '2fr 1fr auto', gap: '16px 20px', alignItems: 'end', marginTop: 24 }}>
          <Field label={t('apiKeys.name')} editable value={name} onChange={setName} />
          <div>
            <label style={LABEL_STYLE} htmlFor="api-key-validity">{t('apiKeys.validity')}</label>
            <select id="api-key-validity" className="input" value={validDays} onChange={e => setValidDays(Number(e.target.value))}>
              {VALIDITY_DAYS.map(days => <option key={days} value={days}>{t('apiKeys.days', { count: days })}</option>)}
            </select>
          </div>
          <button className="btn-primary" disabled={!name.trim() || create.isPending} onClick={() => create.mutate()}>
            {t('apiKeys.create')}
          </button>
        </div>

        {created && (
          <section aria-label={t('apiKeys.newKey')} style={{ marginTop: 24, padding: 16, border: '1px solid var(--color-rule)' }}>
            <strong>{t('apiKeys.newKey')}</strong>
            <p>{t('apiKeys.showOnce')}</p>
            <textarea
              readOnly
              aria-label={t('apiKeys.token')}
              className="input"
              value={created.token}
              rows={4}
              style={{ width: '100%', fontFamily: 'var(--font-mono)', fontSize: 12, wordBreak: 'break-all' }}
              onFocus={e => e.target.select()}
            />
            <div style={{ display: 'flex', gap: 12, alignItems: 'center', marginTop: 8 }}>
              <button className="btn-secondary" onClick={copy}>{copied ? t('apiKeys.copied') : t('apiKeys.copy')}</button>
              <button className="btn-secondary" onClick={() => { setCreated(null); create.reset() }}>{t('apiKeys.done')}</button>
            </div>
            <p style={{ marginTop: 12 }}>{t('apiKeys.usage')}</p>
            <pre style={{ fontSize: 12, whiteSpace: 'pre-wrap', wordBreak: 'break-all' }}>
              {`curl -H "Authorization: Bearer <${t('apiKeys.token').toLowerCase()}>" ${window.location.origin}/api/seasons`}
            </pre>
          </section>
        )}

        {(keys.isError || create.isError || revoke.isError) && <p role="alert">{t('apiKeys.error')}</p>}
        {keys.data?.length === 0 && <p style={{ marginTop: 24 }}>{t('apiKeys.none')}</p>}
        {keys.data?.map(key => (
          <article key={key.id} style={{ borderTop: '1px solid var(--color-rule)', padding: '20px 0', marginTop: 24 }}>
            <strong>{key.name}</strong>
            <p>
              {t('apiKeys.createdAt', { date: date(key.createdAt) })} · {t('apiKeys.expiresAt', { date: date(key.expiresAt) })} ·{' '}
              {key.lastUsedAt ? t('apiKeys.lastUsedAt', { date: date(key.lastUsedAt) }) : t('apiKeys.neverUsed')}
            </p>
            <button className="btn-secondary" disabled={revoke.isPending} onClick={() => revoke.mutate(key.id)}>
              {t('apiKeys.revoke')}
            </button>
          </article>
        ))}
      </div>
    </div>
  )
}
