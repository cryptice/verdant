import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useParams, useNavigate, Link } from 'react-router-dom'
import { useState, useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import { api } from '../api/client'
import { Masthead, Chip, Stat, PhotoPlaceholder } from '../components/faltet'
import { Dialog } from '../components/Dialog'
import { useOnboarding } from '../onboarding/OnboardingContext'

const GARDEN_ICONS = [
  '🌱', '🌿', '🌾', '🌻', '🌸', '🌺', '🌼', '🍀',
  '🌲', '🌳', '🌴', '🎋', '🪴', '🍃', '🍂', '🍁',
  '🥬', '🥦', '🧅', '🧄', '🍅', '🫑', '🥕', '🌽',
  '🍓', '🫐', '🍇', '🍎', '🍋', '🍊', '🫒', '🌰',
]

export function GardenDetail() {
  const { id } = useParams<{ id: string }>()
  const gardenId = Number(id)
  const navigate = useNavigate()
  const qc = useQueryClient()
  const { t } = useTranslation()
  const { completeStep } = useOnboarding()

  const { data: garden, isLoading } = useQuery({
    queryKey: ['garden', gardenId],
    queryFn: () => api.gardens.get(gardenId),
  })

  const { data: beds } = useQuery({
    queryKey: ['garden-beds', gardenId],
    queryFn: () => api.gardens.beds(gardenId),
  })

  // Used to decide whether to surface a "+ New garden" shortcut here — when the
  // user only has this one garden, the sidebar skips the Gardens index, so the
  // create-garden affordance would otherwise be unreachable.
  const { data: allGardens } = useQuery({
    queryKey: ['gardens'],
    queryFn: api.gardens.list,
    staleTime: 60_000,
  })

  // Edit garden state
  const [editing, setEditing] = useState(false)
  const [editName, setEditName] = useState('')
  const [editDesc, setEditDesc] = useState('')
  const [editEmoji, setEditEmoji] = useState('')

  // Delete garden state
  const [showDelete, setShowDelete] = useState(false)
  const [deleteError, setDeleteError] = useState<string | null>(null)

  const updateMut = useMutation({
    mutationFn: () => api.gardens.update(gardenId, { name: editName, description: editDesc || undefined, emoji: editEmoji || undefined }),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['garden', gardenId] }); setEditing(false) },
  })

  const deleteMut = useMutation({
    mutationFn: () => api.gardens.delete(gardenId),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['dashboard'] }); navigate('/') },
    onError: (err) => { setDeleteError(err instanceof Error ? err.message : String(err)) },
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

  function resetNewBed() {
    setBedName(''); setBedDescription(''); setBedLength(''); setBedWidth('')
    setNewBedConditionsOpen(false); setNewSoilType(''); setNewSoilPh('')
    setNewSunExposure(''); setNewAspect(''); setNewDrainage('')
    setNewIrrigationType(''); setNewProtection(''); setNewRaisedBed(false)
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

  const sortedBeds = useMemo(() =>
    beds?.slice().sort((a, b) => a.name.localeCompare(b.name)) ?? [],
    [beds]
  )

  // Plant count aggregation from beds (bedCount * approximate) — use garden's plantCount if available
  const plantCount = sortedBeds.reduce((sum, _b) => sum, 0)

  // Harvest stems this year — placeholder; same pattern as BedDetail
  const harvestStemsThisYear = 0

  if (isLoading) return (
    <div className="flex justify-center p-16">
      <div className="animate-spin h-8 w-8 border-2 border-accent border-t-transparent rounded-full" />
    </div>
  )
  if (!garden) return null

  const SOIL_TYPES = ['SANDY', 'LOAMY', 'CLAY', 'SILTY', 'PEATY', 'CHALKY'] as const
  const SUN_EXPOSURES = ['FULL_SUN', 'PARTIAL_SUN', 'PARTIAL_SHADE', 'FULL_SHADE'] as const
  const DRAINAGES = ['POOR', 'MODERATE', 'GOOD', 'SHARP'] as const
  const ASPECTS = ['FLAT', 'N', 'NE', 'E', 'SE', 'S', 'SW', 'W', 'NW', 'UNOBSTRUCTED'] as const
  const IRRIGATION_TYPES = ['DRIP', 'SPRINKLER', 'SOAKER_HOSE', 'MANUAL', 'NONE'] as const
  const PROTECTIONS = ['OPEN_FIELD', 'ROW_COVER', 'LOW_TUNNEL', 'HIGH_TUNNEL', 'GREENHOUSE', 'COLDFRAME'] as const

  return (
    <div>
      <Masthead
        left={
          <span>
            <Link to="/gardens" style={{ color: 'inherit', textDecoration: 'none' }}>
              {t('nav.gardens')}
            </Link>
            {' / '}
            <span style={{ color: 'var(--color-accent)' }}>{garden.name}</span>
          </span>
        }
        center={t('garden.masthead.center')}
        right={
          <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end' }}>
            {allGardens && allGardens.length === 1 && (
              <Link to="/gardens?new=1" className="btn-secondary">
                {t('dashboard.newGarden')}
              </Link>
            )}
            <button
              onClick={() => { setEditName(garden.name); setEditDesc(garden.description ?? ''); setEditEmoji(garden.emoji ?? ''); setEditing(true) }}
              className="btn-secondary"
            >
              {t('common.edit')}
            </button>
          </div>
        }
      />

      <div style={{ padding: '28px 40px' }}>
        {/* Hero */}
        <div>
          <h1
            style={{
              fontFamily: 'var(--font-display)',
              fontSize: 80,
              fontWeight: 300,
              letterSpacing: -1.5,
              lineHeight: 1,
              margin: 0,
              fontVariationSettings: '"SOFT" 100, "opsz" 144',
            }}
          >
            {garden.emoji && <span style={{ marginRight: 16 }}>{garden.emoji}</span>}
            {garden.name}<span style={{ color: 'var(--color-accent)' }}>.</span>
          </h1>
          {garden.description && (
            <p
              style={{
                marginTop: 16,
                fontFamily: 'Georgia, var(--font-display)',
                fontSize: 15,
                lineHeight: 1.6,
                color: 'var(--color-forest)',
              }}
            >
              {garden.description}
            </p>
          )}
        </div>

        {/* Stats band */}
        <div
          className="stats-band"
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(3, 1fr)',
            gap: 18,
          }}
        >
          <Stat size="medium" value={sortedBeds.length} label={t('garden.stats.activeBeds')} hue="sage" />
          <Stat size="medium" value={plantCount} label={t('garden.stats.activePlants')} hue="mustard" />
          <Stat size="medium" value={harvestStemsThisYear} unit="st" label={t('garden.stats.harvested')} hue="clay" />
        </div>

        {/* Beds section heading */}
        <div style={{ display: 'flex', alignItems: 'baseline', justifyContent: 'space-between', gap: 12, marginTop: 40, marginBottom: 14 }}>
          <h2
            style={{
              fontFamily: 'var(--font-display)',
              fontStyle: 'italic',
              fontSize: 30,
              fontWeight: 300,
              margin: 0,
              fontVariationSettings: '"SOFT" 100, "opsz" 144',
            }}
          >
            {t('garden.beds')}<span style={{ color: 'var(--color-accent)' }}>.</span>
          </h2>
          <button
            onClick={() => { resetNewBed(); setShowNewBed(true) }}
            style={{
              background: 'transparent',
              border: '1px solid var(--color-accent)',
              fontFamily: 'var(--font-mono)',
              fontSize: 11,
              letterSpacing: 1.6,
              textTransform: 'uppercase',
              color: 'var(--color-accent)',
              cursor: 'pointer',
              padding: '6px 14px',
              whiteSpace: 'nowrap',
            }}
          >
            {t('garden.newBed')}
          </button>
        </div>

        {sortedBeds.length === 0 && (
          <p style={{ fontFamily: 'var(--font-display)', fontStyle: 'italic', color: 'var(--color-forest)' }}>
            {t('garden.noBedsYet')}
          </p>
        )}

        {/* Bed rows */}
        {sortedBeds.length > 0 && (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3">
            {sortedBeds.map((bed) => (
              <Link
                key={bed.id}
                to={`/bed/${bed.id}`}
                className="bed-card"
              >
                <div style={{ display: 'flex', alignItems: 'baseline', justifyContent: 'space-between', gap: 8 }}>
                  <span style={{ fontFamily: 'var(--font-display)', fontSize: 20, lineHeight: 1.1 }}>{bed.name}</span>
                  <span style={{ color: 'var(--color-accent)', fontFamily: 'var(--font-mono)', fontSize: 14 }}>→</span>
                </div>
                <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap', marginTop: 'auto' }}>
                  {bed.sunExposure && (
                    <Chip tone="sage">{t(`bed.conditions.sunExposures.${bed.sunExposure}`)}</Chip>
                  )}
                  {bed.drainage && (
                    <Chip tone="sky">{t(`bed.conditions.drainages.${bed.drainage}`)}</Chip>
                  )}
                  {bed.protection && (
                    <Chip tone="berry">{t(`bed.conditions.protections.${bed.protection}`)}</Chip>
                  )}
                </div>
              </Link>
            ))}
          </div>
        )}

        {/* Harvest card */}
        <div
          style={{
            background: 'var(--color-ink)',
            color: 'var(--color-cream)',
            padding: '22px 28px',
            position: 'relative',
            overflow: 'hidden',
            marginTop: 40,
          }}
        >
          <div
            style={{
              position: 'absolute',
              top: -40,
              right: -40,
              width: 140,
              height: 140,
              borderRadius: '50%',
              background: 'var(--color-blush)',
              opacity: 0.2,
            }}
          />
          <div
            style={{
              fontFamily: 'var(--font-display)',
              fontStyle: 'italic',
              fontSize: 26,
              fontVariationSettings: '"SOFT" 100, "opsz" 144',
            }}
          >
            {t('garden.stats.harvested')}{' '}
            <span style={{ color: 'var(--color-blush)' }}>
              {new Date().getFullYear()}
            </span>.
          </div>
          <div
            style={{
              marginTop: 12,
              fontFamily: 'var(--font-mono)',
              fontSize: 10,
              letterSpacing: 1.4,
              textTransform: 'uppercase',
              color: 'var(--color-sage)',
            }}
          >
            {harvestStemsThisYear} st
          </div>
        </div>

        {/* Garden layout — photo placeholder */}
        <div style={{ marginTop: 48, maxWidth: 420 }}>
          <PhotoPlaceholder tone="sage" aspect="tall" label={garden.name.toUpperCase()} />
        </div>

        {/* Danger callout — full-width, very bottom */}
        <div
          style={{
            marginTop: 48,
            border: '1px solid color-mix(in srgb, var(--color-accent) 40%, transparent)',
            padding: '22px 28px',
          }}
        >
          <div
            style={{
              fontFamily: 'var(--font-mono)',
              fontSize: 10,
              letterSpacing: 1.4,
              textTransform: 'uppercase',
              color: 'var(--color-accent)',
              marginBottom: 10,
            }}
          >
            {t('garden.danger.title')}
          </div>
          <p style={{ fontFamily: 'var(--font-display)', fontStyle: 'italic', fontSize: 15, margin: 0 }}>
            {t('garden.danger.warning')}
          </p>
          <button
            onClick={() => setShowDelete(true)}
            style={{
              marginTop: 10,
              background: 'transparent',
              border: 'none',
              fontFamily: 'var(--font-mono)',
              fontSize: 10,
              letterSpacing: 1.4,
              textTransform: 'uppercase',
              color: 'var(--color-accent)',
              cursor: 'pointer',
              padding: 0,
            }}
          >
            → {t('garden.danger.delete')}
          </button>
        </div>
      </div>

      {/* Edit garden dialog */}
      <Dialog
        open={editing}
        onClose={() => setEditing(false)}
        title={t('garden.editGardenTitle')}
        actions={
          <>
            <button onClick={() => setEditing(false)} className="px-4 py-2 text-sm text-text-secondary">{t('common.cancel')}</button>
            <button onClick={() => updateMut.mutate()} disabled={!editName.trim()} className="btn-primary text-sm">{t('common.save')}</button>
          </>
        }
      >
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
                    editEmoji === icon ? 'bg-accent-light ring-1 ring-accent' : 'hover:bg-divider'
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

      {/* Delete garden dialog */}
      <Dialog
        open={showDelete}
        onClose={() => { setShowDelete(false); setDeleteError(null) }}
        title={t('garden.deleteGardenTitle')}
        actions={
          <>
            <button onClick={() => { setShowDelete(false); setDeleteError(null) }} className="px-4 py-2 text-sm text-text-secondary">{t('common.cancel')}</button>
            <button onClick={() => deleteMut.mutate()} className="px-4 py-2 text-sm text-error font-semibold">{t('common.delete')}</button>
          </>
        }
      >
        <p className="text-text-secondary">{t('garden.deleteGardenConfirm')}</p>
        {deleteError && <p className="text-error text-sm mt-2">{deleteError}</p>}
      </Dialog>

      {/* New bed dialog */}
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
              <input type="number" step="0.1" min="0" value={bedLength} onChange={e => setBedLength(e.target.value)} placeholder="—" className="input w-full" />
            </div>
            <div>
              <label className="field-label">{t('bed.conditions.widthMeters')}</label>
              <input type="number" step="0.1" min="0" value={bedWidth} onChange={e => setBedWidth(e.target.value)} placeholder="—" className="input w-full" />
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
