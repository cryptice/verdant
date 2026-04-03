import { useQuery } from '@tanstack/react-query'
import { useState, useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import { api, type SpeciesResponse } from '../api/client'
import { PageHeader } from '../components/PageHeader'
import { ErrorDisplay } from '../components/ErrorDisplay'
import { SpeciesAutocomplete } from '../components/SpeciesAutocomplete'
import { OnboardingHint } from '../onboarding/OnboardingHint'

export function Analytics() {
  const { t } = useTranslation()

  // --- Section 1: Season overview ---
  const { data: summaries, error: summariesError, isLoading: summariesLoading, refetch: summariesRefetch } = useQuery({
    queryKey: ['analytics-season-summaries'],
    queryFn: () => api.analytics.seasonSummaries(),
  })

  // --- Section 2: Species comparison ---
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

  // --- Section 3: Yield per bed ---
  const { data: yieldData, isLoading: yieldLoading } = useQuery({
    queryKey: ['analytics-yield-per-bed'],
    queryFn: () => api.analytics.yieldPerBed(),
  })

  // Collect all unique season columns
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

  if (summariesLoading) return <div className="flex justify-center p-16"><div className="animate-spin h-8 w-8 border-2 border-accent border-t-transparent rounded-full" /></div>
  if (summariesError) return <ErrorDisplay error={summariesError} onRetry={summariesRefetch} />

  return (
    <div>
      <PageHeader title={t('analytics.title')} />
      <OnboardingHint />
      <div data-onboarding="analytics-view" className="px-4 py-4 space-y-8">

        {/* ── Section 1: Season overview cards ── */}
        <section>
          <h2 className="text-sm font-medium text-text-secondary mb-3">{t('analytics.seasonOverview')}</h2>

          {(!summaries || summaries.length === 0) && (
            <p className="text-text-secondary text-sm text-center py-4">{t('analytics.noData')}</p>
          )}

          {summaries && summaries.length > 0 && (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
              {summaries.map(s => (
                <div key={s.seasonId} className="border border-divider rounded-xl bg-bg shadow-sm p-4">
                  <div className="flex items-baseline gap-2 mb-2">
                    <span className="text-sm font-semibold">{s.seasonName}</span>
                    <span className="text-xs text-text-secondary">{s.year}</span>
                  </div>
                  <p className="text-2xl font-bold tabular-nums">{s.totalStemsHarvested.toLocaleString()}</p>
                  <p className="text-xs text-text-secondary mb-2">{t('analytics.totalStems')}</p>
                  <div className="flex gap-4 text-xs text-text-secondary mb-2">
                    <span>{t('analytics.plants')}: {s.totalPlants}</span>
                    <span>{t('analytics.speciesCount')}: {s.speciesCount}</span>
                  </div>
                  {s.topSpecies.length > 0 && (
                    <div>
                      <p className="text-xs text-text-muted mb-0.5">{t('analytics.topSpecies')}</p>
                      <p className="text-xs text-text-secondary">
                        {s.topSpecies.slice(0, 3).map(sp => sp.speciesName).join(', ')}
                      </p>
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}
        </section>

        {/* ── Section 2: Species comparison ── */}
        <section>
          <h2 className="text-sm font-medium text-text-secondary mb-3">{t('analytics.speciesComparison')}</h2>
          <div className="mb-3 max-w-xs">
            <SpeciesAutocomplete
              value={compSpecies}
              onChange={setCompSpecies}
              placeholder={t('analytics.selectSpecies')}
            />
          </div>

          {!compSpecies && (
            <p className="text-text-secondary text-sm py-2">{t('analytics.selectSpecies')}</p>
          )}

          {compSpecies && compLoading && (
            <div className="flex justify-center py-4">
              <div className="animate-spin h-5 w-5 border-2 border-accent border-t-transparent rounded-full" />
            </div>
          )}

          {compSpecies && comparison && !compLoading && (
            <div className="border border-divider rounded-xl overflow-hidden bg-bg shadow-sm">
              <table className="w-full">
                <thead>
                  <tr className="border-b border-divider bg-surface">
                    <th className="text-left px-4 py-2 text-xs font-medium text-text-secondary">{t('seasons.title')}</th>
                    <th className="text-right px-4 py-2 text-xs font-medium text-text-secondary">{t('trials.plantCount')}</th>
                    <th className="text-right px-4 py-2 text-xs font-medium text-text-secondary">{t('analytics.totalStems')}</th>
                    <th className="text-right px-4 py-2 text-xs font-medium text-text-secondary">{t('analytics.stemsPerPlant')}</th>
                    <th className="text-right px-4 py-2 text-xs font-medium text-text-secondary">{t('analytics.avgLength')}</th>
                    <th className="text-right px-4 py-2 text-xs font-medium text-text-secondary">{t('analytics.avgVaseLife')}</th>
                    <th className="px-4 py-2 text-xs font-medium text-text-secondary w-32"></th>
                  </tr>
                </thead>
                <tbody>
                  {comparison.seasons.map(s => (
                    <tr key={s.seasonId} className="border-b border-divider last:border-0">
                      <td className="px-4 py-2.5 text-sm">
                        {s.seasonName} <span className="text-text-secondary text-xs">({s.year})</span>
                      </td>
                      <td className="px-4 py-2.5 text-sm text-right tabular-nums">{s.plantCount}</td>
                      <td className="px-4 py-2.5 text-sm text-right tabular-nums">{s.stemsHarvested}</td>
                      <td className="px-4 py-2.5 text-sm text-right tabular-nums">{s.stemsPerPlant != null ? s.stemsPerPlant.toFixed(1) : '—'}</td>
                      <td className="px-4 py-2.5 text-sm text-right tabular-nums">{s.avgStemLength != null ? `${s.avgStemLength.toFixed(0)} cm` : '—'}</td>
                      <td className="px-4 py-2.5 text-sm text-right tabular-nums">{s.avgVaseLife != null ? `${s.avgVaseLife.toFixed(0)} d` : '—'}</td>
                      <td className="px-4 py-2.5">
                        <div className="h-2 rounded-full bg-surface overflow-hidden">
                          <div
                            className="h-full rounded-full bg-accent"
                            style={{ width: `${(s.stemsHarvested / maxStems) * 100}%` }}
                          />
                        </div>
                      </td>
                    </tr>
                  ))}
                  {comparison.seasons.length === 0 && (
                    <tr>
                      <td colSpan={7} className="px-4 py-4 text-sm text-text-secondary text-center">{t('analytics.noData')}</td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          )}
        </section>

        {/* ── Section 3: Yield per bed ── */}
        <section>
          <h2 className="text-sm font-medium text-text-secondary mb-3">{t('analytics.yieldPerBed')}</h2>

          {yieldLoading && (
            <div className="flex justify-center py-4">
              <div className="animate-spin h-5 w-5 border-2 border-accent border-t-transparent rounded-full" />
            </div>
          )}

          {yieldData && yieldData.length === 0 && !yieldLoading && (
            <p className="text-text-secondary text-sm text-center py-4">{t('analytics.noData')}</p>
          )}

          {yieldData && yieldData.length > 0 && !yieldLoading && (
            <div className="border border-divider rounded-xl overflow-hidden bg-bg shadow-sm overflow-x-auto">
              <table className="w-full">
                <thead>
                  <tr className="border-b border-divider bg-surface">
                    <th className="text-left px-4 py-2 text-xs font-medium text-text-secondary">{t('sow.bedLabel')}</th>
                    <th className="text-left px-4 py-2 text-xs font-medium text-text-secondary">{t('analytics.area')}</th>
                    {allSeasons.map(s => (
                      <th key={s.seasonId} className="text-right px-4 py-2 text-xs font-medium text-text-secondary" colSpan={2}>
                        {s.seasonName}
                      </th>
                    ))}
                  </tr>
                  <tr className="border-b border-divider bg-surface">
                    <th className="px-4 py-1"></th>
                    <th className="px-4 py-1"></th>
                    {allSeasons.map(s => (
                      <th key={`${s.seasonId}-sub`} className="text-right px-4 py-1 text-xs text-text-muted" colSpan={1}>
                        {t('analytics.totalStems')}
                      </th>
                    ))}
                    {allSeasons.map(s => (
                      <th key={`${s.seasonId}-sub2`} className="text-right px-4 py-1 text-xs text-text-muted" colSpan={1}>
                        {t('analytics.stemsPerM2')}
                      </th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {yieldData.map(bed => {
                    const seasonMap = new Map(bed.seasons.map(s => [s.seasonId, s]))
                    return (
                      <tr key={bed.bedId} className="border-b border-divider last:border-0 hover:bg-surface transition-colors">
                        <td className="px-4 py-2.5 text-sm">
                          <span>{bed.bedName}</span>
                          <span className="text-text-secondary text-xs ml-1">({bed.gardenName})</span>
                        </td>
                        <td className="px-4 py-2.5 text-sm text-text-secondary tabular-nums">
                          {bed.areaM2 != null ? `${bed.areaM2.toFixed(1)} m\u00B2` : '—'}
                        </td>
                        {allSeasons.map(s => {
                          const data = seasonMap.get(s.seasonId)
                          return (
                            <td key={s.seasonId} className="px-4 py-2.5 text-sm text-right tabular-nums" colSpan={2}>
                              {data ? (
                                <>
                                  <span>{data.stemsHarvested}</span>
                                  {data.stemsPerM2 != null && (
                                    <span className="text-text-secondary text-xs ml-2">({data.stemsPerM2.toFixed(1)}/m{'\u00B2'})</span>
                                  )}
                                </>
                              ) : '—'}
                            </td>
                          )
                        })}
                      </tr>
                    )
                  })}
                </tbody>
              </table>
            </div>
          )}
        </section>
      </div>
    </div>
  )
}
