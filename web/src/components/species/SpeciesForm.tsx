import { useId, useRef, useState, type ReactNode } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { api, type SpeciesResponse } from '../../api/client'
import { Field, Rule } from '../faltet'
import { fileToJpegBase64 } from '../PhotoPicker'

const GROWING_POSITIONS = ['SUNNY', 'PARTIALLY_SUNNY', 'SHADOWY'] as const
const SOIL_TYPES = ['CLAY', 'SANDY', 'LOAMY', 'CHALKY', 'PEATY', 'SILTY'] as const
const PLANT_TYPES = ['ANNUAL', 'BIENNIAL', 'PERENNIAL', 'BULB', 'TUBER'] as const
const UNIT_TYPES = ['SEED', 'PLUG', 'BULB', 'TUBER', 'PLANT'] as const

/** What the form submits besides the species itself; providers and workflows are separate resources. */
export interface SpeciesFormExtras {
  providerId?: number
  productUrl?: string
  workflowTemplateId?: number
}

/**
 * The variety form, laid out like the admin's: provider, photos, names, growth,
 * months, conditions, providers, tags, commercial figures and workflow.
 * Users don't upload seed packet images to providers; that stays an admin task.
 */
export function SpeciesForm({
  species,
  template,
  title,
  submitLabel,
  isSubmitting,
  error,
  onCancel,
  onSubmit,
  workflowSection,
}: {
  /** The variety being edited; null when creating. */
  species: SpeciesResponse | null
  /** Values to start a new variety from (copying). */
  template?: SpeciesResponse | null
  title: string
  submitLabel: string
  isSubmitting: boolean
  error: string | null
  onCancel: () => void
  onSubmit: (req: Record<string, unknown>, extras: SpeciesFormExtras) => void
  /** Edit mode renders the variety's own workflow controls here. */
  workflowSection?: ReactNode
}) {
  const { t, i18n } = useTranslation()
  const isEdit = species !== null
  const readOnly = species?.isSystem ?? false
  const init = species ?? template ?? null
  const text = (v: number | undefined | null) => v?.toString() ?? ''

  const [commonName, setCommonName] = useState(init?.commonName ?? '')
  const [commonNameSv, setCommonNameSv] = useState(init?.commonNameSv ?? '')
  const [variantName, setVariantName] = useState(init?.variantName ?? '')
  const [variantNameSv, setVariantNameSv] = useState(init?.variantNameSv ?? '')
  const [scientificName, setScientificName] = useState(init?.scientificName ?? '')
  const [germinationTimeDaysMin, setGerminationTimeDaysMin] = useState(text(init?.germinationTimeDaysMin))
  const [germinationTimeDaysMax, setGerminationTimeDaysMax] = useState(text(init?.germinationTimeDaysMax))
  const [daysToHarvestMin, setDaysToHarvestMin] = useState(text(init?.daysToHarvestMin))
  const [daysToHarvestMax, setDaysToHarvestMax] = useState(text(init?.daysToHarvestMax))
  const [heightCmMin, setHeightCmMin] = useState(text(init?.heightCmMin))
  const [heightCmMax, setHeightCmMax] = useState(text(init?.heightCmMax))
  const [sowingDepthMm, setSowingDepthMm] = useState(text(init?.sowingDepthMm))
  const [germinationRate, setGerminationRate] = useState(text(init?.germinationRate))
  const [sowingMonths, setSowingMonths] = useState(new Set(init?.sowingMonths ?? []))
  const [bloomMonths, setBloomMonths] = useState(new Set(init?.bloomMonths ?? []))
  const [positions, setPositions] = useState(new Set(init?.growingPositions ?? []))
  const [soils, setSoils] = useState(new Set(init?.soils ?? []))
  const [tagIds, setTagIds] = useState(new Set(init?.tags.map(tg => tg.id) ?? []))
  const [costPerSeedSek, setCostPerSeedSek] = useState(init?.costPerSeedCents != null ? String(init.costPerSeedCents / 100) : '')
  const [expectedStemsPerPlant, setExpectedStemsPerPlant] = useState(text(init?.expectedStemsPerPlant))
  const [expectedVaseLifeDays, setExpectedVaseLifeDays] = useState(text(init?.expectedVaseLifeDays))
  const [plantType, setPlantType] = useState(init?.plantType ?? '')
  const [defaultUnitType, setDefaultUnitType] = useState(init?.defaultUnitType ?? '')
  const [providerId, setProviderId] = useState('')
  const [productUrl, setProductUrl] = useState('')
  const [workflowTemplateId, setWorkflowTemplateId] = useState('')

  const { data: providers = [] } = useQuery({ queryKey: ['providers'], queryFn: api.providers.list, enabled: !isEdit })
  const { data: tags = [] } = useQuery({ queryKey: ['species-tags'], queryFn: api.species.tags })
  const { data: templates = [] } = useQuery({ queryKey: ['workflow-templates'], queryFn: api.workflows.templates, enabled: !isEdit })

  const digits = (set: (v: string) => void) => (v: string) => set(v.replace(/\D/g, ''))
  const toggle = <T,>(current: Set<T>, set: (s: Set<T>) => void, value: T) => {
    const next = new Set(current)
    if (next.has(value)) next.delete(value); else next.add(value)
    set(next)
  }
  const int = (v: string) => (v ? parseInt(v, 10) : undefined)
  const monthLabel = (m: number) => new Date(2000, m - 1, 1).toLocaleString(i18n.language, { month: 'short' })

  const submit = () => {
    const req: Record<string, unknown> = {
      commonName: commonName.trim() || commonNameSv.trim(),
      commonNameSv: commonNameSv || undefined,
      variantName: variantName || undefined,
      variantNameSv: variantNameSv || undefined,
      scientificName: scientificName || undefined,
      germinationTimeDaysMin: int(germinationTimeDaysMin),
      germinationTimeDaysMax: int(germinationTimeDaysMax),
      daysToHarvestMin: int(daysToHarvestMin),
      daysToHarvestMax: int(daysToHarvestMax),
      sowingDepthMm: int(sowingDepthMm),
      heightCmMin: int(heightCmMin),
      heightCmMax: int(heightCmMax),
      bloomMonths: [...bloomMonths].sort((a, b) => a - b),
      sowingMonths: [...sowingMonths].sort((a, b) => a - b),
      germinationRate: int(germinationRate),
      growingPositions: [...positions],
      soils: [...soils],
      tagIds: [...tagIds],
      costPerSeedCents: costPerSeedSek ? Math.round(parseFloat(costPerSeedSek.replace(',', '.')) * 100) : undefined,
      expectedStemsPerPlant: int(expectedStemsPerPlant),
      expectedVaseLifeDays: int(expectedVaseLifeDays),
      plantType: plantType || undefined,
      defaultUnitType: defaultUnitType || undefined,
    }
    onSubmit(req, {
      providerId: !isEdit && providerId ? Number(providerId) : undefined,
      productUrl: productUrl || undefined,
      workflowTemplateId: !isEdit && workflowTemplateId ? Number(workflowTemplateId) : undefined,
    })
  }

  return (
    <div>
      <div className="page-body-tight">
        <h1 style={{ fontFamily: 'var(--font-display)', fontSize: 34, fontWeight: 300, margin: '0 0 8px', lineHeight: 1.15 }}>{title}</h1>
        {readOnly && <p style={NOTE}>{t('speciesForm.catalogueReadOnly')}</p>}

        <fieldset disabled={readOnly} style={{ border: 0, padding: 0, margin: 0, minWidth: 0 }}>
          {!isEdit && (
            <Section title={t('speciesForm.seedProvider')}>
              <div style={GRID}>
                <SelectField label={t('speciesForm.provider')} value={providerId} onChange={setProviderId}>
                  <option value="">{t('speciesForm.selectProvider')}</option>
                  {providers.map(p => <option key={p.id} value={p.id}>{p.name}</option>)}
                </SelectField>
                {providerId && <Field label={t('speciesForm.productUrl')} editable value={productUrl} onChange={setProductUrl} />}
              </div>
            </Section>
          )}

          {isEdit && !readOnly && <PhotosSection species={species} />}

          <Section
            title={t('speciesForm.basicInfo')}
            action={!readOnly && (
              <button
                type="button"
                style={LINK}
                onClick={() => {
                  if (!commonName && commonNameSv) setCommonName(commonNameSv)
                  if (!variantName && variantNameSv) setVariantName(variantNameSv)
                }}
              >
                {t('speciesForm.copyToEnglish')}
              </button>
            )}
          >
            <div style={GRID}>
              <Field label={t('speciesForm.commonNameSv')} editable value={commonNameSv} onChange={setCommonNameSv} accent="clay" />
              <Field label={t('speciesForm.commonNameEn')} editable value={commonName} onChange={setCommonName} accent="clay" />
              <Field
                label={t('speciesForm.variantNameSv')}
                editable
                value={variantNameSv}
                onChange={setVariantNameSv}
                onBlur={() => { if (variantName.trimEnd().endsWith('(kopia)')) setVariantName(variantNameSv) }}
                accent="clay"
              />
              <Field label={t('speciesForm.variantNameEn')} editable value={variantName} onChange={setVariantName} accent="clay" />
              <Field label={t('speciesForm.scientificName')} editable value={scientificName} onChange={setScientificName} accent="sage" />
            </div>
          </Section>

          <Section title={t('speciesForm.growthInfo')}>
            <div style={{ display: 'grid', gap: 14 }}>
              <RangeField label={t('speciesForm.germinationTimeDays')} min={germinationTimeDaysMin} max={germinationTimeDaysMax} onMin={digits(setGerminationTimeDaysMin)} onMax={digits(setGerminationTimeDaysMax)} />
              <RangeField label={t('speciesForm.daysToHarvest')} min={daysToHarvestMin} max={daysToHarvestMax} onMin={digits(setDaysToHarvestMin)} onMax={digits(setDaysToHarvestMax)} />
              <RangeField label={t('speciesForm.heightCm')} min={heightCmMin} max={heightCmMax} onMin={digits(setHeightCmMin)} onMax={digits(setHeightCmMax)} />
            </div>
            <div style={{ ...GRID, marginTop: 18 }}>
              <Field label={t('speciesForm.sowingDepthMm')} editable value={sowingDepthMm} onChange={digits(setSowingDepthMm)} accent="mustard" />
              <Field label={t('speciesForm.germinationRatePercent')} editable value={germinationRate} onChange={digits(setGerminationRate)} accent="sage" />
            </div>
          </Section>

          <Section title={t('speciesForm.months')}>
            <ChipGroup label={t('speciesForm.sowingMonths')}>
              {MONTHS.map(m => <ChipToggle key={m} label={monthLabel(m)} selected={sowingMonths.has(m)} onClick={() => toggle(sowingMonths, setSowingMonths, m)} />)}
            </ChipGroup>
            <ChipGroup label={t('speciesForm.bloomMonths')}>
              {MONTHS.map(m => <ChipToggle key={m} label={monthLabel(m)} selected={bloomMonths.has(m)} onClick={() => toggle(bloomMonths, setBloomMonths, m)} />)}
            </ChipGroup>
          </Section>

          <Section title={t('speciesForm.growingConditions')}>
            <ChipGroup label={t('speciesForm.growingPosition')}>
              {GROWING_POSITIONS.map(p => <ChipToggle key={p} label={t(`speciesForm.position.${p}`)} selected={positions.has(p)} onClick={() => toggle(positions, setPositions, p)} />)}
            </ChipGroup>
            <ChipGroup label={t('speciesForm.soilType')}>
              {SOIL_TYPES.map(s => <ChipToggle key={s} label={t(`speciesForm.soil.${s}`)} selected={soils.has(s)} onClick={() => toggle(soils, setSoils, s)} />)}
            </ChipGroup>
          </Section>

          {isEdit && <ProvidersSection species={species} readOnly={readOnly} />}

          {tags.length > 0 && (
            <Section title={t('speciesForm.tags')}>
              <div style={CHIPS}>
                {tags.map(tg => <ChipToggle key={tg.id} label={tg.name} selected={tagIds.has(tg.id)} onClick={() => toggle(tagIds, setTagIds, tg.id)} />)}
              </div>
            </Section>
          )}

          <Section title={t('speciesForm.commercial')}>
            <div style={GRID}>
              <Field label={t('speciesForm.costPerSeedSek')} editable value={costPerSeedSek} onChange={v => setCostPerSeedSek(v.replace(/[^\d.,]/g, ''))} accent="mustard" />
              <Field label={t('speciesForm.expectedStemsPerPlant')} editable value={expectedStemsPerPlant} onChange={digits(setExpectedStemsPerPlant)} accent="sage" />
              <Field label={t('speciesForm.expectedVaseLifeDays')} editable value={expectedVaseLifeDays} onChange={digits(setExpectedVaseLifeDays)} accent="sky" />
              <SelectField label={t('speciesForm.plantType')} value={plantType} onChange={setPlantType}>
                <option value="">—</option>
                {PLANT_TYPES.map(pt => <option key={pt} value={pt}>{t(`speciesForm.plantTypes.${pt}`)}</option>)}
              </SelectField>
              <SelectField label={t('speciesForm.defaultUnitType')} value={defaultUnitType} onChange={setDefaultUnitType}>
                <option value="">—</option>
                {UNIT_TYPES.map(u => <option key={u} value={u}>{t(`speciesForm.unitTypes.${u}`)}</option>)}
              </SelectField>
            </div>
          </Section>

          {!isEdit && (
            <Section title={t('speciesForm.workflow')}>
              <div style={GRID}>
                <SelectField label={t('speciesForm.workflowTemplate')} value={workflowTemplateId} onChange={setWorkflowTemplateId}>
                  <option value="">{t('speciesForm.workflowNone')}</option>
                  {templates.map(tpl => <option key={tpl.id} value={tpl.id}>{tpl.name}</option>)}
                </SelectField>
              </div>
            </Section>
          )}
        </fieldset>

        {isEdit && workflowSection && <Section title={t('speciesForm.workflow')}>{workflowSection}</Section>}

        {error && <p style={{ ...NOTE, color: 'var(--color-error)' }}>{error}</p>}
      </div>

      {!readOnly && (
        <div className="sticky-footer">
          <button type="button" className="btn-secondary" onClick={onCancel}>{t('common.cancel')}</button>
          <button
            type="button"
            className="btn-primary"
            onClick={submit}
            disabled={isSubmitting || (!commonNameSv.trim() && !commonName.trim())}
          >
            {isSubmitting ? t('speciesForm.saving') : submitLabel}
          </button>
        </div>
      )}
    </div>
  )
}

function PhotosSection({ species }: { species: SpeciesResponse }) {
  const { t } = useTranslation()
  const qc = useQueryClient()
  const inputRef = useRef<HTMLInputElement>(null)
  const [deletingId, setDeletingId] = useState<number | null>(null)
  const refresh = () => qc.invalidateQueries({ queryKey: ['species', species.id] })

  const upload = useMutation({
    mutationFn: async (file: File) => api.species.uploadPhoto(species.id, await fileToJpegBase64(file)),
    onSuccess: refresh,
  })
  const remove = useMutation({
    mutationFn: (photoId: number) => api.species.deletePhoto(species.id, photoId),
    onSuccess: () => { setDeletingId(null); refresh() },
  })

  return (
    <Section
      title={`${t('speciesForm.photos')} (${species.photos.length})`}
      action={
        <button type="button" className="btn-secondary" disabled={upload.isPending} onClick={() => inputRef.current?.click()}>
          {upload.isPending ? t('speciesForm.uploading') : t('speciesForm.upload')}
        </button>
      }
    >
      <input
        ref={inputRef}
        type="file"
        accept="image/*"
        style={{ display: 'none' }}
        onChange={e => {
          const file = e.target.files?.[0]
          if (file) upload.mutate(file)
          e.target.value = ''
        }}
      />
      {species.photos.length === 0 ? (
        <p style={NOTE}>{t('speciesForm.noPhotos')}</p>
      ) : (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(120px, 1fr))', gap: 10 }}>
          {species.photos.map(photo => (
            <div key={photo.id} style={{ position: 'relative' }}>
              <img src={photo.imageUrl} alt="" style={{ width: '100%', height: 120, objectFit: 'cover', border: '1px solid var(--color-ink)' }} />
              {deletingId === photo.id ? (
                <div style={{ display: 'flex', gap: 10, marginTop: 4 }}>
                  <button type="button" style={{ ...LINK, color: 'var(--color-error)' }} onClick={() => remove.mutate(photo.id)}>{t('speciesForm.confirm')}</button>
                  <button type="button" style={LINK} onClick={() => setDeletingId(null)}>{t('common.cancel')}</button>
                </div>
              ) : (
                <button type="button" style={{ ...LINK, marginTop: 4 }} onClick={() => setDeletingId(photo.id)}>{t('speciesForm.remove')}</button>
              )}
            </div>
          ))}
        </div>
      )}
      {(upload.error || remove.error) && <p style={{ ...NOTE, color: 'var(--color-error)' }}>{String((upload.error ?? remove.error)?.message)}</p>}
    </Section>
  )
}

function ProvidersSection({ species, readOnly }: { species: SpeciesResponse; readOnly: boolean }) {
  const { t } = useTranslation()
  const qc = useQueryClient()
  const [adding, setAdding] = useState(false)
  const [providerId, setProviderId] = useState('')
  const [productUrl, setProductUrl] = useState('')
  const [removingId, setRemovingId] = useState<number | null>(null)
  const refresh = () => qc.invalidateQueries({ queryKey: ['species', species.id] })

  const { data: providers = [] } = useQuery({ queryKey: ['providers'], queryFn: api.providers.list, enabled: adding })
  const add = useMutation({
    mutationFn: () => api.species.addProvider(species.id, { providerId: Number(providerId), productUrl: productUrl || undefined }),
    onSuccess: () => { setAdding(false); setProviderId(''); setProductUrl(''); refresh() },
  })
  const remove = useMutation({
    mutationFn: (spId: number) => api.species.removeProvider(species.id, spId),
    onSuccess: () => { setRemovingId(null); refresh() },
  })
  const linked = new Set(species.providers.map(sp => sp.providerId))

  return (
    <Section
      title={`${t('speciesForm.seedProviders')} (${species.providers.length})`}
      action={!readOnly && !adding && (
        <button type="button" className="btn-secondary" onClick={() => setAdding(true)}>{t('speciesForm.addProvider')}</button>
      )}
    >
      {species.providers.length === 0 && !adding && <p style={NOTE}>{t('speciesForm.noProviders')}</p>}
      {species.providers.map(sp => (
        <div key={sp.id} style={{ display: 'flex', alignItems: 'baseline', gap: 14, flexWrap: 'wrap', padding: '10px 0', borderBottom: '1px solid color-mix(in srgb, var(--color-ink) 15%, transparent)' }}>
          <span style={{ fontFamily: 'var(--font-display)', fontSize: 18 }}>{sp.providerName}</span>
          <span style={{ ...NOTE, margin: 0 }}>{sp.providerIdentifier}</span>
          <span style={{ flex: 1 }} />
          {sp.productUrl && <a href={sp.productUrl} target="_blank" rel="noopener noreferrer" style={LINK}>{t('speciesForm.productPage')} →</a>}
          {!readOnly && (removingId === sp.id ? (
            <>
              <button type="button" style={{ ...LINK, color: 'var(--color-error)' }} onClick={() => remove.mutate(sp.id)}>{t('speciesForm.confirm')}</button>
              <button type="button" style={LINK} onClick={() => setRemovingId(null)}>{t('common.cancel')}</button>
            </>
          ) : (
            <button type="button" style={LINK} onClick={() => setRemovingId(sp.id)}>{t('speciesForm.remove')}</button>
          ))}
        </div>
      ))}
      {adding && (
        <div style={{ marginTop: 14, padding: 14, border: '1px dashed var(--color-ink)' }}>
          <div style={GRID}>
            <SelectField label={t('speciesForm.provider')} value={providerId} onChange={setProviderId}>
              <option value="">{t('speciesForm.selectProvider')}</option>
              {providers.filter(p => !linked.has(p.id)).map(p => <option key={p.id} value={p.id}>{p.name}</option>)}
            </SelectField>
            {providerId && <Field label={t('speciesForm.productUrl')} editable value={productUrl} onChange={setProductUrl} />}
          </div>
          <div style={{ display: 'flex', gap: 10, marginTop: 14 }}>
            <button type="button" className="btn-primary" disabled={!providerId || add.isPending} onClick={() => add.mutate()}>{t('common.add')}</button>
            <button type="button" className="btn-secondary" onClick={() => { setAdding(false); setProviderId(''); setProductUrl('') }}>{t('common.cancel')}</button>
          </div>
        </div>
      )}
      {(add.error || remove.error) && <p style={{ ...NOTE, color: 'var(--color-error)' }}>{String((add.error ?? remove.error)?.message)}</p>}
    </Section>
  )
}

function Section({ title, action, children }: { title: string; action?: ReactNode; children: ReactNode }) {
  return (
    <section style={{ marginTop: 28 }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 12, marginBottom: 10 }}>
        <span style={LABEL}>{title}</span>
        {action}
      </div>
      <Rule variant="soft" />
      <div style={{ marginTop: 16 }}>{children}</div>
    </section>
  )
}

function SelectField({ label, value, onChange, children }: { label: string; value: string; onChange: (v: string) => void; children: ReactNode }) {
  const id = useId()
  return (
    <div>
      <label htmlFor={id} style={{ ...LABEL, display: 'block', marginBottom: 4 }}>{label}</label>
      <select id={id} className="input" value={value} onChange={e => onChange(e.target.value)} style={{ width: '100%' }}>{children}</select>
    </div>
  )
}

function RangeField({ label, min, max, onMin, onMax }: { label: string; min: string; max: string; onMin: (v: string) => void; onMax: (v: string) => void }) {
  const { t } = useTranslation()
  return (
    <div style={{ display: 'flex', alignItems: 'baseline', columnGap: 12, rowGap: 4, flexWrap: 'wrap' }}>
      <span style={{ ...LABEL, flex: '0 0 170px' }}>{label}</span>
      <span style={{ display: 'inline-flex', alignItems: 'baseline', gap: 12 }}>
        <input className="input" inputMode="numeric" maxLength={4} aria-label={`${label}, ${t('speciesForm.min')}`} placeholder={t('speciesForm.min')} value={min} onChange={e => onMin(e.target.value)} style={{ width: 72 }} />
        <span>–</span>
        <input className="input" inputMode="numeric" maxLength={4} aria-label={`${label}, ${t('speciesForm.max')}`} placeholder={t('speciesForm.max')} value={max} onChange={e => onMax(e.target.value)} style={{ width: 72 }} />
      </span>
    </div>
  )
}

function ChipGroup({ label, children }: { label: string; children: ReactNode }) {
  return (
    <div style={{ marginBottom: 14 }}>
      <span style={{ ...LABEL, display: 'block', marginBottom: 8 }}>{label}</span>
      <div style={CHIPS}>{children}</div>
    </div>
  )
}

function ChipToggle({ label, selected, onClick }: { label: string; selected: boolean; onClick: () => void }) {
  return (
    <button
      type="button"
      aria-pressed={selected}
      onClick={onClick}
      style={{
        fontFamily: 'var(--font-mono)',
        fontSize: 10,
        letterSpacing: 1.4,
        textTransform: 'uppercase',
        padding: '5px 9px',
        border: '1px solid var(--color-ink)',
        borderRadius: 999,
        background: selected ? 'var(--color-ink)' : 'transparent',
        color: selected ? 'var(--color-cream)' : 'var(--color-ink)',
        cursor: 'pointer',
      }}
    >
      {label}
    </button>
  )
}

const MONTHS = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12]
const GRID = { display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', gap: '20px 28px' } as const
const CHIPS = { display: 'flex', flexWrap: 'wrap', gap: 6 } as const
const LABEL = { fontFamily: 'var(--font-mono)', fontSize: 9, letterSpacing: 1.4, textTransform: 'uppercase', color: 'var(--color-forest)' } as const
const NOTE = { fontFamily: 'var(--font-display)', fontStyle: 'italic', fontSize: 14, color: 'var(--color-forest)', margin: '8px 0' } as const
const LINK = { background: 'transparent', border: 'none', padding: 0, fontFamily: 'var(--font-mono)', fontSize: 10, letterSpacing: 1.4, textTransform: 'uppercase', color: 'var(--color-accent)', cursor: 'pointer' } as const
