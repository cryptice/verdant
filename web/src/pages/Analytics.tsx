import { useQuery } from '@tanstack/react-query'
import { useState, useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import { api, type SpeciesResponse } from '../api/client'
import { Masthead, Chip } from '../components/faltet'
import { ErrorDisplay } from '../components/ErrorDisplay'
import { SpeciesAutocomplete } from '../components/SpeciesAutocomplete'

export function Analytics() {
  const { t } = useTranslation()

  const { data: summaries, error: summariesError, isLoading: summariesLoading, refetch: summariesRefetch } = useQuery({
    queryKey: ['analytics-season-summaries'],
    queryFn: () => api.analytics.seasonSummaries(),
  })

  const [compSpecies, setCompSpecies] = useState<SpeciesResponse | null>(null)
  const { data: comparison, isLoading: compLoading } = useQuery({
    queryKey: ['analytics-species-comparison', compSpecies?.id],
    queryFn: () => api.analytics.speciesComparison(compSpecies!.id),
    enabled: compSpecies !== null,
  })

  const maxStems = useMemo(() => {
    if (!comparison?.seasons) return 1
    return Math.max(1, ...comparison.seasons.map(s => s.stemsHarvested))
  }, [comparison])

  const { data: yieldData } = useQuery({
    queryKey: ['analytics-yield-per-bed'],
    queryFn: () => api.analytics.yieldPerBed(),
  })

  const allSeasons = useMemo(() => {
    if (!yieldData) return [] as { seasonId: number; seasonName: string }[]
    const map = new Map<number, string>()
    yieldData.forEach(bed => {
      bed.seasons.forEach(s => {
        if (!map.has(s.seasonId)) map.set(s.seasonId, s.seasonName)
      })
    })
    return Array.from(map.entries()).map(([seasonId, seasonName]) => ({ seasonId, seasonName }))
  }, [yieldData])

  if (summariesLoading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', padding: 64 }}>
        <div style={{ width: 32, height: 32, border: '2px solid var(--color-ink)', borderTopColor: 'transparent', borderRadius: '50%', animation: 'spin 0.8s linear infinite' }} />
      </div>
    )
  }
  if (summariesError) return <ErrorDisplay error={summariesError} onRetry={summariesRefetch} />

  return (
    <div>
      <Masthead left={t('nav.analytics')} center={t('analytics.masthead.center')} />

      <div className="page-body" style={{ display: 'flex', flexDirection: 'column', gap: 40 }}>

        {/* § Season overview */}
        <section>
          <SectionHeading title={t('analytics.section.seasonOverview')} />
          {(!summaries || summaries.length === 0) ? (
            <EmptyBlock label={t('analytics.noData')} />
          ) : (
            <div style={{ display: 'flex', gap: 20, overflowX: 'auto', paddingBottom: 8 }}>
              {summaries.map(s => (
                <div
                  key={s.seasonId}
                  style={{
                    flex: '0 0 280px',
                    background: 'var(--color-paper)',
                    border: '1px solid var(--color-ink)',
                    padding: '22px 28px',
                  }}
                >
                  <div style={{ display: 'flex', justifyContent: 'space-between', fontFamily: 'var(--font-mono)', fontSize: 10, letterSpacing: 1.4, textTransform: 'uppercase', color: 'var(--color-forest)' }}>
                    <span>{s.seasonName}</span>
                    <span>{s.year}</span>
                  </div>
                  <div style={{
                    fontFamily: 'var(--font-display)', fontSize: 44, fontWeight: 300, letterSpacing: -0.6, marginTop: 14,
                    fontVariationSettings: '"SOFT" 100, "opsz" 144',
                  }}>
                    {s.totalStemsHarvested.toLocaleString()}
                  </div>
                  <div style={{ fontFamily: 'var(--font-mono)', fontSize: 9, letterSpacing: 1.4, textTransform: 'uppercase', color: 'var(--color-forest)', opacity: 0.7 }}>
                    {t('analytics.card.stems')}
                  </div>
                  <div style={{ marginTop: 14, borderTop: '1px solid color-mix(in srgb, var(--color-ink) 20%, transparent)', paddingTop: 14, display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10 }}>
                    <Meta label={t('analytics.card.plants')} value={s.totalPlants.toString()} />
                    <Meta label={t('analytics.card.species')} value={s.speciesCount.toString()} />
                    {s.topSpecies.length > 0 && (
                      <div style={{ gridColumn: '1 / -1' }}>
                        <div style={{ fontFamily: 'var(--font-mono)', fontSize: 9, letterSpacing: 1.4, textTransform: 'uppercase', color: 'var(--color-forest)', opacity: 0.7, marginBottom: 4 }}>
                          {t('analytics.card.topSpecies')}
                        </div>
                        <Chip tone="clay">{s.topSpecies[0].speciesName}</Chip>
                      </div>
                    )}
                  </div>
                </div>
              ))}
            </div>
          )}
        </section>

        {/* § Species comparison */}
        <section>
          <SectionHeading title={t('analytics.section.speciesComparison')} />
          <div style={{ marginBottom: 22, maxWidth: 360 }}>
            <SpeciesAutocomplete value={compSpecies} onChange={setCompSpecies} />
          </div>

          {compSpecies === null ? (
            <EmptyBlock label={t('analytics.selectSpecies')} />
          ) : compLoading ? (
            <EmptyBlock label={t('analytics.loading')} />
          ) : !comparison?.seasons || comparison.seasons.length === 0 ? (
            <EmptyBlock label={t('analytics.noData')} />
          ) : (
            <div>
              {comparison.seasons.map(season => {
                const pct = Math.round((season.stemsHarvested / maxStems) * 100)
                return (
                  <div
                    key={season.seasonId}
                    style={{
                      display: 'grid',
                      gridTemplateColumns: '120px 100px 1fr 70px',
                      gap: 18,
                      alignItems: 'center',
                      padding: '14px 0',
                      borderBottom: '1px solid color-mix(in srgb, var(--color-ink) 20%, transparent)',
                    }}
                  >
                    <span style={{ fontFamily: 'var(--font-mono)', fontSize: 10, letterSpacing: 1.4, textTransform: 'uppercase', color: 'var(--color-forest)' }}>
                      {season.seasonName}
                    </span>
                    <span style={{ fontFamily: 'var(--font-display)', fontSize: 20, fontVariantNumeric: 'tabular-nums' }}>
                      {season.stemsHarvested.toLocaleString()}
                    </span>
                    <div style={{ height: 8, background: 'var(--color-paper)', border: '1px solid var(--color-ink)' }}>
                      <div style={{ width: `${pct}%`, height: '100%', background: 'var(--color-accent)' }} />
                    </div>
                    <span style={{ fontFamily: 'var(--font-mono)', fontSize: 10, letterSpacing: 1.4, textTransform: 'uppercase', color: 'var(--color-forest)', opacity: 0.7, textAlign: 'right' }}>
                      {pct}%
                    </span>
                  </div>
                )
              })}
            </div>
          )}
        </section>

        {/* § Yield per bed */}
        <section>
          <SectionHeading title={t('analytics.section.yieldPerBed')} />

          {(!yieldData || yieldData.length === 0) ? (
            <EmptyBlock label={t('analytics.noData')} />
          ) : (
            <div>
              {/* Header */}
              <div style={{
                display: 'grid',
                gridTemplateColumns: `180px repeat(${allSeasons.length}, 1fr)`,
                gap: 18,
                padding: '10px 0',
                borderBottom: '1px solid var(--color-ink)',
                fontFamily: 'var(--font-mono)', fontSize: 9, letterSpacing: 1.4, textTransform: 'uppercase', color: 'var(--color-forest)', opacity: 0.7,
              }}>
                <span>{t('analytics.card.bed')}</span>
                {allSeasons.map(col => <span key={col.seasonId} style={{ textAlign: 'right' }}>{col.seasonName}</span>)}
              </div>

              {/* Rows */}
              {yieldData.map(bed => (
                <div
                  key={bed.bedId}
                  style={{
                    display: 'grid',
                    gridTemplateColumns: `180px repeat(${allSeasons.length}, 1fr)`,
                    gap: 18,
                    padding: '12px 0',
                    borderBottom: '1px solid color-mix(in srgb, var(--color-ink) 20%, transparent)',
                    alignItems: 'center',
                  }}
                >
                  <span style={{ fontFamily: 'var(--font-display)', fontSize: 20 }}>{bed.bedName}</span>
                  {allSeasons.map(col => {
                    const season = bed.seasons.find(s => s.seasonId === col.seasonId)
                    return (
                      <span
                        key={col.seasonId}
                        style={{
                          textAlign: 'right',
                          fontFamily: 'var(--font-display)',
                          fontSize: 18,
                          fontVariantNumeric: 'tabular-nums',
                          color: season ? 'var(--color-ink)' : 'color-mix(in srgb, var(--color-accent) 40%, transparent)',
                        }}
                      >
                        {season ? season.stemsHarvested.toLocaleString() : '—'}
                      </span>
                    )
                  })}
                </div>
              ))}
            </div>
          )}
        </section>

      </div>
    </div>
  )
}

function SectionHeading({ title }: { title: string }) {
  return (
    <div style={{ marginBottom: 14 }}>
      <h2 style={{ fontFamily: 'var(--font-display)', fontStyle: 'italic', fontSize: 30, fontWeight: 300, margin: 0, fontVariationSettings: '"SOFT" 100, "opsz" 144' }}>
        {title}<span style={{ color: 'var(--color-accent)' }}>.</span>
      </h2>
    </div>
  )
}

function EmptyBlock({ label }: { label: string }) {
  return (
    <div className="empty-state">
      <div style={{ fontFamily: 'var(--font-mono)', fontSize: 9, letterSpacing: 1.4, textTransform: 'uppercase', color: 'var(--color-forest)', opacity: 0.7 }}>
        {label}
      </div>
    </div>
  )
}

function Meta({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <div style={{ fontFamily: 'var(--font-mono)', fontSize: 9, letterSpacing: 1.4, textTransform: 'uppercase', color: 'var(--color-forest)', opacity: 0.7 }}>
        {label}
      </div>
      <div style={{ fontFamily: 'var(--font-display)', fontSize: 18 }}>
        {value}
      </div>
    </div>
  )
}
