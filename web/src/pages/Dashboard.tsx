import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { api } from '../api/client'
import { ErrorDisplay } from '../components/ErrorDisplay'
import { StatusBadge } from '../components/StatusBadge'
import { useAuth } from '../auth/AuthContext'
import { useOnboarding } from '../onboarding/OnboardingContext'

function formatWeight(grams: number) {
  return grams >= 1000 ? `${(grams / 1000).toFixed(1)} kg` : `${grams} g`
}

export function Dashboard() {
  const { user } = useAuth()
  const { t } = useTranslation()
  const { isActive, completedCount, totalCount, setDrawerOpen } = useOnboarding()

  const { data: dashboard, error, isLoading, refetch } = useQuery({
    queryKey: ['dashboard'],
    queryFn: api.dashboard,
  })

  const { data: tray, error: trayError } = useQuery({
    queryKey: ['tray-summary'],
    queryFn: api.plants.traySummary,
  })

  const { data: harvests, error: harvestsError } = useQuery({
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
      <div>
        <h1 className="text-2xl font-semibold">{t('dashboard.greeting', { name: user?.displayName?.split(' ')[0] })}</h1>
        {dashboard && (
          <p className="text-text-secondary text-sm mt-1">
            {t('dashboard.gardens', { count: dashboard.stats.totalGardens })} · {t('dashboard.plants', { count: dashboard.stats.totalPlants })}
          </p>
        )}
      </div>

      {isActive && (
        <div className="card bg-accent-light border-accent/20">
          <div className="flex items-center gap-4">
            <span className="text-3xl">🌿</span>
            <div className="flex-1">
              <p className="font-semibold">{t('dashboard.onboardingWidget.title')}</p>
              <p className="text-sm text-text-secondary mt-0.5">{t('dashboard.onboardingWidget.description')}</p>
              <div className="flex items-center gap-3 mt-2">
                <div className="flex-1 h-1.5 bg-white/50 rounded-full overflow-hidden">
                  <div
                    className="h-full bg-accent rounded-full transition-all duration-500"
                    style={{ width: `${(completedCount / totalCount) * 100}%` }}
                  />
                </div>
                <span className="text-xs text-text-secondary whitespace-nowrap">
                  {t('dashboard.onboardingWidget.progress', { completed: completedCount, total: totalCount })}
                </span>
              </div>
            </div>
            <button
              onClick={() => setDrawerOpen(true)}
              className="btn-primary shrink-0 text-sm"
            >
              {t('dashboard.onboardingWidget.button')}
            </button>
          </div>
        </div>
      )}

      {dashboard && dashboard.gardens.length > 0 && (
        <section>
          <div className="flex items-center justify-between mb-2">
            <h2 className="font-bold text-lg">{t('nav.gardens')}</h2>
            <Link to="/gardens" className="text-sm text-accent hover:underline">{t('dashboard.viewAll')}</Link>
          </div>
          <div className="space-y-3">
            {dashboard.gardens.map((g, i) => (
              <Link key={g.id} to={`/garden/${g.id}`} data-onboarding={i === 0 ? 'garden-card' : undefined} className="card flex items-center gap-4 no-underline text-inherit group">
                <span className="text-4xl group-hover:scale-110 transition-transform duration-200">{g.emoji ?? '🌱'}</span>
                <div className="flex-1 min-w-0">
                  <p className="font-semibold text-lg">{g.name}</p>
                  <p className="text-sm text-text-secondary">
                    {t('dashboard.plants', { count: g.plantCount })} · {t('dashboard.beds', { count: g.bedCount })}
                  </p>
                </div>
              </Link>
            ))}
          </div>
        </section>
      )}

      {trayError && (
        <section>
          <h2 className="font-bold text-lg mb-2">{t('dashboard.plantsInTrays')}</h2>
          <p className="text-error text-sm">{trayError instanceof Error ? trayError.message : String(trayError)}</p>
        </section>
      )}

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

      {harvestsError && (
        <section>
          <h2 className="font-bold text-lg mb-2">{t('dashboard.harvestStats')}</h2>
          <p className="text-error text-sm">{harvestsError instanceof Error ? harvestsError.message : String(harvestsError)}</p>
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
