import { useQuery } from '@tanstack/react-query'
import { useState, useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import { api, type SpeciesResponse, type PlantResponse, type PlantEventResponse } from '../api/client'
import { Masthead, Chip } from '../components/faltet'
import { ErrorDisplay } from '../components/ErrorDisplay'

const MONTHS = ['J', 'F', 'M', 'A', 'M', 'J', 'J', 'A', 'S', 'O', 'N', 'D']

interface SpeciesRow {
  speciesId: number
  name: string
  scientificName?: string
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

function barStyle(range: [number, number], color: string, top: number): React.CSSProperties {
  const left = `${(range[0] / 12) * 100}%`
  const width = `${(((range[1] - range[0]) + 1) / 12) * 100}%`
  return {
    position: 'absolute',
    left,
    width,
    top,
    height: 6,
    background: color,
  }
}

const selectStyle: React.CSSProperties = {
  backgroundColor: 'transparent',
  border: 'none',
  borderBottom: '1px solid var(--color-ink)',
  borderRadius: 0,
  padding: '4px 0',
  fontFamily: 'var(--font-display)',
  fontSize: 20,
  fontWeight: 300,
  color: 'var(--color-ink)',
  outline: 'none',
  minWidth: 200,
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

    bySpecies.forEach((speciesPlants, speciesId) => {
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

      result.push({ speciesId, name, scientificName: sp.scientificName ?? undefined, sowRange, bloomRange, harvestRange })
    })

    result.sort((a, b) => a.name.localeCompare(b.name))
    return result
  }, [speciesList, plants, allEvents, i18n.language])

  if (isLoading && seasonId !== undefined) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', padding: 64 }}>
        <div style={{ width: 32, height: 32, border: '2px solid var(--color-ink)', borderTopColor: 'transparent', borderRadius: '50%', animation: 'spin 0.8s linear infinite' }} />
      </div>
    )
  }
  if (error) return <ErrorDisplay error={error} onRetry={refetch} />

  const MUSTARD = 'var(--color-mustard)'
  const SAGE = 'var(--color-sage)'
  const CLAY = 'var(--color-accent)'

  return (
    <div>
      <Masthead left={t('nav.calendar')} center={t('calendar.masthead.center')} />

      <div style={{ padding: '28px 40px' }}>
        {/* Season selector */}
        <div style={{ marginBottom: 28, display: 'flex', alignItems: 'flex-end', gap: 14 }}>
          <div>
            <div style={{ fontFamily: 'var(--font-mono)', fontSize: 9, letterSpacing: 1.4, textTransform: 'uppercase', color: 'var(--color-forest)', opacity: 0.7, marginBottom: 4 }}>
              {t('calendar.seasonLabel')}
            </div>
            <select
              value={seasonId ?? ''}
              onChange={(e) => setSeasonId(e.target.value ? Number(e.target.value) : undefined)}
              style={selectStyle}
            >
              <option value="">—</option>
              {seasons?.map((s) => (
                <option key={s.id} value={s.id}>{s.name} · {s.year}</option>
              ))}
            </select>
          </div>
        </div>

        {/* Legend */}
        <div style={{ display: 'flex', gap: 10, marginBottom: 22 }}>
          <Chip tone="mustard">{t('calendar.legend.sow')}</Chip>
          <Chip tone="sage">{t('calendar.legend.bloom')}</Chip>
          <Chip tone="clay">{t('calendar.legend.harvest')}</Chip>
        </div>

        {/* Empty state */}
        {(seasonId === undefined || rows.length === 0) && (
          <div style={{ padding: '60px 22px', textAlign: 'center', borderTop: '1px solid var(--color-ink)', borderBottom: '1px solid var(--color-ink)' }}>
            <div style={{ fontFamily: 'var(--font-mono)', fontSize: 9, letterSpacing: 1.4, textTransform: 'uppercase', color: 'var(--color-forest)', opacity: 0.7, marginBottom: 8 }}>
              {t('calendar.emptyTitle')}
            </div>
            <div style={{ fontFamily: 'var(--font-display)', fontStyle: 'italic', fontSize: 16, color: 'var(--color-forest)' }}>
              {t('calendar.emptyBody')}
            </div>
          </div>
        )}

        {/* Calendar grid */}
        {rows.length > 0 && (
          <div>
            {/* Header row */}
            <div style={{ display: 'grid', gridTemplateColumns: '200px repeat(12, 1fr)', borderBottom: '1px solid var(--color-ink)' }}>
              <div />
              {MONTHS.map((m, i) => (
                <div
                  key={i}
                  style={{
                    textAlign: 'center',
                    padding: '10px 0',
                    fontFamily: 'var(--font-mono)',
                    fontSize: 9,
                    letterSpacing: 1.4,
                    textTransform: 'uppercase',
                    color: 'var(--color-forest)',
                    opacity: 0.7,
                    borderLeft: '1px solid color-mix(in srgb, var(--color-ink) 20%, transparent)',
                  }}
                >
                  {m}
                </div>
              ))}
            </div>

            {/* Species rows */}
            {rows.map((row) => (
              <div
                key={row.speciesId}
                style={{
                  display: 'grid',
                  gridTemplateColumns: '200px repeat(12, 1fr)',
                  alignItems: 'stretch',
                  borderBottom: '1px solid color-mix(in srgb, var(--color-ink) 20%, transparent)',
                  minHeight: 44,
                }}
              >
                <div style={{ padding: '10px 14px 10px 0' }}>
                  <div style={{ fontFamily: 'var(--font-display)', fontSize: 20, fontWeight: 300 }}>{row.name}</div>
                  {row.scientificName && (
                    <div style={{ fontFamily: 'var(--font-display)', fontStyle: 'italic', fontSize: 9, color: 'var(--color-sage)' }}>
                      {row.scientificName}
                    </div>
                  )}
                </div>
                <div style={{ gridColumn: '2 / -1', position: 'relative', borderLeft: '1px solid color-mix(in srgb, var(--color-ink) 20%, transparent)' }}>
                  {row.sowRange && <div style={barStyle(row.sowRange, MUSTARD, 14)} />}
                  {row.bloomRange && <div style={barStyle(row.bloomRange, SAGE, 22)} />}
                  {row.harvestRange && <div style={barStyle(row.harvestRange, CLAY, 30)} />}
                  {/* Vertical month hairlines */}
                  {Array.from({ length: 11 }).map((_, i) => (
                    <div
                      key={i}
                      style={{
                        position: 'absolute',
                        left: `${((i + 1) / 12) * 100}%`,
                        top: 0,
                        bottom: 0,
                        width: 1,
                        background: 'color-mix(in srgb, var(--color-ink) 12%, transparent)',
                      }}
                    />
                  ))}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}
