import { useQuery } from '@tanstack/react-query'
import { useParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { api } from '../api/client'
import { PageHeader } from '../components/PageHeader'
import { ErrorDisplay } from '../components/ErrorDisplay'
import { StatusBadge } from '../components/StatusBadge'

export function PlantedSpeciesDetail() {
  const { speciesId } = useParams<{ speciesId: string }>()
  const id = Number(speciesId)
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

  if (isLoading) return <div className="flex justify-center p-16"><div className="animate-spin h-8 w-8 border-2 border-accent border-t-transparent rounded-full" /></div>
  if (error) return <ErrorDisplay error={error} onRetry={refetch} />

  const byLocation = new Map<string, typeof locations>()
  locations?.forEach(loc => {
    const key = loc.bedName ? `${loc.gardenName} — ${loc.bedName}` : (loc.gardenName || t('sow.tray'))
    if (!byLocation.has(key)) byLocation.set(key, [])
    byLocation.get(key)!.push(loc)
  })

  return (
    <div>
      <PageHeader title={summary?.speciesName ?? t('species.title')} back />
      <div className="px-4 py-4 space-y-4">
        {Array.from(byLocation.entries()).map(([location, items]) => (
          <div key={location} className="card">
            <p className="font-medium mb-2">{location}</p>
            {items!.map((item, i) => (
              <div key={i} className="flex items-center justify-between py-1 text-sm">
                <div className="flex items-center gap-2">
                  <StatusBadge status={item.status} />
                  <span>{item.year}</span>
                </div>
                <span className="font-medium">{item.count}</span>
              </div>
            ))}
          </div>
        ))}

        {(!locations || locations.length === 0) && (
          <p className="text-text-secondary text-sm text-center py-4">{t('species.noActivePlants')}</p>
        )}
      </div>
    </div>
  )
}
