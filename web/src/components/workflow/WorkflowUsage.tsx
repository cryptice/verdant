import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { api, type SpeciesResponse } from '../../api/client'

const head = { fontFamily: 'var(--font-mono)', fontSize: 9, letterSpacing: 1.4, textTransform: 'uppercase', color: 'var(--color-forest)' } as const

/** The groups whose varieties follow a workflow template, each opening to its varieties. */
export function WorkflowUsage({ templateId }: { templateId: number }) {
  const { t, i18n } = useTranslation()
  const { data: species } = useQuery({ queryKey: ['species'], queryFn: api.species.list })
  if (!species) return null

  const sv = i18n.language === 'sv'
  const name = (s: SpeciesResponse) => {
    const common = sv ? (s.commonNameSv ?? s.commonName) : s.commonName
    const variant = sv ? (s.variantNameSv ?? s.variantName) : s.variantName
    return variant ? `${common} – ${variant}` : common
  }
  const byName = (a: string, b: string) => a.localeCompare(b, 'sv')

  // A variety in several groups shows under each, so every group lists all of its varieties on the workflow.
  const users = species.filter(s => s.workflowTemplateId === templateId)
  const groups = new Map<string, SpeciesResponse[]>()
  for (const s of users) {
    const names = s.groups.length > 0 ? s.groups.map(g => g.name) : [t('workflows.noGroup')]
    for (const g of names) groups.set(g, [...(groups.get(g) ?? []), s])
  }
  const entries = [...groups.entries()].sort(([a], [b]) => byName(a, b))

  return (
    <section style={{ marginTop: 36 }}>
      <div style={{ ...head, marginBottom: 8 }}>
        § {t('workflows.usedBy')} · {t('workflows.varietyCount', { count: users.length })}
      </div>
      {entries.length === 0 ? (
        <p style={{ fontFamily: 'var(--font-display)', fontStyle: 'italic', fontSize: 15, color: 'var(--color-forest)', opacity: 0.7, margin: 0 }}>
          {t('workflows.usedByNone')}
        </p>
      ) : (
        <div style={{ borderTop: '1px solid var(--color-ink)' }}>
          {entries.map(([group, members]) => (
            <details key={group} style={{ borderBottom: '1px solid color-mix(in srgb, var(--color-ink) 20%, transparent)' }}>
              <summary style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline', gap: 12, padding: '10px 0', cursor: 'pointer' }}>
                <span style={{ fontFamily: 'var(--font-display)', fontSize: 18 }}>{group}</span>
                <span style={{ fontFamily: 'var(--font-mono)', fontSize: 11, color: 'var(--color-forest)', whiteSpace: 'nowrap' }}>
                  {t('workflows.varietyCount', { count: members.length })}
                </span>
              </summary>
              <ul style={{ listStyle: 'none', margin: '0 0 10px', padding: '0 0 0 14px' }}>
                {members.map(s => ({ s, label: name(s) })).sort((a, b) => byName(a.label, b.label)).map(({ s, label }) => (
                  <li key={s.id} style={{ padding: '3px 0' }}>
                    <Link to={`/species/${s.id}`} style={{ fontFamily: 'var(--font-display)', fontSize: 15, color: 'var(--color-ink)' }}>{label}</Link>
                  </li>
                ))}
              </ul>
            </details>
          ))}
        </div>
      )}
    </section>
  )
}
