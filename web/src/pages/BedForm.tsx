import { useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useMutation, useQueryClient, useQuery } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { api } from '../api/client'
import { PageHeader } from '../components/PageHeader'
import type { BreadcrumbItem } from '../components/Breadcrumb'
import { useOnboarding } from '../onboarding/OnboardingContext'

const SOIL_TYPES = ['SANDY', 'LOAMY', 'CLAY', 'SILTY', 'PEATY', 'CHALKY'] as const
const SUN_EXPOSURES = ['FULL_SUN', 'PARTIAL_SUN', 'PARTIAL_SHADE', 'FULL_SHADE'] as const
const DRAINAGES = ['POOR', 'MODERATE', 'GOOD', 'SHARP'] as const
const ASPECTS = ['FLAT', 'N', 'NE', 'E', 'SE', 'S', 'SW', 'W', 'NW'] as const
const IRRIGATION_TYPES = ['DRIP', 'SPRINKLER', 'SOAKER_HOSE', 'MANUAL', 'NONE'] as const
const PROTECTIONS = ['OPEN_FIELD', 'ROW_COVER', 'LOW_TUNNEL', 'HIGH_TUNNEL', 'GREENHOUSE', 'COLDFRAME'] as const

export function BedForm() {
  const { gardenId } = useParams<{ gardenId: string }>()
  const navigate = useNavigate()
  const qc = useQueryClient()
  const { t } = useTranslation()
  const { completeStep } = useOnboarding()
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [lengthMeters, setLengthMeters] = useState('')
  const [widthMeters, setWidthMeters] = useState('')

  // Conditions — collapsed by default on create
  const [conditionsOpen, setConditionsOpen] = useState(false)
  const [soilType, setSoilType] = useState('')
  const [soilPh, setSoilPh] = useState('')
  const [sunExposure, setSunExposure] = useState('')
  const [aspect, setAspect] = useState('')
  const [drainage, setDrainage] = useState('')
  const [irrigationType, setIrrigationType] = useState('')
  const [protection, setProtection] = useState('')
  const [raisedBed, setRaisedBed] = useState(false)

  const phNum = soilPh !== '' ? parseFloat(soilPh) : undefined
  const phOutOfRange = phNum !== undefined && (phNum < 3.0 || phNum > 9.0)

  const { data: garden } = useQuery({
    queryKey: ['garden', Number(gardenId)],
    queryFn: () => api.gardens.get(Number(gardenId)),
  })

  const mutation = useMutation({
    mutationFn: () => api.beds.create(Number(gardenId), {
      name,
      description: description || undefined,
      lengthMeters: lengthMeters !== '' ? parseFloat(lengthMeters) : undefined,
      widthMeters: widthMeters !== '' ? parseFloat(widthMeters) : undefined,
      soilType: soilType || undefined,
      soilPh: phNum,
      sunExposure: sunExposure || undefined,
      drainage: drainage || undefined,
      aspect: aspect || undefined,
      irrigationType: irrigationType || undefined,
      protection: protection || undefined,
      raisedBed,
    }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['garden-beds', Number(gardenId)] })
      completeStep('create_bed')
      navigate(`/garden/${gardenId}`, { replace: true })
    },
  })

  const breadcrumbs: BreadcrumbItem[] = [
    { label: t('nav.myWorld'), to: '/' },
    { label: garden?.name ?? '…', to: `/garden/${gardenId}` },
  ]

  return (
    <div className="max-w-lg">
      <PageHeader title={t('bed.newBedTitle')} breadcrumbs={breadcrumbs} />
      <div className="form-card">
        <div>
          <label className="field-label">{t('common.nameLabel')}</label>
          <input value={name} onChange={e => setName(e.target.value)} placeholder={t('bed.bedNamePlaceholder')} className="input" />
          <p className="text-xs text-text-secondary mt-1">{t('bed.bedNameHint')}</p>
        </div>
        <div>
          <label className="field-label">{t('common.descriptionLabel')}</label>
          <textarea value={description} onChange={e => setDescription(e.target.value)} placeholder={t('common.optional')} rows={2} className="input" />
        </div>
        <div className="grid grid-cols-2 gap-3">
          <div>
            <label className="field-label">{t('bed.conditions.lengthMeters')}</label>
            <input type="number" step="0.1" min="0" value={lengthMeters} onChange={e => setLengthMeters(e.target.value)} placeholder="—" className="input" />
          </div>
          <div>
            <label className="field-label">{t('bed.conditions.widthMeters')}</label>
            <input type="number" step="0.1" min="0" value={widthMeters} onChange={e => setWidthMeters(e.target.value)} placeholder="—" className="input" />
          </div>
        </div>

        <div className="border border-divider rounded-lg overflow-hidden">
          <button
            type="button"
            onClick={() => setConditionsOpen(o => !o)}
            className="w-full flex items-center justify-between px-4 py-3 text-left bg-surface hover:bg-divider transition-colors"
          >
            <span className="text-sm font-medium">{t('bed.conditions.sectionTitle')}</span>
            <span className="text-text-secondary text-sm">{conditionsOpen ? '▲' : '▼'}</span>
          </button>
          {conditionsOpen && (
            <div className="px-4 py-3 space-y-3 border-t border-divider">
              <div className="grid grid-cols-1 gap-3 md:grid-cols-2">
                <div>
                  <label className="field-label">{t('bed.conditions.soilType')}</label>
                  <select value={soilType} onChange={e => setSoilType(e.target.value)} className="input">
                    <option value="">{t('common.select')}</option>
                    {SOIL_TYPES.map(v => <option key={v} value={v}>{t(`bed.conditions.soilTypes.${v}`)}</option>)}
                  </select>
                </div>
                <div>
                  <label className="field-label">{t('bed.conditions.soilPh')}</label>
                  <input type="number" step="0.1" min="0" max="14" value={soilPh} onChange={e => setSoilPh(e.target.value)} placeholder="—" className="input" />
                  {phOutOfRange && <p className="text-error text-xs mt-1">{t('bed.conditions.phHint')}</p>}
                </div>
                <div>
                  <label className="field-label">{t('bed.conditions.sunExposure')}</label>
                  <select value={sunExposure} onChange={e => setSunExposure(e.target.value)} className="input">
                    <option value="">{t('common.select')}</option>
                    {SUN_EXPOSURES.map(v => <option key={v} value={v}>{t(`bed.conditions.sunExposures.${v}`)}</option>)}
                  </select>
                </div>
                <div>
                  <label className="field-label">{t('bed.conditions.aspect')}</label>
                  <select value={aspect} onChange={e => setAspect(e.target.value)} className="input">
                    <option value="">{t('common.select')}</option>
                    {ASPECTS.map(v => <option key={v} value={v}>{t(`bed.conditions.aspects.${v}`)}</option>)}
                  </select>
                </div>
                <div>
                  <label className="field-label">{t('bed.conditions.drainage')}</label>
                  <select value={drainage} onChange={e => setDrainage(e.target.value)} className="input">
                    <option value="">{t('common.select')}</option>
                    {DRAINAGES.map(v => <option key={v} value={v}>{t(`bed.conditions.drainages.${v}`)}</option>)}
                  </select>
                </div>
                <div>
                  <label className="field-label">{t('bed.conditions.irrigationType')}</label>
                  <select value={irrigationType} onChange={e => setIrrigationType(e.target.value)} className="input">
                    <option value="">{t('common.select')}</option>
                    {IRRIGATION_TYPES.map(v => <option key={v} value={v}>{t(`bed.conditions.irrigationTypes.${v}`)}</option>)}
                  </select>
                </div>
                <div>
                  <label className="field-label">{t('bed.conditions.protection')}</label>
                  <select value={protection} onChange={e => setProtection(e.target.value)} className="input">
                    <option value="">{t('common.select')}</option>
                    {PROTECTIONS.map(v => <option key={v} value={v}>{t(`bed.conditions.protections.${v}`)}</option>)}
                  </select>
                </div>
                <div className="flex items-center gap-2 pt-1">
                  <input
                    id="raisedBed-create"
                    type="checkbox"
                    checked={raisedBed}
                    onChange={e => setRaisedBed(e.target.checked)}
                    className="h-4 w-4 rounded border-divider accent-accent"
                  />
                  <label htmlFor="raisedBed-create" className="text-sm">{t('bed.conditions.raisedBed')}</label>
                </div>
              </div>
            </div>
          )}
        </div>
      </div>
      {mutation.error && <p className="text-error text-sm mt-3">{mutation.error instanceof Error ? mutation.error.message : String(mutation.error)}</p>}
      <div className="mt-4 flex justify-end">
        <button onClick={() => mutation.mutate()} disabled={!name.trim() || mutation.isPending} className="btn-primary">
          {mutation.isPending ? t('bed.creatingBed') : t('bed.createBed')}
        </button>
      </div>
    </div>
  )
}
