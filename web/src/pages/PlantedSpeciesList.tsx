import { useQuery } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { useState, useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import { api } from '../api/client'
import { Masthead, Ledger } from '../components/faltet'
import { ErrorDisplay } from '../components/ErrorDisplay'
import { OnboardingHint } from '../onboarding/OnboardingHint'

export function PlantedSpeciesList() {
  const navigate = useNavigate()
  const { data, error, isLoading, refetch } = useQuery({
    queryKey: ['species-summary'],
    queryFn: api.plants.speciesSummary,
  })

  const { t } = useTranslation()
  const [search, setSearch] = useState('')

  const filtered = useMemo(() =>
    data?.filter(s =>
      s.speciesName.toLowerCase().includes(search.toLowerCase()) ||
      (s.scientificName?.toLowerCase().includes(search.toLowerCase()))
    ) ?? [],
    [data, search]
  )

  if (isLoading) return <div className="flex justify-center p-16"><div className="animate-spin h-8 w-8 border-2 border-accent border-t-transparent rounded-full" /></div>
  if (error) return <ErrorDisplay error={error} onRetry={refetch} />

  return (
    <div>
      <Masthead
        left={t('nav.plants')}
        center="— Växtliggaren —"
      />
      <OnboardingHint />

      <div data-onboarding="plant-actions" style={{ padding: '28px 40px' }}>
        <input
          aria-label={t('common.searchSpecies')}
          value={search}
          onChange={e => setSearch(e.target.value)}
          placeholder={t('common.searchSpecies')}
          className="input"
          style={{ marginBottom: 22, width: '100%' }}
        />

        <Ledger
          paginated
          pageSize={50}
          columns={[
            {
              key: 'id',
              label: '№',
              width: '60px',
              render: (_p, i) => (
                <span style={{ fontFamily: 'var(--font-display)', fontStyle: 'italic', fontSize: 22, color: 'var(--color-sage)' }}>
                  {String(i + 1).padStart(2, '0')}
                </span>
              ),
            },
            {
              key: 'name',
              label: t('species.colName'),
              width: '1.5fr',
              render: (p) => (
                <div>
                  <div style={{ fontFamily: 'var(--font-display)', fontSize: 20 }}>{p.speciesName}</div>
                  {p.scientificName && (
                    <div style={{ fontFamily: 'var(--font-display)', fontStyle: 'italic', fontSize: 13, color: 'var(--color-sage)' }}>
                      {p.scientificName}
                    </div>
                  )}
                </div>
              ),
            },
            {
              key: 'count',
              label: t('bed.plants.col.count'),
              width: '100px',
              align: 'right',
              render: (p) => (
                <span style={{ fontVariantNumeric: 'tabular-nums' }}>{p.activePlantCount}</span>
              ),
            },
            {
              key: 'goto',
              label: '',
              width: '40px',
              align: 'right',
              render: () => (
                <span style={{ color: 'var(--color-sage)', fontFamily: 'var(--font-mono)' }}>→</span>
              ),
            },
          ]}
          rows={filtered}
          rowKey={(p) => p.speciesId}
          onRowClick={(p) => navigate(`/species/${p.speciesId}/plants`)}
        />
      </div>
    </div>
  )
}
