import { useQuery } from '@tanstack/react-query'
import { useState, useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import { api, type SpeciesResponse, type PlantResponse, type PlantEventResponse } from '../api/client'
import { PageHeader } from '../components/PageHeader'
import { ErrorDisplay } from '../components/ErrorDisplay'

const MONTHS = ['J', 'F', 'M', 'A', 'M', 'J', 'J', 'A', 'S', 'O', 'N', 'D']

interface SpeciesRow {
  speciesId: number
  name: string
  sowRange?: [number, number]   // month indices 0-11
  bloomRange?: [number, number]
  harvestRange?: [number, number]
}

function monthIndex(dateStr: string): number {
  return new Date(dateStr).getMonth()
}

function parseBloomMonths(bloomMonths: string | undefined): [number, number] | undefined {
  if (!bloomMonths) return undefined
  const parts = bloomMonths.split(',').map(s => parseInt(s.trim(), 10)).filter(n => !isNaN(n))
  if (parts.length === 0) return undefined
  return [Math.min(...parts) - 1, Math.max(...parts) - 1]
}

function barStyle(range: [number, number], color: string, top: string) {
  const left = `${(range[0] / 12) * 100}%`
  const width = `${(((range[1] - range[0]) + 1) / 12) * 100}%`
  return {
    position: 'absolute' as const,
    left,
    width,
    top,
    height: '6px',
    borderRadius: '3px',
    backgroundColor: color,
  }
}

export function CropCalendar() {
  const { t, i18n } = useTranslation()
  const [seasonId, setSeasonId] = useState<number | undefined>(undefined)

  const { data: seasons } = useQuery({
    queryKey: ['seasons'],
    queryFn: () => api.seasons.list(),
  })

  const { data: speciesList } = useQuery({
    queryKey: ['species'],
    queryFn: () => api.species.list(),
  })

  const { data: plants, error, isLoading, refetch } = useQuery({
    queryKey: ['plants-for-calendar', seasonId],
    queryFn: () => api.plants.list(),
    enabled: seasonId !== undefined,
  })

  // Fetch events for all plants that belong to selected season's species
  const plantIds = plants?.map(p => p.id) ?? []
  const { data: allEvents } = useQuery({
    queryKey: ['plant-events-calendar', plantIds.join(',')],
    queryFn: async () => {
      if (!plants || plants.length === 0) return [] as PlantEventResponse[]
      const eventPromises = plants.map(p => api.plants.events(p.id))
      const results = await Promise.all(eventPromises)
      return results.flat()
    },
    enabled: !!plants && plants.length > 0,
  })

  const rows = useMemo<SpeciesRow[]>(() => {
    if (!speciesList) return []

    const speciesMap = new Map<number, SpeciesResponse>()
    speciesList.forEach(s => speciesMap.set(s.id, s))

    // Group plants by species
    const bySpecies = new Map<number, PlantResponse[]>()
    plants?.forEach(p => {
      if (p.speciesId) {
        const list = bySpecies.get(p.speciesId) ?? []
        list.push(p)
        bySpecies.set(p.speciesId, list)
      }
    })

    // Group events by plant
    const eventsByPlant = new Map<number, PlantEventResponse[]>()
    allEvents?.forEach(e => {
      const list = eventsByPlant.get(e.plantId) ?? []
      list.push(e)
      eventsByPlant.set(e.plantId, list)
    })

    const result: SpeciesRow[] = []
    const processedSpeciesIds = new Set<number>()

    bySpecies.forEach((speciesPlants, speciesId) => {
      processedSpeciesIds.add(speciesId)
      const sp = speciesMap.get(speciesId)
      if (!sp) return

      const name = i18n.language === 'sv'
        ? `${sp.commonNameSv ?? sp.commonName}${sp.variantNameSv ? ` — ${sp.variantNameSv}` : (sp.variantName ? ` — ${sp.variantName}` : '')}`
        : `${sp.commonName}${sp.variantName ? ` — ${sp.variantName}` : ''}`

      // Collect event months
      const sowMonths: number[] = []
      const bloomStartMonths: number[] = []
      const bloomEndMonths: number[] = []
      const harvestMonths: number[] = []

      speciesPlants.forEach(plant => {
        const events = eventsByPlant.get(plant.id) ?? []
        events.forEach(ev => {
          const mi = monthIndex(ev.eventDate)
          if (ev.eventType === 'SEEDED') sowMonths.push(mi)
          if (ev.eventType === 'FIRST_BLOOM') bloomStartMonths.push(mi)
          if (ev.eventType === 'LAST_BLOOM') bloomEndMonths.push(mi)
          if (ev.eventType === 'HARVESTED') harvestMonths.push(mi)
        })
      })

      const sowRange: [number, number] | undefined = sowMonths.length > 0
        ? [Math.min(...sowMonths), Math.max(...sowMonths)]
        : undefined

      let bloomRange: [number, number] | undefined
      if (bloomStartMonths.length > 0 || bloomEndMonths.length > 0) {
        const allBloom = [...bloomStartMonths, ...bloomEndMonths]
        bloomRange = [Math.min(...allBloom), Math.max(...allBloom)]
      } else {
        bloomRange = parseBloomMonths(sp.bloomMonths)
      }

      const harvestRange: [number, number] | undefined = harvestMonths.length > 0
        ? [Math.min(...harvestMonths), Math.max(...harvestMonths)]
        : undefined

      result.push({ speciesId, name, sowRange, bloomRange, harvestRange })
    })

    result.sort((a, b) => a.name.localeCompare(b.name))
    return result
  }, [speciesList, plants, allEvents, seasonId, i18n.language])

  if (isLoading && seasonId !== undefined) return <div className="flex justify-center p-16"><div className="animate-spin h-8 w-8 border-2 border-accent border-t-transparent rounded-full" /></div>
  if (error) return <ErrorDisplay error={error} onRetry={refetch} />

  return (
    <div>
      <PageHeader title={t('calendar.title')} />
      <div className="px-4 py-4">
        {/* Season filter */}
        <div className="mb-4">
          <select
            value={seasonId ?? ''}
            onChange={e => setSeasonId(e.target.value ? Number(e.target.value) : undefined)}
            className="input w-auto"
          >
            <option value="">{t('pestDisease.allSeasons')}</option>
            {seasons?.map(s => (
              <option key={s.id} value={s.id}>{s.name}</option>
            ))}
          </select>
        </div>

        {/* Legend */}
        <div className="flex gap-4 mb-4 text-xs text-text-secondary">
          <span className="flex items-center gap-1">
            <span className="inline-block w-4 h-1.5 rounded-full" style={{ backgroundColor: '#93C5FD' }} />
            {t('calendar.sow')}
          </span>
          <span className="flex items-center gap-1">
            <span className="inline-block w-4 h-1.5 rounded-full" style={{ backgroundColor: '#F9A8D4' }} />
            {t('calendar.bloom')}
          </span>
          <span className="flex items-center gap-1">
            <span className="inline-block w-4 h-1.5 rounded-full" style={{ backgroundColor: '#FDBA74' }} />
            {t('calendar.harvest')}
          </span>
        </div>

        {!seasonId && (
          <p className="text-text-secondary text-sm text-center py-4">{t('calendar.noData')}</p>
        )}

        {seasonId && rows.length === 0 && (
          <p className="text-text-secondary text-sm text-center py-4">{t('calendar.noData')}</p>
        )}

        {seasonId && rows.length > 0 && (
          <div className="border border-divider rounded-xl overflow-hidden bg-bg shadow-sm">
            {/* Month headers */}
            <div className="flex border-b border-divider bg-surface">
              <div className="w-48 min-w-[12rem] shrink-0 px-4 py-2 text-xs font-medium text-text-secondary">
                {t('common.speciesLabel')}
              </div>
              <div className="flex-1 flex">
                {MONTHS.map((m, i) => (
                  <div key={i} className="flex-1 text-center text-xs font-medium text-text-secondary py-2">
                    {m}
                  </div>
                ))}
              </div>
            </div>

            {/* Species rows */}
            {rows.map((row, idx) => (
              <div
                key={row.speciesId}
                className={`flex border-b border-divider last:border-0 ${idx % 2 === 1 ? 'bg-surface/50' : ''}`}
              >
                <div className="w-48 min-w-[12rem] shrink-0 px-4 py-3 text-sm truncate" title={row.name}>
                  {row.name}
                </div>
                <div className="flex-1 relative" style={{ minHeight: '36px' }}>
                  {/* Month grid lines */}
                  <div className="absolute inset-0 flex">
                    {MONTHS.map((_, i) => (
                      <div key={i} className="flex-1 border-l border-divider/30 first:border-l-0" />
                    ))}
                  </div>
                  {/* Bars */}
                  {row.sowRange && (
                    <div style={barStyle(row.sowRange, '#93C5FD', '6px')} />
                  )}
                  {row.bloomRange && (
                    <div style={barStyle(row.bloomRange, '#F9A8D4', '15px')} />
                  )}
                  {row.harvestRange && (
                    <div style={barStyle(row.harvestRange, '#FDBA74', '24px')} />
                  )}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}
