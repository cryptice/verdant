import { useQuery } from '@tanstack/react-query'
import { Link, useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { api } from '../api/client'
import { ErrorDisplay } from '../components/ErrorDisplay'
import { StatusBadge } from '../components/StatusBadge'
import { useAuth } from '../auth/AuthContext'

function formatWeight(grams: number) {
  return grams >= 1000 ? `${(grams / 1000).toFixed(1)} kg` : `${grams} g`
}

export function Dashboard() {
  const { user } = useAuth()
  const navigate = useNavigate()
  const { t } = useTranslation()

  const { data: dashboard, error, isLoading, refetch } = useQuery({
    queryKey: ['dashboard'],
    queryFn: api.dashboard,
  })

  const { data: tray } = useQuery({
    queryKey: ['tray-summary'],
    queryFn: api.plants.traySummary,
  })

  const { data: harvests } = useQuery({
    queryKey: ['harvest-stats'],
    queryFn: api.stats.harvests,
  })

  if (isLoading) {
    return (
      <div className="flex items-center justify-center p-16">
        <div className="animate-spin h-8 w-8 border-2 border-accent border-t-transparent rounded-full" />
      </div>
    )
  }

  if (error) return <ErrorDisplay error={error} onRetry={refetch} />

  return (
    <div className="space-y-6">
      <div className="flex items-start justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold">{t('dashboard.greeting', { name: user?.displayName?.split(' ')[0] })}</h1>
          {dashboard && (
            <p className="text-text-secondary text-sm mt-1">
              {t('dashboard.gardens', { count: dashboard.stats.totalGardens })} · {t('dashboard.plants', { count: dashboard.stats.totalPlants })}
            </p>
          )}
        </div>
        <button onClick={() => navigate('/garden/new')} className="btn-primary shrink-0">{t('dashboard.newGarden')}</button>
      </div>

      {dashboard && dashboard.gardens.length === 0 && (
        <div className="card text-center py-8">
          <p className="text-4xl mb-2">🌱</p>
          <p className="font-medium">{t('dashboard.noGardens')}</p>
          <p className="text-text-secondary text-sm">{t('dashboard.noGardensHint')}</p>
        </div>
      )}

      {dashboard?.gardens.map(g => (
        <Link key={g.id} to={`/garden/${g.id}`} className="card flex items-center gap-4 no-underline text-inherit">
          <span className="text-3xl">{g.emoji ?? '🌱'}</span>
          <div className="flex-1 min-w-0">
            <p className="font-semibold text-lg">{g.name}</p>
            <p className="text-sm text-text-secondary">
              {t('dashboard.plants', { count: g.plantCount })} · {t('dashboard.beds', { count: g.bedCount })}
            </p>
          </div>
        </Link>
      ))}

      {tray && tray.length > 0 && (
        <section>
          <h2 className="font-bold text-lg mb-2">{t('dashboard.plantsInTrays')}</h2>
          <div className="card space-y-2">
            {tray.map((t2, i) => (
              <div key={i} className="flex items-center justify-between text-sm">
                <span className="flex-1">{t2.speciesName}</span>
                <span className="text-accent font-medium w-8 text-right">{t2.count}</span>
                <span className="w-20 text-right"><StatusBadge status={t2.status} /></span>
              </div>
            ))}
          </div>
        </section>
      )}

      {harvests && harvests.length > 0 && (
        <section>
          <h2 className="font-bold text-lg mb-2">{t('dashboard.harvestStats')}</h2>
          <div className="space-y-3">
            {harvests.map(h => (
              <div key={h.species} className="card">
                <p className="font-bold mb-2">{h.species}</p>
                <div className="flex justify-between text-center text-sm">
                  <div>
                    <p className="font-bold text-accent">{formatWeight(h.totalWeightGrams)}</p>
                    <p className="text-text-secondary text-xs">{t('dashboard.weight')}</p>
                  </div>
                  <div>
                    <p className="font-bold text-accent">{h.totalQuantity}</p>
                    <p className="text-text-secondary text-xs">{t('dashboard.quantity')}</p>
                  </div>
                  <div>
                    <p className="font-bold text-accent">{h.harvestCount}</p>
                    <p className="text-text-secondary text-xs">{t('dashboard.harvests')}</p>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </section>
      )}

      {(!harvests || harvests.length === 0) && (
        <div className="card text-center py-6">
          <p className="text-2xl text-text-secondary/30 mb-1">🌾</p>
          <p className="text-sm text-text-secondary">{t('dashboard.noHarvests')}</p>
        </div>
      )}

    </div>
  )
}
