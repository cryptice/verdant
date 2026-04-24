import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useParams, Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { api } from '../api/client'
import { Masthead, Chip, Stat, PhotoPlaceholder } from '../components/faltet'
import { SpeciesEditModal } from '../components/faltet/SpeciesEditModal'
import { Dialog } from '../components/Dialog'

const SOIL_TYPES = ['SANDY', 'LOAMY', 'CLAY', 'SILTY', 'PEATY', 'CHALKY'] as const
const SUN_EXPOSURES = ['FULL_SUN', 'PARTIAL_SUN', 'PARTIAL_SHADE', 'FULL_SHADE'] as const
const DRAINAGES = ['POOR', 'MODERATE', 'GOOD', 'SHARP'] as const
const ASPECTS = ['FLAT', 'N', 'NE', 'E', 'SE', 'S', 'SW', 'W', 'NW', 'UNOBSTRUCTED'] as const
const IRRIGATION_TYPES = ['DRIP', 'SPRINKLER', 'SOAKER_HOSE', 'MANUAL', 'NONE'] as const
const PROTECTIONS = ['OPEN_FIELD', 'ROW_COVER', 'LOW_TUNNEL', 'HIGH_TUNNEL', 'GREENHOUSE', 'COLDFRAME'] as const

export function BedDetail() {
  const { id } = useParams<{ id: string }>()
  const bedId = Number(id)
  const { t } = useTranslation()
  const qc = useQueryClient()

  const { data: bed } = useQuery({ queryKey: ['bed', bedId], queryFn: () => api.beds.get(bedId) })
  const { data: plants } = useQuery({ queryKey: ['bed-plants', bedId], queryFn: () => api.beds.plants(bedId) })
  const { data: garden } = useQuery({
    queryKey: ['garden', bed?.gardenId],
    queryFn: () => api.gardens.get(bed!.gardenId),
    enabled: !!bed,
  })
  const { data: siblings = [] } = useQuery({
    queryKey: ['garden-beds', bed?.gardenId],
    queryFn: () => api.gardens.beds(bed!.gardenId),
    enabled: !!bed,
  })

  // Alphabetical prev/next navigation within the same garden.
  const sortedSiblings = [...siblings].sort((a, b) => a.name.localeCompare(b.name))
  const currentIdx = sortedSiblings.findIndex((s) => s.id === bedId)
  const prevBed = currentIdx > 0 ? sortedSiblings[currentIdx - 1] : null
  const nextBed =
    currentIdx >= 0 && currentIdx < sortedSiblings.length - 1
      ? sortedSiblings[currentIdx + 1]
      : null

  const [modalSpecies, setModalSpecies] = useState<number | null>(null)

  // Edit dialog state — populated lazily when the user opens it.
  const [editing, setEditing] = useState(false)
  const [editName, setEditName] = useState('')
  const [editDescription, setEditDescription] = useState('')
  const [editLength, setEditLength] = useState('')
  const [editWidth, setEditWidth] = useState('')
  const [editConditionsOpen, setEditConditionsOpen] = useState(false)
  const [editSoilType, setEditSoilType] = useState('')
  const [editSoilPh, setEditSoilPh] = useState('')
  const [editSunExposure, setEditSunExposure] = useState('')
  const [editAspect, setEditAspect] = useState('')
  const [editDrainage, setEditDrainage] = useState('')
  const [editIrrigationType, setEditIrrigationType] = useState('')
  const [editProtection, setEditProtection] = useState('')
  const [editRaisedBed, setEditRaisedBed] = useState(false)

  const editPhNum = editSoilPh !== '' ? parseFloat(editSoilPh) : undefined
  const editPhOutOfRange = editPhNum !== undefined && (editPhNum < 3.0 || editPhNum > 9.0)

  const openEdit = () => {
    if (!bed) return
    setEditName(bed.name)
    setEditDescription(bed.description ?? '')
    setEditLength(bed.lengthMeters != null ? String(bed.lengthMeters) : '')
    setEditWidth(bed.widthMeters != null ? String(bed.widthMeters) : '')
    setEditSoilType(bed.soilType ?? '')
    setEditSoilPh(bed.soilPh != null ? String(bed.soilPh) : '')
    setEditSunExposure(bed.sunExposure ?? '')
    setEditAspect(bed.aspect ?? '')
    setEditDrainage(bed.drainage ?? '')
    setEditIrrigationType(bed.irrigationType ?? '')
    setEditProtection(bed.protection ?? '')
    setEditRaisedBed(bed.raisedBed ?? false)
    setEditConditionsOpen(false)
    setEditing(true)
  }

  const updateMut = useMutation({
    mutationFn: () => api.beds.update(bedId, {
      name: editName,
      description: editDescription || undefined,
      lengthMeters: editLength !== '' ? parseFloat(editLength) : undefined,
      widthMeters: editWidth !== '' ? parseFloat(editWidth) : undefined,
      soilType: editSoilType || undefined,
      soilPh: editPhNum,
      sunExposure: editSunExposure || undefined,
      drainage: editDrainage || undefined,
      aspect: editAspect || undefined,
      irrigationType: editIrrigationType || undefined,
      protection: editProtection || undefined,
      raisedBed: editRaisedBed,
    }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['bed', bedId] })
      qc.invalidateQueries({ queryKey: ['garden-beds', bed?.gardenId] })
      setEditing(false)
    },
  })

  if (!bed) return null

  const area =
    bed.lengthMeters && bed.widthMeters
      ? (bed.lengthMeters * bed.widthMeters).toFixed(1)
      : '—'

  const activePlants = plants?.filter((p) => p.status === 'PLANTED_OUT') ?? []
  const uniqueSpeciesCount = new Set(plants?.map((p) => p.speciesId).filter(Boolean)).size

  const sunKey = bed.sunExposure ? bed.sunExposure.toLowerCase() : null
  const irrigationKey = bed.irrigationType ? bed.irrigationType.toLowerCase() : null

  return (
    <div>
      <Masthead
        left={
          <span>
            <Link to="/gardens" style={{ color: 'inherit', textDecoration: 'none' }}>
              {t('nav.gardens')}
            </Link>
            {' / '}
            {garden ? (
              <Link to={`/garden/${garden.id}`} style={{ color: 'inherit', textDecoration: 'none' }}>
                {garden.name}
              </Link>
            ) : '…'}
            {' / '}
            <span style={{ color: 'var(--color-accent)' }}>{bed.name}</span>
          </span>
        }
        center={t('bed.masthead.center')}
        right={
          <button onClick={openEdit} className="btn-secondary">
            {t('common.edit')}
          </button>
        }
      />

      <div style={{ padding: '28px 40px' }}>
        {/* Alphabetical prev/next nav within the same garden */}
        {(prevBed || nextBed) && (
          <div
            style={{
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
              marginBottom: 20,
              fontFamily: 'var(--font-mono)',
              fontSize: 10,
              letterSpacing: 1.4,
              textTransform: 'uppercase',
            }}
          >
            {prevBed ? (
              <Link to={`/bed/${prevBed.id}`} style={{ color: 'var(--color-accent)', textDecoration: 'none' }}>
                ← {prevBed.name}
              </Link>
            ) : <span />}
            {nextBed ? (
              <Link to={`/bed/${nextBed.id}`} style={{ color: 'var(--color-accent)', textDecoration: 'none' }}>
                {nextBed.name} →
              </Link>
            ) : <span />}
          </div>
        )}

        {/* Hero row */}
        <div>
          <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap', marginBottom: 18 }}>
            {sunKey && <Chip tone="sage">{t(`bed.sun.${sunKey}`)}</Chip>}
            {irrigationKey && <Chip tone="sky">{t(`bed.irrigation.${irrigationKey}`)}</Chip>}
            {bed.raisedBed && <Chip tone="berry">{t('bed.raised')}</Chip>}
          </div>
          <h1
            style={{
              fontFamily: 'var(--font-display)',
              fontSize: 80,
              fontWeight: 300,
              lineHeight: 1,
              letterSpacing: -1.5,
              margin: 0,
              fontVariationSettings: '"SOFT" 100, "opsz" 144',
            }}
          >
            <span style={{ fontStyle: 'italic', color: 'var(--color-accent)' }}>{bed.name}.</span>
          </h1>
          {bed.description && (
            <p
              style={{
                marginTop: 16,
                fontFamily: 'Georgia, var(--font-display)',
                fontSize: 15,
                lineHeight: 1.6,
                color: 'var(--color-forest)',
              }}
            >
              {bed.description}
            </p>
          )}
        </div>

        {/* Stats band */}
        <div
          style={{
            margin: '40px 0',
            padding: '20px 0',
            borderTop: '1px solid var(--color-ink)',
            borderBottom: '1px solid var(--color-ink)',
            display: 'grid',
            gridTemplateColumns: 'repeat(5, 1fr)',
            gap: 18,
          }}
        >
          <Stat size="medium" value={activePlants.length} label={t('bed.stats.active')} hue="sage" />
          {/* Harvest totals, days-to-harvest and utilization aren't wired yet;
             show a dash instead of mocked numbers. */}
          <Stat size="medium" value="—" label={t('bed.stats.harvested')} hue="clay" />
          <Stat size="medium" value="—" label={t('bed.stats.daysToHarvest')} hue="mustard" />
          <Stat size="medium" value={plants?.length ?? 0} label={t('bed.stats.plants')} hue="sky" />
          <Stat size="medium" value="—" label={t('bed.stats.utilization')} hue="berry" />
        </div>

        {/* Plantor section */}
        <SectionHeader
          title={t('bed.plants.title')}
          meta={`${uniqueSpeciesCount} ${t('bed.plants.metaSuffix')}`}
          actions={
            <Link
              to={`/sow?bedId=${bedId}`}
              style={{
                display: 'inline-block',
                background: 'var(--color-accent)',
                color: 'var(--color-cream)',
                border: '1px solid var(--color-accent)',
                fontFamily: 'var(--font-mono)',
                fontSize: 11,
                letterSpacing: 1.6,
                textTransform: 'uppercase',
                textDecoration: 'none',
                padding: '7px 16px',
                whiteSpace: 'nowrap',
              }}
            >
              + {t('bed.plants.sow')}
            </Link>
          }
        />

        {/* Plant table header */}
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: '50px 1.5fr 70px 1fr 120px',
            gap: 18,
            padding: '10px 0',
            borderBottom: '1px solid var(--color-ink)',
            fontFamily: 'var(--font-mono)',
            fontSize: 9,
            letterSpacing: 1.4,
            textTransform: 'uppercase',
            color: 'var(--color-forest)',
            opacity: 0.7,
          }}
        >
          <span>№</span>
          <span>{t('bed.plants.col.species')}</span>
          <span>{t('bed.plants.col.count')}</span>
          <span>{t('bed.plants.col.status')}</span>
          <span>{t('bed.plants.col.timeline')}</span>
        </div>

        {/* Plant rows */}
        {plants?.map((p, i) => (
          <button
            key={p.id}
            onClick={() => p.speciesId != null && setModalSpecies(p.speciesId)}
            style={{
              display: 'grid',
              gridTemplateColumns: '50px 1.5fr 70px 1fr 120px',
              gap: 18,
              padding: '12px 0',
              borderBottom: '1px solid color-mix(in srgb, var(--color-ink) 20%, transparent)',
              background: 'transparent',
              border: 'none',
              borderBottomWidth: 1,
              borderBottomStyle: 'solid',
              borderBottomColor: 'color-mix(in srgb, var(--color-ink) 20%, transparent)',
              width: '100%',
              textAlign: 'left',
              cursor: p.speciesId != null ? 'pointer' : 'default',
              alignItems: 'center',
            }}
          >
            <span
              style={{
                fontFamily: 'var(--font-display)',
                fontStyle: 'italic',
                fontSize: 22,
                color: 'var(--color-accent)',
              }}
            >
              {String(i + 1).padStart(2, '0')}
            </span>
            <div style={{ fontFamily: 'var(--font-display)', fontSize: 20 }}>
              {p.speciesName ?? p.name}
            </div>
            <span
              style={{
                fontFamily: 'var(--font-display)',
                fontSize: 20,
                fontVariantNumeric: 'tabular-nums',
              }}
            >
              {p.seedCount ?? 1}
            </span>
            <span
              style={{
                fontFamily: 'var(--font-mono)',
                fontSize: 10,
                letterSpacing: 1.4,
                textTransform: 'uppercase',
              }}
            >
              {p.status}
            </span>
            <span style={{ fontFamily: 'var(--font-mono)', fontSize: 10 }}>
              {p.plantedDate ?? '—'}
            </span>
          </button>
        ))}

        {/* Harvest card — TODO: wire to real harvest stats when available */}
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
              background: 'var(--color-butter)',
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
            {t('bed.harvest.headline', { stems: 0 })}{' '}
            <span style={{ color: 'var(--color-blush)' }}>{t('bed.harvest.season', { year: new Date().getFullYear() })}</span>.
          </div>
        </div>

        {/* Bed layout — photo + dimensions side-by-side */}
        <div style={{ marginTop: 48, display: 'grid', gridTemplateColumns: '260px 1fr', gap: 22, alignItems: 'start' }}>
          <PhotoPlaceholder tone="sage" aspect="tall" label={bed.name.toUpperCase()} />
          <div
            style={{
              display: 'grid',
              gridTemplateColumns: '1fr 1fr',
              gap: 0,
              border: '1px solid var(--color-ink)',
            }}
          >
            <MetaCell label={t('bed.meta.length')} value={bed.lengthMeters ? `${bed.lengthMeters} m` : '—'} />
            <MetaCell label={t('bed.meta.width')} value={bed.widthMeters ? `${bed.widthMeters} m` : '—'} />
            <MetaCell label={t('bed.meta.orient')} value={bed.aspect ?? '—'} />
            <MetaCell label={t('bed.meta.area')} value={`${area} m²`} />
          </div>
        </div>

        {/* Danger callout */}
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
            {t('bed.danger.title')}
          </div>
          <p style={{ fontFamily: 'var(--font-display)', fontStyle: 'italic', fontSize: 15, margin: 0 }}>
            {t('bed.danger.warning')}
          </p>
          <button
            onClick={() => {
              if (window.confirm(t('bed.danger.confirm') ?? '')) {
                api.beds.delete(bedId)
              }
            }}
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
            → {t('bed.danger.delete', { id: String(bedId).padStart(2, '0') })}
          </button>
        </div>
      </div>

      <SpeciesEditModal speciesId={modalSpecies} onClose={() => setModalSpecies(null)} />

      {/* Edit bed dialog */}
      <Dialog
        open={editing}
        onClose={() => setEditing(false)}
        title={t('bed.editBedTitle') ?? 'Edit bed'}
        actions={
          <>
            <button onClick={() => setEditing(false)} className="px-4 py-2 text-sm text-text-secondary">{t('common.cancel')}</button>
            <button
              onClick={() => updateMut.mutate()}
              disabled={!editName.trim() || updateMut.isPending}
              className="btn-primary text-sm"
            >
              {updateMut.isPending ? t('common.saving') : t('common.save')}
            </button>
          </>
        }
      >
        <div className="space-y-4">
          <div>
            <label className="field-label">{t('common.nameLabel')}</label>
            <input value={editName} onChange={e => setEditName(e.target.value)} className="input w-full" />
          </div>
          <div>
            <label className="field-label">{t('common.descriptionLabel')}</label>
            <textarea value={editDescription} onChange={e => setEditDescription(e.target.value)} rows={2} className="input w-full" placeholder={t('common.optional')} />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="field-label">{t('bed.conditions.lengthMeters')}</label>
              <input type="number" step="0.1" min="0" value={editLength} onChange={e => setEditLength(e.target.value)} placeholder="—" className="input w-full" />
            </div>
            <div>
              <label className="field-label">{t('bed.conditions.widthMeters')}</label>
              <input type="number" step="0.1" min="0" value={editWidth} onChange={e => setEditWidth(e.target.value)} placeholder="—" className="input w-full" />
            </div>
          </div>

          <div className="border border-divider rounded-lg overflow-hidden">
            <button
              type="button"
              onClick={() => setEditConditionsOpen(o => !o)}
              className="w-full flex items-center justify-between px-4 py-3 text-left bg-surface hover:bg-divider transition-colors"
            >
              <span className="text-sm font-medium">{t('bed.conditions.sectionTitle')}</span>
              <span className="text-text-secondary text-sm">{editConditionsOpen ? '▲' : '▼'}</span>
            </button>
            {editConditionsOpen && (
              <div className="px-4 py-3 space-y-3 border-t border-divider">
                <div className="grid grid-cols-1 gap-3 md:grid-cols-2">
                  <div>
                    <label className="field-label">{t('bed.conditions.soilType')}</label>
                    <select value={editSoilType} onChange={e => setEditSoilType(e.target.value)} className="input">
                      <option value="">{t('common.select')}</option>
                      {SOIL_TYPES.map(v => <option key={v} value={v}>{t(`bed.conditions.soilTypes.${v}`)}</option>)}
                    </select>
                  </div>
                  <div>
                    <label className="field-label">{t('bed.conditions.soilPh')}</label>
                    <input type="number" step="0.1" min="0" max="14" value={editSoilPh} onChange={e => setEditSoilPh(e.target.value)} placeholder="—" className="input" />
                    {editPhOutOfRange && <p className="text-error text-xs mt-1">{t('bed.conditions.phHint')}</p>}
                  </div>
                  <div>
                    <label className="field-label">{t('bed.conditions.sunExposure')}</label>
                    <select value={editSunExposure} onChange={e => setEditSunExposure(e.target.value)} className="input">
                      <option value="">{t('common.select')}</option>
                      {SUN_EXPOSURES.map(v => <option key={v} value={v}>{t(`bed.conditions.sunExposures.${v}`)}</option>)}
                    </select>
                  </div>
                  <div>
                    <label className="field-label">{t('bed.conditions.aspect')}</label>
                    <select value={editAspect} onChange={e => setEditAspect(e.target.value)} className="input">
                      <option value="">{t('common.select')}</option>
                      {ASPECTS.map(v => <option key={v} value={v}>{t(`bed.conditions.aspects.${v}`)}</option>)}
                    </select>
                  </div>
                  <div>
                    <label className="field-label">{t('bed.conditions.drainage')}</label>
                    <select value={editDrainage} onChange={e => setEditDrainage(e.target.value)} className="input">
                      <option value="">{t('common.select')}</option>
                      {DRAINAGES.map(v => <option key={v} value={v}>{t(`bed.conditions.drainages.${v}`)}</option>)}
                    </select>
                  </div>
                  <div>
                    <label className="field-label">{t('bed.conditions.irrigationType')}</label>
                    <select value={editIrrigationType} onChange={e => setEditIrrigationType(e.target.value)} className="input">
                      <option value="">{t('common.select')}</option>
                      {IRRIGATION_TYPES.map(v => <option key={v} value={v}>{t(`bed.conditions.irrigationTypes.${v}`)}</option>)}
                    </select>
                  </div>
                  <div>
                    <label className="field-label">{t('bed.conditions.protection')}</label>
                    <select value={editProtection} onChange={e => setEditProtection(e.target.value)} className="input">
                      <option value="">{t('common.select')}</option>
                      {PROTECTIONS.map(v => <option key={v} value={v}>{t(`bed.conditions.protections.${v}`)}</option>)}
                    </select>
                  </div>
                  <div className="flex items-center gap-2 pt-1">
                    <input
                      id="raisedBed-edit"
                      type="checkbox"
                      checked={editRaisedBed}
                      onChange={e => setEditRaisedBed(e.target.checked)}
                      className="h-4 w-4 rounded border-divider accent-accent"
                    />
                    <label htmlFor="raisedBed-edit" className="text-sm">{t('bed.conditions.raisedBed')}</label>
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

function MetaCell({ label, value }: { label: string; value: string }) {
  return (
    <div
      style={{
        padding: '10px 14px',
        borderTop: '1px solid var(--color-ink)',
        borderLeft: '1px solid var(--color-ink)',
      }}
    >
      <div
        style={{
          fontFamily: 'var(--font-mono)',
          fontSize: 9,
          letterSpacing: 1.4,
          textTransform: 'uppercase',
          color: 'var(--color-forest)',
          opacity: 0.7,
        }}
      >
        {label}
      </div>
      <div style={{ fontFamily: 'var(--font-display)', fontSize: 22 }}>{value}</div>
    </div>
  )
}

function SectionHeader({
  title,
  meta,
  actions,
}: {
  title: string
  meta?: string
  actions?: React.ReactNode
}) {
  return (
    <div style={{ display: 'flex', alignItems: 'baseline', gap: 12, margin: '40px 0 12px' }}>
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
        {title}
        <span style={{ color: 'var(--color-accent)' }}>.</span>
      </h2>
      {meta && (
        <span
          style={{
            fontFamily: 'var(--font-mono)',
            fontSize: 10,
            letterSpacing: 1.4,
            textTransform: 'uppercase',
          }}
        >
          {meta}
        </span>
      )}
      {actions && <div style={{ marginLeft: 'auto' }}>{actions}</div>}
    </div>
  )
}
