import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useParams, useNavigate, Link } from 'react-router-dom'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { api } from '../api/client'
import type { BedResponse } from '../api/client'
import type { BreadcrumbItem } from '../components/Breadcrumb'
import { PageHeader } from '../components/PageHeader'
import { ErrorDisplay } from '../components/ErrorDisplay'
import { StatusBadge } from '../components/StatusBadge'
import { Dialog } from '../components/Dialog'

const SOIL_TYPES = ['SANDY', 'LOAMY', 'CLAY', 'SILTY', 'PEATY', 'CHALKY'] as const
const SUN_EXPOSURES = ['FULL_SUN', 'PARTIAL_SUN', 'PARTIAL_SHADE', 'FULL_SHADE'] as const
const DRAINAGES = ['POOR', 'MODERATE', 'GOOD', 'SHARP'] as const
const ASPECTS = ['FLAT', 'N', 'NE', 'E', 'SE', 'S', 'SW', 'W', 'NW'] as const
const IRRIGATION_TYPES = ['DRIP', 'SPRINKLER', 'SOAKER_HOSE', 'MANUAL', 'NONE'] as const
const PROTECTIONS = ['OPEN_FIELD', 'ROW_COVER', 'LOW_TUNNEL', 'HIGH_TUNNEL', 'GREENHOUSE', 'COLDFRAME'] as const

function hasAnyCondition(bed: BedResponse): boolean {
  return !!(
    bed.soilType || bed.soilPh != null || bed.sunExposure || bed.drainage ||
    bed.aspect || bed.irrigationType || bed.protection || bed.raisedBed != null
  )
}

export function BedDetail() {
  const { id } = useParams<{ id: string }>()
  const bedId = Number(id)
  const navigate = useNavigate()
  const qc = useQueryClient()
  const { t, i18n } = useTranslation()

  const { data: bed, error, isLoading, refetch } = useQuery({
    queryKey: ['bed', bedId],
    queryFn: () => api.beds.get(bedId),
  })

  const { data: plants } = useQuery({
    queryKey: ['bed-plants', bedId],
    queryFn: () => api.beds.plants(bedId),
  })

  const { data: garden } = useQuery({
    queryKey: ['garden', bed?.gardenId],
    queryFn: () => api.gardens.get(bed!.gardenId),
    enabled: !!bed,
  })

  const { data: speciesList } = useQuery({
    queryKey: ['species'],
    queryFn: api.species.list,
  })

  const { data: applications } = useQuery({
    queryKey: ['bed-applications', bedId],
    queryFn: () => api.supplyApplications.listByBed(bedId, 10),
  })

  const [editing, setEditing] = useState(false)
  const [editName, setEditName] = useState('')
  const [editDesc, setEditDesc] = useState('')
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

  const [showDelete, setShowDelete] = useState(false)
  const [showAdd, setShowAdd] = useState(false)
  const [expandedGroups, setExpandedGroups] = useState<Set<string>>(new Set())

  const editPhNum = editSoilPh !== '' ? parseFloat(editSoilPh) : undefined
  const editPhOutOfRange = editPhNum !== undefined && (editPhNum < 3.0 || editPhNum > 9.0)

  function openEdit(b: BedResponse) {
    setEditName(b.name)
    setEditDesc(b.description ?? '')
    setEditLength(b.lengthMeters != null ? String(b.lengthMeters) : '')
    setEditWidth(b.widthMeters != null ? String(b.widthMeters) : '')
    setEditSoilType(b.soilType ?? '')
    setEditSoilPh(b.soilPh != null ? String(b.soilPh) : '')
    setEditSunExposure(b.sunExposure ?? '')
    setEditAspect(b.aspect ?? '')
    setEditDrainage(b.drainage ?? '')
    setEditIrrigationType(b.irrigationType ?? '')
    setEditProtection(b.protection ?? '')
    setEditRaisedBed(b.raisedBed ?? false)
    setEditConditionsOpen(hasAnyCondition(b))
    setEditing(true)
  }

  const updateMut = useMutation({
    mutationFn: () => api.beds.update(bedId, {
      name: editName,
      description: editDesc || undefined,
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
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['bed', bedId] }); setEditing(false) },
  })

  const [deleteError, setDeleteError] = useState<string | null>(null)

  const deleteMut = useMutation({
    mutationFn: () => api.beds.delete(bedId),
    onSuccess: () => { navigate(-1); qc.invalidateQueries({ queryKey: ['garden-beds'] }) },
    onError: (err) => { setDeleteError(err instanceof Error ? err.message : String(err)) },
  })

  if (isLoading) return <div className="flex justify-center p-16"><div className="animate-spin h-8 w-8 border-2 border-accent border-t-transparent rounded-full" /></div>
  if (error) return <ErrorDisplay error={error} onRetry={refetch} />
  if (!bed) return null

  const resolveSpeciesName = (p: { speciesId?: number; speciesName?: string; name: string }) => {
    if (p.speciesId && speciesList) {
      const sp = speciesList.find(s => s.id === p.speciesId)
      if (sp) {
        const name = i18n.language === 'sv' ? (sp.commonNameSv ?? sp.commonName) : sp.commonName
        const variant = i18n.language === 'sv' ? (sp.variantNameSv ?? sp.variantName) : sp.variantName
        return variant ? `${name} — ${variant}` : name
      }
    }
    return p.speciesName ?? p.name
  }

  const grouped = new Map<string, typeof plants>()
  plants?.forEach(p => {
    const key = resolveSpeciesName(p)
    if (!grouped.has(key)) grouped.set(key, [])
    grouped.get(key)!.push(p)
  })

  const toggleGroup = (key: string) => {
    setExpandedGroups(prev => {
      const next = new Set(prev)
      if (next.has(key)) next.delete(key); else next.add(key)
      return next
    })
  }

  const breadcrumbs: BreadcrumbItem[] = [
    { label: t('nav.myWorld'), to: '/' },
    { label: garden?.name ?? '…', to: `/garden/${bed.gardenId}` },
  ]

  return (
    <div className="flex flex-col flex-1">
      <PageHeader
        title={bed.name}
        breadcrumbs={breadcrumbs}
        editAction={() => openEdit(bed)}
      />

      <div className="px-4 py-4 space-y-3">
        {bed.description && <p className="text-text-secondary">{bed.description}</p>}

        <div className="flex items-center justify-between">
          <h2 className="font-semibold">{t('bed.plants')}</h2>
          <div className="flex gap-2">
            <button onClick={() => setShowAdd(true)} className="btn-primary text-sm">{t('bed.addPlant')}</button>
            <Link to={`/activity/apply-supply?bedId=${bedId}`} className="btn-secondary text-sm no-underline">
              {t('supplyApplication.fertilize')}
            </Link>
          </div>
        </div>

        {plants && plants.length === 0 && (
          <p className="text-text-secondary text-sm">{t('bed.noPlantsYet')}</p>
        )}

        {Array.from(grouped.entries()).map(([species, group]) => {
          const expanded = expandedGroups.has(species)
          return (
            <div key={species} className="card p-0 overflow-hidden">
              <button aria-expanded={expanded} onClick={() => toggleGroup(species)} className="w-full flex items-center justify-between px-4 py-3 text-left">
                <div>
                  <p className="font-semibold">{species}</p>
                  <p className="text-xs text-text-secondary">{t('bed.plantCount', { count: group!.length })}</p>
                </div>
                <span className="text-text-secondary text-lg">{expanded ? '▲' : '▼'}</span>
              </button>
              {expanded && (
                <div className="border-t border-divider">
                  {group!.map((p, i) => (
                    <div key={p.id}>
                      <Link to={`/plant/${p.id}`} className="flex items-center justify-between px-4 py-2.5 text-sm no-underline text-inherit hover:bg-surface">
                        <span>{p.name}</span>
                        <StatusBadge status={p.status} />
                      </Link>
                      {i < group!.length - 1 && <hr className="mx-4 border-divider" />}
                    </div>
                  ))}
                </div>
              )}
            </div>
          )
        })}
      </div>

      <div className="px-4 mt-6 space-y-2">
        <h2 className="font-semibold">{t('supplyApplication.historyTitle')}</h2>
        {applications && applications.length === 0 && (
          <p className="text-text-secondary text-sm">{t('supplyApplication.noApplications')}</p>
        )}
        {applications?.map(a => (
          <div key={a.id} className="card text-sm flex items-center justify-between">
            <div>
              <p className="font-semibold">{a.supplyTypeName}</p>
              <p className="text-xs text-text-secondary">
                {a.quantity} {a.supplyUnit.toLowerCase()} · {a.targetScope === 'BED'
                  ? t('supplyApplication.appliedToBed')
                  : t('supplyApplication.appliedToPlants', { count: a.plantIds.length })}
              </p>
            </div>
            <span className="text-xs text-text-secondary">{new Date(a.appliedAt).toLocaleDateString()}</span>
          </div>
        ))}
      </div>

      <div className="mt-auto px-4 pb-4 pt-6">
        <div className="rounded-xl border border-error/20 overflow-hidden">
          <div className="px-4 py-2 bg-error/5 border-b border-error/20">
            <p className="text-xs font-semibold text-error uppercase tracking-wide">{t('common.dangerZone')}</p>
          </div>
          <div className="px-4 py-3 flex items-center justify-between">
            <p className="text-sm text-text-secondary">{t('bed.deleteBedConfirm')}</p>
            <button onClick={() => setShowDelete(true)} className="ml-4 shrink-0 text-sm font-medium text-error hover:underline">
              {t('bed.deleteBed')}
            </button>
          </div>
        </div>
      </div>

      <Dialog open={editing} onClose={() => setEditing(false)} title={t('bed.editBedTitle')} actions={
        <>
          <button onClick={() => setEditing(false)} className="px-4 py-2 text-sm text-text-secondary">{t('common.cancel')}</button>
          <button onClick={() => updateMut.mutate()} disabled={!editName.trim()} className="btn-primary text-sm">{t('common.save')}</button>
        </>
      }>
        <div className="space-y-3">
          <input value={editName} onChange={e => setEditName(e.target.value)} placeholder={t('bed.bedNamePlaceholder')} className="input" />
          <textarea value={editDesc} onChange={e => setEditDesc(e.target.value)} placeholder={t('bed.descriptionOptionalPlaceholder')} rows={2} className="input" />
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="field-label">{t('bed.conditions.lengthMeters')}</label>
              <input type="number" step="0.1" min="0" value={editLength} onChange={e => setEditLength(e.target.value)} placeholder="—" className="input" />
            </div>
            <div>
              <label className="field-label">{t('bed.conditions.widthMeters')}</label>
              <input type="number" step="0.1" min="0" value={editWidth} onChange={e => setEditWidth(e.target.value)} placeholder="—" className="input" />
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

      <Dialog open={showDelete} onClose={() => { setShowDelete(false); setDeleteError(null) }} title={t('bed.deleteBedTitle')} actions={
        <>
          <button onClick={() => { setShowDelete(false); setDeleteError(null) }} className="px-4 py-2 text-sm text-text-secondary">{t('common.cancel')}</button>
          <button onClick={() => deleteMut.mutate()} className="px-4 py-2 text-sm text-error font-semibold">{t('common.delete')}</button>
        </>
      }>
        <p className="text-text-secondary">{t('bed.deleteBedConfirm')}</p>
        {deleteError && <p className="text-error text-sm mt-2">{deleteError}</p>}
      </Dialog>

      <Dialog open={showAdd} onClose={() => setShowAdd(false)} title={t('bed.addPlantTitle')} actions={
        <button onClick={() => setShowAdd(false)} className="px-4 py-2 text-sm text-text-secondary">{t('common.cancel')}</button>
      }>
        <div className="space-y-2">
          <button onClick={() => { setShowAdd(false); navigate(`/sow?bedId=${bedId}`) }} className="btn-primary w-full text-sm">{t('bed.sowSeedsInBed')}</button>
        </div>
      </Dialog>

    </div>
  )
}
