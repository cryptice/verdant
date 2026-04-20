import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useParams, useNavigate, Link } from 'react-router-dom'
import { useState, useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import type { BreadcrumbItem } from '../components/Breadcrumb'
import { api } from '../api/client'
import { PageHeader } from '../components/PageHeader'
import { OnboardingHint } from '../onboarding/OnboardingHint'
import { ErrorDisplay } from '../components/ErrorDisplay'
import { Dialog } from '../components/Dialog'
import { useOnboarding } from '../onboarding/OnboardingContext'

const GARDEN_ICONS = [
  '🌱', '🌿', '🌾', '🌻', '🌸', '🌺', '🌼', '🍀',
  '🌲', '🌳', '🌴', '🎋', '🪴', '🍃', '🍂', '🍁',
  '🥬', '🥦', '🧅', '🧄', '🍅', '🫑', '🥕', '🌽',
  '🍓', '🫐', '🍇', '🍎', '🍋', '🍊', '🫒', '🌰',
]

const SOIL_TYPES = ['SANDY', 'LOAMY', 'CLAY', 'SILTY', 'PEATY', 'CHALKY'] as const
const SUN_EXPOSURES = ['FULL_SUN', 'PARTIAL_SUN', 'PARTIAL_SHADE', 'FULL_SHADE'] as const
const DRAINAGES = ['POOR', 'MODERATE', 'GOOD', 'SHARP'] as const
const ASPECTS = ['FLAT', 'N', 'NE', 'E', 'SE', 'S', 'SW', 'W', 'NW'] as const
const IRRIGATION_TYPES = ['DRIP', 'SPRINKLER', 'SOAKER_HOSE', 'MANUAL', 'NONE'] as const
const PROTECTIONS = ['OPEN_FIELD', 'ROW_COVER', 'LOW_TUNNEL', 'HIGH_TUNNEL', 'GREENHOUSE', 'COLDFRAME'] as const

export function GardenDetail() {
  const { id } = useParams<{ id: string }>()
  const gardenId = Number(id)
  const navigate = useNavigate()
  const qc = useQueryClient()
  const { t } = useTranslation()
  const { completeStep } = useOnboarding()

  const { data: garden, error, isLoading, refetch } = useQuery({
    queryKey: ['garden', gardenId],
    queryFn: () => api.gardens.get(gardenId),
  })

  const { data: beds } = useQuery({
    queryKey: ['garden-beds', gardenId],
    queryFn: () => api.gardens.beds(gardenId),
  })

  // New bed dialog state
  const [showNewBed, setShowNewBed] = useState(false)
  const [bedName, setBedName] = useState('')
  const [bedDescription, setBedDescription] = useState('')
  const [bedLength, setBedLength] = useState('')
  const [bedWidth, setBedWidth] = useState('')
  const [newBedConditionsOpen, setNewBedConditionsOpen] = useState(false)
  const [newSoilType, setNewSoilType] = useState('')
  const [newSoilPh, setNewSoilPh] = useState('')
  const [newSunExposure, setNewSunExposure] = useState('')
  const [newAspect, setNewAspect] = useState('')
  const [newDrainage, setNewDrainage] = useState('')
  const [newIrrigationType, setNewIrrigationType] = useState('')
  const [newProtection, setNewProtection] = useState('')
  const [newRaisedBed, setNewRaisedBed] = useState(false)

  const newPhNum = newSoilPh !== '' ? parseFloat(newSoilPh) : undefined
  const newPhOutOfRange = newPhNum !== undefined && (newPhNum < 3.0 || newPhNum > 9.0)

  // Garden edit state
  const [editing, setEditing] = useState(false)
  const [editName, setEditName] = useState('')
  const [editDesc, setEditDesc] = useState('')
  const [editEmoji, setEditEmoji] = useState('')
  const [showDelete, setShowDelete] = useState(false)

  // Filter state
  const [filterSun, setFilterSun] = useState('')
  const [filterDrainage, setFilterDrainage] = useState('')
  const [filterProtection, setFilterProtection] = useState('')

  const anyFilterSet = !!(filterSun || filterDrainage || filterProtection)

  function resetNewBed() {
    setBedName('')
    setBedDescription('')
    setBedLength('')
    setBedWidth('')
    setNewBedConditionsOpen(false)
    setNewSoilType('')
    setNewSoilPh('')
    setNewSunExposure('')
    setNewAspect('')
    setNewDrainage('')
    setNewIrrigationType('')
    setNewProtection('')
    setNewRaisedBed(false)
  }

  const createBedMut = useMutation({
    mutationFn: () => api.beds.create(gardenId, {
      name: bedName,
      description: bedDescription || undefined,
      lengthMeters: bedLength !== '' ? parseFloat(bedLength) : undefined,
      widthMeters: bedWidth !== '' ? parseFloat(bedWidth) : undefined,
      soilType: newSoilType || undefined,
      soilPh: newPhNum,
      sunExposure: newSunExposure || undefined,
      drainage: newDrainage || undefined,
      aspect: newAspect || undefined,
      irrigationType: newIrrigationType || undefined,
      protection: newProtection || undefined,
      raisedBed: newRaisedBed,
    }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['garden-beds', gardenId] })
      setShowNewBed(false)
      resetNewBed()
      completeStep('create_bed')
    },
  })

  const updateMut = useMutation({
    mutationFn: () => api.gardens.update(gardenId, { name: editName, description: editDesc || undefined, emoji: editEmoji || undefined }),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['garden', gardenId] }); setEditing(false) },
  })

  const [deleteError, setDeleteError] = useState<string | null>(null)

  const deleteMut = useMutation({
    mutationFn: () => api.gardens.delete(gardenId),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['dashboard'] }); navigate('/') },
    onError: (err) => { setDeleteError(err instanceof Error ? err.message : String(err)) },
  })

  const sortedBeds = useMemo(() =>
    beds?.slice().sort((a, b) => a.name.localeCompare(b.name)) ?? [],
    [beds]
  )

  const filteredBeds = useMemo(() => {
    return sortedBeds.filter(bed => {
      if (filterSun && bed.sunExposure !== filterSun) return false
      if (filterDrainage && bed.drainage !== filterDrainage) return false
      if (filterProtection && bed.protection !== filterProtection) return false
      return true
    })
  }, [sortedBeds, filterSun, filterDrainage, filterProtection])

  if (isLoading) return <div className="flex justify-center p-16"><div className="animate-spin h-8 w-8 border-2 border-accent border-t-transparent rounded-full" /></div>
  if (error) return <ErrorDisplay error={error} onRetry={refetch} />
  if (!garden) return null

  const breadcrumbs: BreadcrumbItem[] = [{ label: t('nav.myWorld'), to: '/' }]

  return (
    <div className="flex flex-col flex-1">
      <PageHeader
        title={garden.name}
        icon={garden.emoji}
        breadcrumbs={breadcrumbs}
        editAction={() => { setEditName(garden.name); setEditDesc(garden.description ?? ''); setEditEmoji(garden.emoji ?? ''); setEditing(true) }}
      />
      <OnboardingHint />

      <div className="px-4 py-4 space-y-4">
        {garden.description && <p className="text-text-secondary">{garden.description}</p>}

        <div className="flex items-center justify-between">
          <h2 className="font-semibold">{t('garden.beds')}</h2>
          <button data-onboarding="add-bed-btn" onClick={() => setShowNewBed(true)} className="btn-primary text-sm">{t('garden.newBed')}</button>
        </div>

        {beds && beds.length > 0 && (
          <div className="flex flex-wrap items-center gap-2">
            <select
              value={filterSun}
              onChange={e => setFilterSun(e.target.value)}
              className="text-sm border border-divider rounded-full px-3 py-1 bg-surface"
            >
              <option value="">☀️ {t('bed.conditions.sunExposure')}</option>
              {SUN_EXPOSURES.map(v => <option key={v} value={v}>{t(`bed.conditions.sunExposures.${v}`)}</option>)}
            </select>
            <select
              value={filterDrainage}
              onChange={e => setFilterDrainage(e.target.value)}
              className="text-sm border border-divider rounded-full px-3 py-1 bg-surface"
            >
              <option value="">💧 {t('bed.conditions.drainage')}</option>
              {DRAINAGES.map(v => <option key={v} value={v}>{t(`bed.conditions.drainages.${v}`)}</option>)}
            </select>
            <select
              value={filterProtection}
              onChange={e => setFilterProtection(e.target.value)}
              className="text-sm border border-divider rounded-full px-3 py-1 bg-surface"
            >
              <option value="">🏠 {t('bed.conditions.protection')}</option>
              {PROTECTIONS.map(v => <option key={v} value={v}>{t(`bed.conditions.protections.${v}`)}</option>)}
            </select>
            {anyFilterSet && (
              <button
                onClick={() => { setFilterSun(''); setFilterDrainage(''); setFilterProtection('') }}
                className="text-sm text-accent hover:underline"
              >
                {t('bed.conditions.filterClear')}
              </button>
            )}
          </div>
        )}

        {beds && beds.length === 0 && (
          <p className="text-text-secondary text-sm">{t('garden.noBedsYet')}</p>
        )}

        {filteredBeds.map(bed => {
          const hasChips = !!(bed.sunExposure || bed.drainage || bed.protection)
          return (
            <Link key={bed.id} to={`/bed/${bed.id}`} className="card block no-underline text-inherit">
              <p className="font-semibold">{bed.name}</p>
              {bed.description && <p className="text-sm text-text-secondary mt-1">{bed.description}</p>}
              {hasChips && (
                <div className="flex flex-wrap gap-1.5 mt-2">
                  {bed.sunExposure && (
                    <span className="inline-flex items-center gap-1 px-2 py-0.5 text-xs rounded-full bg-surface border border-divider">
                      ☀️ {t(`bed.conditions.sunExposures.${bed.sunExposure}`)}
                    </span>
                  )}
                  {bed.drainage && (
                    <span className="inline-flex items-center gap-1 px-2 py-0.5 text-xs rounded-full bg-surface border border-divider">
                      💧 {t(`bed.conditions.drainages.${bed.drainage}`)}
                    </span>
                  )}
                  {bed.protection && (
                    <span className="inline-flex items-center gap-1 px-2 py-0.5 text-xs rounded-full bg-surface border border-divider">
                      🏠 {t(`bed.conditions.protections.${bed.protection}`)}
                    </span>
                  )}
                </div>
              )}
            </Link>
          )
        })}

        {beds && beds.length > 0 && filteredBeds.length === 0 && anyFilterSet && (
          <p className="text-text-secondary text-sm">{t('garden.noBedsYet')}</p>
        )}
      </div>

      <div className="mt-auto px-4 pb-4 pt-6">
        <div className="rounded-xl border border-error/20 overflow-hidden">
          <div className="px-4 py-2 bg-error/5 border-b border-error/25">
            <p className="text-xs font-semibold text-error uppercase tracking-wide">{t('common.dangerZone')}</p>
          </div>
          <div className="px-4 py-3 flex items-center justify-between">
            <p className="text-sm text-text-secondary">{t('garden.deleteGardenConfirm')}</p>
            <button onClick={() => setShowDelete(true)} className="ml-4 shrink-0 text-sm font-medium text-error hover:underline">
              {t('garden.deleteGarden')}
            </button>
          </div>
        </div>
      </div>

      <Dialog open={editing} onClose={() => setEditing(false)} title={t('garden.editGardenTitle')} actions={
        <>
          <button onClick={() => setEditing(false)} className="px-4 py-2 text-sm text-text-secondary">{t('common.cancel')}</button>
          <button onClick={() => updateMut.mutate()} disabled={!editName.trim()} className="btn-primary text-sm">{t('common.save')}</button>
        </>
      }>
        <div className="space-y-4">
          <div>
            <label className="field-label">{t('common.iconLabel')}</label>
            <div className="grid grid-cols-8 gap-1 p-2 bg-surface rounded-md border border-divider">
              {GARDEN_ICONS.map(icon => (
                <button
                  key={icon}
                  type="button"
                  onClick={() => setEditEmoji(editEmoji === icon ? '' : icon)}
                  className={`text-xl p-1.5 rounded-md transition-colors leading-none ${
                    editEmoji === icon
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
            <input value={editName} onChange={e => setEditName(e.target.value)} placeholder={t('garden.gardenNamePlaceholder')} className="input" />
          </div>
          <div>
            <label className="field-label">{t('common.descriptionLabel')}</label>
            <textarea value={editDesc} onChange={e => setEditDesc(e.target.value)} placeholder={t('common.optional')} rows={2} className="input" />
          </div>
        </div>
      </Dialog>

      <Dialog open={showDelete} onClose={() => { setShowDelete(false); setDeleteError(null) }} title={t('garden.deleteGardenTitle')} actions={
        <>
          <button onClick={() => { setShowDelete(false); setDeleteError(null) }} className="px-4 py-2 text-sm text-text-secondary">{t('common.cancel')}</button>
          <button onClick={() => deleteMut.mutate()} className="px-4 py-2 text-sm text-error font-semibold">{t('common.delete')}</button>
        </>
      }>
        <p className="text-text-secondary">{t('garden.deleteGardenConfirm')}</p>
        {deleteError && <p className="text-error text-sm mt-2">{deleteError}</p>}
      </Dialog>

      <Dialog
        open={showNewBed}
        onClose={() => { setShowNewBed(false); resetNewBed() }}
        title={t('bed.newBedTitle')}
        actions={
          <>
            <button onClick={() => { setShowNewBed(false); resetNewBed() }} className="px-4 py-2 text-sm text-text-secondary">{t('common.cancel')}</button>
            <button onClick={() => createBedMut.mutate()} disabled={!bedName.trim() || createBedMut.isPending} className="btn-primary text-sm">
              {createBedMut.isPending ? t('bed.creatingBed') : t('bed.createBed')}
            </button>
          </>
        }
      >
        <div className="space-y-4">
          <div>
            <label className="field-label">{t('common.nameLabel')}</label>
            <input value={bedName} onChange={e => setBedName(e.target.value)} placeholder={t('bed.bedNamePlaceholder')} className="input w-full" />
            <p className="text-xs text-text-secondary mt-1">{t('bed.bedNameHint')}</p>
          </div>
          <div>
            <label className="field-label">{t('common.descriptionLabel')}</label>
            <textarea value={bedDescription} onChange={e => setBedDescription(e.target.value)} placeholder={t('common.optional')} rows={2} className="input w-full" />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="field-label">{t('bed.conditions.lengthMeters')}</label>
              <input type="number" step="0.1" min="0" value={bedLength} onChange={e => setBedLength(e.target.value)} placeholder="—" className="input" />
            </div>
            <div>
              <label className="field-label">{t('bed.conditions.widthMeters')}</label>
              <input type="number" step="0.1" min="0" value={bedWidth} onChange={e => setBedWidth(e.target.value)} placeholder="—" className="input" />
            </div>
          </div>

          <div className="border border-divider rounded-lg overflow-hidden">
            <button
              type="button"
              onClick={() => setNewBedConditionsOpen(o => !o)}
              className="w-full flex items-center justify-between px-4 py-3 text-left bg-surface hover:bg-divider transition-colors"
            >
              <span className="text-sm font-medium">{t('bed.conditions.sectionTitle')}</span>
              <span className="text-text-secondary text-sm">{newBedConditionsOpen ? '▲' : '▼'}</span>
            </button>
            {newBedConditionsOpen && (
              <div className="px-4 py-3 space-y-3 border-t border-divider">
                <div className="grid grid-cols-1 gap-3 md:grid-cols-2">
                  <div>
                    <label className="field-label">{t('bed.conditions.soilType')}</label>
                    <select value={newSoilType} onChange={e => setNewSoilType(e.target.value)} className="input">
                      <option value="">{t('common.select')}</option>
                      {SOIL_TYPES.map(v => <option key={v} value={v}>{t(`bed.conditions.soilTypes.${v}`)}</option>)}
                    </select>
                  </div>
                  <div>
                    <label className="field-label">{t('bed.conditions.soilPh')}</label>
                    <input type="number" step="0.1" min="0" max="14" value={newSoilPh} onChange={e => setNewSoilPh(e.target.value)} placeholder="—" className="input" />
                    {newPhOutOfRange && <p className="text-error text-xs mt-1">{t('bed.conditions.phHint')}</p>}
                  </div>
                  <div>
                    <label className="field-label">{t('bed.conditions.sunExposure')}</label>
                    <select value={newSunExposure} onChange={e => setNewSunExposure(e.target.value)} className="input">
                      <option value="">{t('common.select')}</option>
                      {SUN_EXPOSURES.map(v => <option key={v} value={v}>{t(`bed.conditions.sunExposures.${v}`)}</option>)}
                    </select>
                  </div>
                  <div>
                    <label className="field-label">{t('bed.conditions.aspect')}</label>
                    <select value={newAspect} onChange={e => setNewAspect(e.target.value)} className="input">
                      <option value="">{t('common.select')}</option>
                      {ASPECTS.map(v => <option key={v} value={v}>{t(`bed.conditions.aspects.${v}`)}</option>)}
                    </select>
                  </div>
                  <div>
                    <label className="field-label">{t('bed.conditions.drainage')}</label>
                    <select value={newDrainage} onChange={e => setNewDrainage(e.target.value)} className="input">
                      <option value="">{t('common.select')}</option>
                      {DRAINAGES.map(v => <option key={v} value={v}>{t(`bed.conditions.drainages.${v}`)}</option>)}
                    </select>
                  </div>
                  <div>
                    <label className="field-label">{t('bed.conditions.irrigationType')}</label>
                    <select value={newIrrigationType} onChange={e => setNewIrrigationType(e.target.value)} className="input">
                      <option value="">{t('common.select')}</option>
                      {IRRIGATION_TYPES.map(v => <option key={v} value={v}>{t(`bed.conditions.irrigationTypes.${v}`)}</option>)}
                    </select>
                  </div>
                  <div>
                    <label className="field-label">{t('bed.conditions.protection')}</label>
                    <select value={newProtection} onChange={e => setNewProtection(e.target.value)} className="input">
                      <option value="">{t('common.select')}</option>
                      {PROTECTIONS.map(v => <option key={v} value={v}>{t(`bed.conditions.protections.${v}`)}</option>)}
                    </select>
                  </div>
                  <div className="flex items-center gap-2 pt-1">
                    <input
                      id="raisedBed-new"
                      type="checkbox"
                      checked={newRaisedBed}
                      onChange={e => setNewRaisedBed(e.target.checked)}
                      className="h-4 w-4 rounded border-divider accent-accent"
                    />
                    <label htmlFor="raisedBed-new" className="text-sm">{t('bed.conditions.raisedBed')}</label>
                  </div>
                </div>
              </div>
            )}
          </div>
        </div>
      </Dialog>

    </div>
  )
}
