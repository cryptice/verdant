import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Link, useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { api } from '../api/client'
import { PageHeader } from '../components/PageHeader'
import { ErrorDisplay } from '../components/ErrorDisplay'
import { Dialog } from '../components/Dialog'
import { useOnboarding } from '../onboarding/OnboardingContext'

const GARDEN_ICONS = [
  '🌱', '🌿', '🌾', '🌻', '🌸', '🌺', '🌼', '🍀',
  '🌲', '🌳', '🌴', '🎋', '🪴', '🍃', '🍂', '🍁',
  '🥬', '🥦', '🧅', '🧄', '🍅', '🫑', '🥕', '🌽',
  '🍓', '🫐', '🍇', '🍎', '🍋', '🍊', '🫒', '🌰',
]

export function GardenList() {
  const qc = useQueryClient()
  const navigate = useNavigate()
  const { t } = useTranslation()
  const { isActive, isStepComplete, completeStep } = useOnboarding()

  const { data: gardens, error, isLoading, refetch } = useQuery({
    queryKey: ['gardens'],
    queryFn: () => api.gardens.list(),
  })

  const { data: beds } = useQuery({
    queryKey: ['beds'],
    queryFn: () => api.beds.list(),
    enabled: (gardens?.length ?? 0) > 0,
  })

  const [showNewGarden, setShowNewGarden] = useState(false)
  const [gardenName, setGardenName] = useState('')
  const [gardenDescription, setGardenDescription] = useState('')
  const [gardenEmoji, setGardenEmoji] = useState('')

  const resetGardenForm = () => { setGardenName(''); setGardenDescription(''); setGardenEmoji('') }

  const gardenMutation = useMutation({
    mutationFn: () => api.gardens.create({ name: gardenName, description: gardenDescription || undefined, emoji: gardenEmoji || undefined }),
    onSuccess: (g) => {
      qc.invalidateQueries({ queryKey: ['gardens'] })
      qc.invalidateQueries({ queryKey: ['dashboard'] })
      setShowNewGarden(false)
      resetGardenForm()
      completeStep('create_garden')
      navigate(`/garden/${g.id}`)
    },
  })

  const openNewGarden = () => {
    resetGardenForm()
    const gardenCount = gardens?.length ?? 0
    const defaultIcon = GARDEN_ICONS[gardenCount % GARDEN_ICONS.length]
    setGardenEmoji(defaultIcon)
    setShowNewGarden(true)
  }

  const isGardenStepIncomplete = isActive && !isStepComplete('create_garden')
  const isBedStepIncomplete = isActive && !isStepComplete('create_bed')

  if (isLoading) {
    return (
      <div className="flex items-center justify-center p-16">
        <div className="animate-spin h-8 w-8 border-2 border-accent border-t-transparent rounded-full" />
      </div>
    )
  }

  if (error) return <ErrorDisplay error={error} onRetry={refetch} />

  return (
    <div>
      <PageHeader title={t('nav.gardens')} action={{ label: t('dashboard.newGarden'), onClick: openNewGarden }} />

      {gardens && gardens.length > 0 && isBedStepIncomplete && beds && beds.length === 0 && (
        <div className="px-4 pt-4">
          <div className="bg-accent-light/50 border border-accent/15 rounded-2xl px-6 py-6 text-center">
            <div className="w-10 h-10 rounded-full bg-accent/10 flex items-center justify-center mx-auto mb-3">
              <span className="text-xl">🌾</span>
            </div>
            <p className="font-semibold text-text-primary">{t('onboarding.steps.create_bed')}</p>
            <p className="text-sm text-text-secondary mt-1 max-w-md mx-auto">{t('onboarding.hints.create_bed')}</p>
            <button onClick={() => navigate(`/garden/${gardens[0].id}`)} className="btn-primary mt-4">
              {t('garden.addBed')}
            </button>
          </div>
        </div>
      )}

      <div className="px-4 py-4 space-y-4">
        {gardens && gardens.length === 0 && (
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

        {gardens?.map((g, i) => (
          <Link key={g.id} to={`/garden/${g.id}`} data-onboarding={i === 0 ? 'garden-card' : undefined} className="card flex items-center gap-4 no-underline text-inherit group">
            <span className="text-4xl group-hover:scale-110 transition-transform duration-200">{g.emoji ?? '🌱'}</span>
            <div className="flex-1 min-w-0">
              <p className="font-semibold text-lg">{g.name}</p>
            </div>
          </Link>
        ))}

      </div>

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
            <input value={gardenName} onChange={e => setGardenName(e.target.value)} placeholder={t('garden.gardenNamePlaceholder')} className="input w-full" />
          </div>
          <div>
            <label className="field-label">{t('common.descriptionLabel')}</label>
            <textarea value={gardenDescription} onChange={e => setGardenDescription(e.target.value)} placeholder={t('common.optional')} rows={3} className="input w-full" />
          </div>
          {gardenMutation.error && <p className="text-error text-sm">{gardenMutation.error instanceof Error ? gardenMutation.error.message : String(gardenMutation.error)}</p>}
        </div>
      </Dialog>
    </div>
  )
}
