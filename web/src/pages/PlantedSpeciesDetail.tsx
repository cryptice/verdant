import { useQuery } from '@tanstack/react-query'
import { useParams, useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { api } from '../api/client'
import { Masthead, Ledger } from '../components/faltet'
import { ErrorDisplay } from '../components/ErrorDisplay'

export function PlantedSpeciesDetail() {
  const { speciesId } = useParams<{ speciesId: string }>()
  const id = Number(speciesId)
  const navigate = useNavigate()
  const { t } = useTranslation()

  const { data: summary } = useQuery({
    queryKey: ['species-summary'],
    queryFn: api.plants.speciesSummary,
    select: (list) => list.find(s => s.speciesId === id),
  })

  const { data: locations, error, isLoading, refetch } = useQuery({
    queryKey: ['species-locations', id],
    queryFn: () => api.plants.speciesLocations(id),
  })

  if (isLoading) return (
    <div className="flex justify-center p-16">
      <div className="animate-spin h-8 w-8 border-2 border-accent border-t-transparent rounded-full" />
    </div>
  )
  if (error) return <ErrorDisplay error={error} onRetry={refetch} />

  const rows = locations ?? []

  return (
    <div>
      <Masthead
        left={
          <span>
            {t('nav.plants')} /{' '}
            <span style={{ color: 'var(--color-clay)' }}>{summary?.speciesName ?? t('species.title')}</span>
          </span>
        }
      />

      <div style={{ padding: '28px 40px' }}>
        {/* Hero */}
        <h1
          style={{
            fontFamily: 'var(--font-display)',
            fontSize: 60,
            fontWeight: 300,
            letterSpacing: -1,
            margin: 0,
            fontVariationSettings: '"SOFT" 100, "opsz" 144',
          }}
        >
          {summary?.speciesName ?? t('species.title')}
          <span style={{ color: 'var(--color-clay)' }}>.</span>
        </h1>
        {summary?.scientificName && (
          <p
            style={{
              fontFamily: 'var(--font-display)',
              fontStyle: 'italic',
              fontSize: 14,
              color: 'var(--color-sage)',
              marginTop: 6,
            }}
          >
            {summary.scientificName}
          </p>
        )}

        {/* Ledger of plant location groups */}
        <div style={{ marginTop: 28 }}>
          <Ledger
            columns={[
              {
                key: 'index',
                label: '№',
                width: '60px',
                render: (_row, i) => (
                  <span
                    style={{
                      fontFamily: 'var(--font-display)',
                      fontStyle: 'italic',
                      fontSize: 22,
                      color: 'var(--color-sage)',
                    }}
                  >
                    {String(i + 1).padStart(2, '0')}
                  </span>
                ),
              },
              {
                key: 'location',
                label: t('plant.col.bed'),
                width: '1.5fr',
                render: (row) => {
                  const label = row.bedName
                    ? `${row.gardenName} — ${row.bedName}`
                    : (row.gardenName || t('sow.tray'))
                  return (
                    <span style={{ fontFamily: 'var(--font-display)', fontSize: 18 }}>
                      {label}
                    </span>
                  )
                },
              },
              {
                key: 'status',
                label: t('plant.col.status'),
                width: '120px',
                render: (row) => (
                  <span
                    style={{
                      fontFamily: 'var(--font-mono)',
                      fontSize: 10,
                      letterSpacing: 1.4,
                      textTransform: 'uppercase',
                    }}
                  >
                    {t(`status.${row.status}`, { defaultValue: row.status })}
                  </span>
                ),
              },
              {
                key: 'count',
                label: '',
                width: '60px',
                align: 'right',
                render: (row) => (
                  <span style={{ fontFamily: 'var(--font-display)', fontSize: 18 }}>
                    {row.count}
                  </span>
                ),
              },
              {
                key: 'goto',
                label: '',
                width: '40px',
                align: 'right',
                render: () => (
                  <span style={{ color: 'var(--color-clay)', fontFamily: 'var(--font-mono)' }}>→</span>
                ),
              },
            ]}
            rows={rows}
            rowKey={(row) => `${row.gardenName}-${row.bedName ?? ''}-${row.status}-${row.year}`}
            onRowClick={(row) => row.bedId != null && navigate(`/bed/${row.bedId}`)}
          />
        </div>

        {rows.length === 0 && (
          <p
            style={{
              fontFamily: 'var(--font-display)',
              fontStyle: 'italic',
              color: 'var(--color-forest)',
              textAlign: 'center',
              marginTop: 20,
            }}
          >
            {t('species.noActivePlants')}
          </p>
        )}
      </div>
    </div>
  )
}
