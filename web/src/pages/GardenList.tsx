import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { api } from '../api/client'
import { Masthead, Ledger } from '../components/faltet'
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

  const { data: gardens = [], error, isLoading, refetch } = useQuery({
    queryKey: ['gardens'],
    queryFn: () => api.gardens.list(),
  })

  const { data: beds = [] } = useQuery({
    queryKey: ['beds'],
    queryFn: () => api.beds.list(),
    enabled: gardens.length > 0,
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
    const gardenCount = gardens.length
    const defaultIcon = GARDEN_ICONS[gardenCount % GARDEN_ICONS.length]
    setGardenEmoji(defaultIcon)
    setShowNewGarden(true)
  }

  // Compute bed count per garden from the beds list
  const bedCountByGarden = new Map<number, number>()
  for (const bed of beds) {
    bedCountByGarden.set(bed.gardenId, (bedCountByGarden.get(bed.gardenId) ?? 0) + 1)
  }

  if (isLoading) {
    return (
      <div className="flex items-center justify-center p-16">
        <div className="animate-spin h-8 w-8 border-2 border-accent border-t-transparent rounded-full" />
      </div>
    )
  }

  if (error) {
    return (
      <div className="p-8 text-center text-sm text-error">
        {t('error.generic')}
        <button onClick={() => refetch()} className="ml-2 underline">{t('error.retry')}</button>
      </div>
    )
  }

  const isGardenStepIncomplete = isActive && !isStepComplete('create_garden')
  const isBedStepIncomplete = isActive && !isStepComplete('create_bed')

  return (
    <div>
      <Masthead
        left={t('nav.gardens')}
        center="— Trädgårdsliggaren —"
        right={
          <button onClick={openNewGarden} className="btn-primary">
            {t('dashboard.newGarden')}
          </button>
        }
      />

      <div style={{ padding: '28px 40px' }}>
        {gardens.length > 0 && isBedStepIncomplete && beds.length === 0 && (
          <div className="bg-accent-light/50 border border-accent/15 rounded-2xl px-6 py-6 text-center mb-6">
            <div className="w-10 h-10 rounded-full bg-accent/10 flex items-center justify-center mx-auto mb-3">
              <span className="text-xl">🌾</span>
            </div>
            <p className="font-semibold text-text-primary">{t('onboarding.steps.create_bed')}</p>
            <p className="text-sm text-text-secondary mt-1 max-w-md mx-auto">{t('onboarding.hints.create_bed')}</p>
            <button onClick={() => navigate(`/garden/${gardens[0].id}`)} className="btn-primary mt-4">
              {t('garden.addBed')}
            </button>
          </div>
        )}

        {gardens.length === 0 && (
          isGardenStepIncomplete ? (
            <div className="bg-accent-light/50 border border-accent/15 rounded-2xl px-6 py-6 text-center mb-6">
              <div className="w-10 h-10 rounded-full bg-accent/10 flex items-center justify-center mx-auto mb-3">
                <span className="text-xl">🏡</span>
              </div>
              <p className="font-semibold text-text-primary">{t('onboarding.steps.create_garden')}</p>
              <p className="text-sm text-text-secondary mt-1 max-w-md mx-auto">{t('onboarding.hints.create_garden')}</p>
              <button onClick={openNewGarden} className="btn-primary mt-4">
                {t('dashboard.newGarden')}
              </button>
            </div>
          ) : null
        )}

        <Ledger
          columns={[
            {
              key: 'id',
              label: '№',
              width: '60px',
              render: (_g, i) => (
                <span style={{ fontFamily: 'var(--font-display)', fontStyle: 'italic', fontSize: 22, color: 'var(--color-sage)' }}>
                  {String(i + 1).padStart(2, '0')}
                </span>
              ),
            },
            {
              key: 'name',
              label: t('common.nameLabel'),
              width: '1.5fr',
              render: (g) => (
                <span style={{ fontFamily: 'var(--font-display)', fontSize: 20 }}>
                  {g.emoji ? `${g.emoji} ${g.name}` : g.name}
                </span>
              ),
            },
            {
              key: 'bedCount',
              label: t('garden.beds'),
              width: '120px',
              align: 'right',
              render: (g) => (
                <span style={{ fontVariantNumeric: 'tabular-nums' }}>
                  {bedCountByGarden.get(g.id) ?? '—'}
                </span>
              ),
            },
            {
              key: 'goto',
              label: '',
              width: '40px',
              align: 'right',
              render: () => (
                <span style={{ color: 'var(--color-accent)', fontFamily: 'var(--font-mono)' }}>→</span>
              ),
            },
          ]}
          rows={gardens}
          rowKey={(g) => g.id}
          onRowClick={(g) => navigate(`/garden/${g.id}`)}
        />
      </div>

      <Dialog
        open={showNewGarden}
        onClose={() => { setShowNewGarden(false); resetGardenForm() }}
        title={t('garden.newGardenTitle')}
        actions={
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
        }
      >
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
