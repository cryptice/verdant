import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { api } from '../api/client'
import { PageHeader } from '../components/PageHeader'
import { ErrorDisplay } from '../components/ErrorDisplay'

export function PlantedSpeciesList() {
  const { data, error, isLoading, refetch } = useQuery({
    queryKey: ['species-summary'],
    queryFn: api.plants.speciesSummary,
  })

  const { t } = useTranslation()
  const [search, setSearch] = useState('')

  if (isLoading) return <div className="flex justify-center p-16"><div className="animate-spin h-8 w-8 border-2 border-accent border-t-transparent rounded-full" /></div>
  if (error) return <ErrorDisplay error={error} onRetry={refetch} />

  const filtered = data?.filter(s =>
    s.speciesName.toLowerCase().includes(search.toLowerCase()) ||
    (s.scientificName?.toLowerCase().includes(search.toLowerCase()))
  ) ?? []

  return (
    <div>
      <PageHeader title={t('species.plantedTitle')} />
      <div className="px-4 py-4 space-y-3">
        <input
          value={search}
          onChange={e => setSearch(e.target.value)}
          placeholder={t('common.searchSpecies')}
          className="input"
        />

        {filtered.length === 0 && (
          <p className="text-text-secondary text-sm text-center py-4">{t('species.noActivePlants')}</p>
        )}

        {filtered.map(s => (
          <Link key={s.speciesId} to={`/species/${s.speciesId}/plants`} className="card flex items-center justify-between no-underline text-inherit">
            <div>
              <p className="font-semibold">{s.speciesName}</p>
              {s.scientificName && <p className="text-xs text-text-secondary">{s.scientificName}</p>}
            </div>
            <span className="bg-accent/15 text-accent text-xs font-bold px-2.5 py-0.5 rounded-full">
              {s.activePlantCount}
            </span>
          </Link>
        ))}
      </div>
    </div>
  )
}
