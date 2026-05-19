import { useMemo, useState } from 'react'
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { api, type PlantResponse } from '../api/client'
import { Masthead, Ledger, Chip } from '../components/faltet'
import { ErrorDisplay } from '../components/ErrorDisplay'

/**
 * Picks a single plant by status, then routes to the requested activity.
 *
 * URL: /activity/plant-picker/:statuses/:nextKind?speciesId=…
 *   :statuses   comma-separated PlantStatus values (e.g. "POTTED_UP,PLANTED_OUT")
 *   :nextKind   one of {pot-up,plant-out,harvest,recover,discard}
 *   speciesId   optional query param to narrow to a single species
 */
export function PlantPicker() {
  const { statuses: statusesRaw, nextKind } = useParams<{ statuses: string; nextKind: string }>()
  const [params] = useSearchParams()
  const speciesIdParam = params.get('speciesId')
  const speciesId = speciesIdParam ? Number(speciesIdParam) : null
  const navigate = useNavigate()
  const { t } = useTranslation()

  const allowedStatuses = useMemo(
    () => (statusesRaw?.split(',').filter(Boolean) ?? []),
    [statusesRaw],
  )

  // Backend supports a single status filter; pull each separately and merge.
  const plantQueries = useQuery({
    queryKey: ['plant-picker', allowedStatuses, speciesId],
    queryFn: async () => {
      if (allowedStatuses.length === 0) return api.plants.list()
      const lists = await Promise.all(allowedStatuses.map((s) => api.plants.list(s)))
      const seen = new Set<number>()
      const merged: PlantResponse[] = []
      for (const list of lists) {
        for (const p of list) {
          if (!seen.has(p.id)) { seen.add(p.id); merged.push(p) }
        }
      }
      return merged
    },
  })

  const [search, setSearch] = useState('')

  if (plantQueries.isLoading) {
    return (
      <div className="flex justify-center p-16">
        <div className="animate-spin h-8 w-8 border-2 border-accent border-t-transparent rounded-full" />
      </div>
    )
  }
  if (plantQueries.error) {
    return <ErrorDisplay error={plantQueries.error} onRetry={plantQueries.refetch} />
  }

  let plants = plantQueries.data ?? []
  if (speciesId !== null) plants = plants.filter((p) => p.speciesId === speciesId)
  if (search.trim()) {
    const tokens = search.toLowerCase().split(/\s+/).filter(Boolean)
    plants = plants.filter((p) => {
      const hay = `${p.name} ${p.speciesName ?? ''}`.toLowerCase()
      return tokens.every((tok) => hay.includes(tok))
    })
  }

  const titleKey = nextKind && ['pot-up','plant-out','harvest','recover','discard'].includes(nextKind)
    ? `activity.${nextKind === 'pot-up' ? 'potUp' : nextKind === 'plant-out' ? 'plantOut' : nextKind}`
    : 'plantPicker.title'

  return (
    <div>
      <Masthead
        left={
          <Link to="/plants" className="text-sm text-text-secondary hover:text-accent">
            ← {t('nav.plants')}
          </Link>
        }
        center={t('plantPicker.center', { action: t(titleKey) })}
      />

      <div className="page-body">
        <input
          aria-label={t('plantPicker.searchPlaceholder')}
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder={t('plantPicker.searchPlaceholder')}
          className="input"
          style={{ marginBottom: 18 }}
        />

        <Ledger
          paginated
          pageSize={50}
          columns={[
            { key: 'name', label: t('plantPicker.name'), width: '1.6fr',
              render: (p: PlantResponse) => (
                <span style={{ fontFamily: 'var(--font-display)', fontSize: 18 }}>{p.name}</span>
              ),
            },
            { key: 'species', label: t('plantPicker.species'), width: '1.2fr',
              render: (p: PlantResponse) => (
                <span style={{ fontFamily: 'var(--font-display)', fontStyle: 'italic', fontSize: 14, color: 'var(--color-sage)' }}>
                  {p.speciesName ?? '—'}
                </span>
              ),
            },
            { key: 'status', label: t('plantPicker.status'), width: '120px',
              render: (p: PlantResponse) => (
                <Chip tone="sage">{t(`status.${p.status}`, { defaultValue: p.status })}</Chip>
              ),
            },
            { key: 'goto', label: '', width: '40px', align: 'right',
              render: () => <span style={{ color: 'var(--color-accent)' }}>→</span>,
            },
          ]}
          rows={plants}
          rowKey={(p: PlantResponse) => p.id}
          onRowClick={(p: PlantResponse) => {
            if (nextKind) navigate(`/plant/${p.id}/activity/${nextKind}`)
            else navigate(`/plant/${p.id}`)
          }}
          emptyMessage={t('plantPicker.empty')}
        />
      </div>
    </div>
  )
}
