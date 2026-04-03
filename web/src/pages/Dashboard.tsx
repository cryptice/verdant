import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Link, useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { api } from '../api/client'
import { ErrorDisplay } from '../components/ErrorDisplay'
import { StatusBadge } from '../components/StatusBadge'
import { Dialog } from '../components/Dialog'
import { useAuth } from '../auth/AuthContext'
import { useOnboarding } from '../onboarding/OnboardingContext'

const GARDEN_ICONS = [
  '🌱', '🌿', '🌾', '🌻', '🌸', '🌺', '🌼', '🍀',
  '🌲', '🌳', '🌴', '🎋', '🪴', '🍃', '🍂', '🍁',
  '🥬', '🥦', '🧅', '🧄', '🍅', '🫑', '🥕', '🌽',
  '🍓', '🫐', '🍇', '🍎', '🍋', '🍊', '🫒', '🌰',
]

function formatWeight(grams: number) {
  return grams >= 1000 ? `${(grams / 1000).toFixed(1)} kg` : `${grams} g`
}

export function Dashboard() {
  const { user } = useAuth()
  const navigate = useNavigate()
  const qc = useQueryClient()
  const { t } = useTranslation()
  const { isActive, completedCount, totalCount, setDrawerOpen, isStepComplete } = useOnboarding()

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

  // Garden creation dialog
  const [showNewGarden, setShowNewGarden] = useState(false)
  const [gardenName, setGardenName] = useState('')
  const [gardenDescription, setGardenDescription] = useState('')
  const [gardenEmoji, setGardenEmoji] = useState('')

  const resetGardenForm = () => { setGardenName(''); setGardenDescription(''); setGardenEmoji('') }

  const gardenMutation = useMutation({
    mutationFn: () => api.gardens.create({ name: gardenName, description: gardenDescription || undefined, emoji: gardenEmoji || undefined }),
    onSuccess: (g) => {
      qc.invalidateQueries({ queryKey: ['dashboard'] })
      setShowNewGarden(false)
      resetGardenForm()
      navigate(`/garden/${g.id}`)
    },
  })

  const openNewGarden = () => { resetGardenForm(); setShowNewGarden(true) }

  const isGardenStepIncomplete = isActive && !isStepComplete('create_garden')

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
        <button onClick={openNewGarden} className="btn-primary shrink-0">{t('dashboard.newGarden')}</button>
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

      {dashboard && dashboard.gardens.length === 0 && (
        isGardenStepIncomplete ? (
          <div className="bg-accent-light/50 border border-accent/15 rounded-2xl px-6 py-6 text-center">
            <div className="w-10 h-10 rounded-full bg-accent/10 flex items-center justify-center mx-auto mb-3">
              <span className="text-xl">🏡</span>
            </div>
            <p className="font-semibold text-text-primary">{t('onboarding.steps.create_garden')}</p>
            <p className="text-sm text-text-secondary mt-1 max-w-md mx-auto">{t('onboarding.hints.create_garden')}</p>
            <button onClick={openNewGarden} className="btn-primary mt-4">
              {t('dashboard.newGarden')}
            </button>
          </div>
        ) : (
          <div className="card text-center py-8">
            <p className="text-4xl mb-2">🌱</p>
            <p className="font-medium">{t('dashboard.noGardens')}</p>
            <p className="text-text-secondary text-sm">{t('dashboard.noGardensHint')}</p>
          </div>
        )
      )}

      {dashboard?.gardens.map((g, i) => (
        <Link key={g.id} to={`/garden/${g.id}`} data-onboarding={i === 0 ? "garden-card" : undefined} className="card flex items-center gap-4 no-underline text-inherit group">
          <span className="text-4xl group-hover:scale-110 transition-transform duration-200">{g.emoji ?? '🌱'}</span>
          <div className="flex-1 min-w-0">
            <p className="font-semibold text-lg">{g.name}</p>
            <p className="text-sm text-text-secondary">
              {t('dashboard.plants', { count: g.plantCount })} · {t('dashboard.beds', { count: g.bedCount })}
            </p>
          </div>
        </Link>
      ))}

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

      <Dialog open={showNewGarden} onClose={() => { setShowNewGarden(false); resetGardenForm() }} title={t('garden.newGardenTitle')} actions={
        <>
          <button onClick={() => { setShowNewGarden(false); resetGardenForm() }} className="px-4 py-2 text-sm text-text-secondary">{t('common.cancel')}</button>
          <button
            onClick={() => gardenMutation.mutate()}
            disabled={!gardenName.trim() || gardenMutation.isPending}
            className="btn-primary text-sm"
          >
            {gardenMutation.isPending ? t('garden.creatingGarden') : t('garden.createGarden')}
          </button>
        </>
      }>
        <div data-onboarding="garden-form" className="space-y-4">
          <div>
            <label className="field-label">{t('common.iconLabel')}</label>
            <div className="grid grid-cols-8 gap-1 p-2 bg-surface rounded-md border border-divider">
              {GARDEN_ICONS.map(icon => (
                <button
                  key={icon}
                  type="button"
                  onClick={() => setGardenEmoji(gardenEmoji === icon ? '' : icon)}
                  className={`text-xl p-1.5 rounded-md transition-colors leading-none ${
                    gardenEmoji === icon
                      ? 'bg-accent-light ring-1 ring-accent'
                      : 'hover:bg-divider'
                  }`}
                >
                  {icon}
                </button>
              ))}
            </div>
          </div>
          <div>
            <label className="field-label">{t('common.nameLabel')}</label>
            <input value={gardenName} onChange={e => setGardenName(e.target.value)} placeholder={t('garden.gardenNamePlaceholder')} className="input" />
          </div>
          <div>
            <label className="field-label">{t('common.descriptionLabel')}</label>
            <textarea value={gardenDescription} onChange={e => setGardenDescription(e.target.value)} placeholder={t('common.optional')} rows={3} className="input" />
          </div>
          {gardenMutation.error && <p className="text-error text-sm">{gardenMutation.error instanceof Error ? gardenMutation.error.message : String(gardenMutation.error)}</p>}
        </div>
      </Dialog>
    </div>
  )
}
