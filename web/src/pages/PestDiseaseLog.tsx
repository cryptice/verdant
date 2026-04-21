import React from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useState, useEffect } from 'react'
import { useTranslation } from 'react-i18next'
import { api, type PestDiseaseLogResponse, type SpeciesResponse, type BedWithGardenResponse } from '../api/client'
import { Masthead, Chip, LedgerPagination } from '../components/faltet'
import { ErrorDisplay } from '../components/ErrorDisplay'
import { Dialog } from '../components/Dialog'
import { SpeciesAutocomplete } from '../components/SpeciesAutocomplete'
import { OnboardingHint } from '../onboarding/OnboardingHint'
import { useOnboarding } from '../onboarding/OnboardingContext'

const CATEGORIES = ['PEST', 'DISEASE', 'DEFICIENCY', 'OTHER'] as const
const SEVERITIES = ['LOW', 'MODERATE', 'HIGH', 'CRITICAL'] as const
const OUTCOMES = ['RESOLVED', 'ONGOING', 'CROP_LOSS', 'MONITORING'] as const

const TEMPLATE = '60px 1.5fr 110px 130px 120px 90px 36px'

const CATEGORY_COLOR: Record<string, string> = {
  PEST: 'var(--color-berry)',
  DISEASE: 'var(--color-clay)',
  DEFICIENCY: 'var(--color-mustard)',
  OTHER: 'var(--color-forest)',
}

const SEVERITY_TONE: Record<string, 'sage' | 'mustard' | 'clay' | 'berry'> = {
  LOW: 'sage',
  MODERATE: 'mustard',
  HIGH: 'clay',
  CRITICAL: 'berry',
}

const OUTCOME_TONE: Record<string, 'sage' | 'mustard' | 'berry' | 'sky'> = {
  RESOLVED: 'sage',
  ONGOING: 'mustard',
  CROP_LOSS: 'berry',
  MONITORING: 'sky',
}

const HEADER_STYLE: React.CSSProperties = {
  fontFamily: 'var(--font-mono)',
  fontSize: 9,
  letterSpacing: 1.4,
  textTransform: 'uppercase',
  color: 'var(--color-forest)',
  opacity: 0.7,
}

export function PestDiseaseLog() {
  const qc = useQueryClient()
  const { t } = useTranslation()
  const { completeStep } = useOnboarding()

  const [seasonFilter, setSeasonFilter] = useState<number | undefined>(undefined)

  const { data: seasons } = useQuery({
    queryKey: ['seasons'],
    queryFn: () => api.seasons.list(),
  })

  const { data: beds } = useQuery({
    queryKey: ['beds'],
    queryFn: () => api.beds.list(),
  })

  const { data, error, isLoading, refetch } = useQuery({
    queryKey: ['pest-disease', seasonFilter],
    queryFn: () => api.pestDisease.list(seasonFilter),
  })

  const [page, setPage] = useState(0)
  const pageSize = 50
  useEffect(() => { setPage(0) }, [seasonFilter])

  const [showAdd, setShowAdd] = useState(false)
  const [editItem, setEditItem] = useState<PestDiseaseLogResponse | null>(null)
  const [deleteItem, setDeleteItem] = useState<PestDiseaseLogResponse | null>(null)

  // Form state
  const [formObservedDate, setFormObservedDate] = useState('')
  const [formCategory, setFormCategory] = useState<string>('PEST')
  const [formName, setFormName] = useState('')
  const [formSeverity, setFormSeverity] = useState<string>('LOW')
  const [formBedId, setFormBedId] = useState<string>('')
  const [formSpecies, setFormSpecies] = useState<SpeciesResponse | null>(null)
  const [formTreatment, setFormTreatment] = useState('')
  const [formOutcome, setFormOutcome] = useState<string>('')
  const [formNotes, setFormNotes] = useState('')
  const [formSeasonId, setFormSeasonId] = useState<string>('')

  const [formError, setFormError] = useState<string | null>(null)
  const [deleteError, setDeleteError] = useState<string | null>(null)

  const resetForm = () => {
    setFormObservedDate(''); setFormCategory('PEST'); setFormName(''); setFormSeverity('LOW')
    setFormBedId(''); setFormSpecies(null); setFormTreatment(''); setFormOutcome('')
    setFormNotes(''); setFormSeasonId(''); setFormError(null)
  }

  const openAdd = () => { resetForm(); setShowAdd(true) }

  const openEdit = (entry: PestDiseaseLogResponse) => {
    setFormObservedDate(entry.observedDate)
    setFormCategory(entry.category)
    setFormName(entry.name)
    setFormSeverity(entry.severity)
    setFormBedId(entry.bedId ? String(entry.bedId) : '')
    setFormSpecies(null)
    setFormTreatment(entry.treatment ?? '')
    setFormOutcome(entry.outcome ?? '')
    setFormNotes(entry.notes ?? '')
    setFormSeasonId(entry.seasonId ? String(entry.seasonId) : '')
    setFormError(null)
    setEditItem(entry)
  }

  const buildPayload = () => ({
    observedDate: formObservedDate,
    category: formCategory,
    name: formName,
    severity: formSeverity,
    bedId: formBedId ? Number(formBedId) : undefined,
    speciesId: formSpecies?.id ?? undefined,
    treatment: formTreatment || undefined,
    outcome: formOutcome || undefined,
    notes: formNotes || undefined,
    seasonId: formSeasonId ? Number(formSeasonId) : undefined,
  })

  const createMut = useMutation({
    mutationFn: () => api.pestDisease.create(buildPayload()),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['pest-disease'] }); setShowAdd(false); resetForm(); completeStep('log_pest') },
    onError: (err) => { setFormError(err instanceof Error ? err.message : String(err)) },
  })

  const updateMut = useMutation({
    mutationFn: () => api.pestDisease.update(editItem!.id, buildPayload()),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['pest-disease'] }); setEditItem(null); resetForm() },
    onError: (err) => { setFormError(err instanceof Error ? err.message : String(err)) },
  })

  const deleteMut = useMutation({
    mutationFn: (id: number) => api.pestDisease.delete(id),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['pest-disease'] }); setDeleteItem(null); setDeleteError(null) },
    onError: (err) => { setDeleteError(err instanceof Error ? err.message : String(err)) },
  })

  if (isLoading) return <div className="flex justify-center p-16"><div className="animate-spin h-8 w-8 border-2 border-accent border-t-transparent rounded-full" /></div>
  if (error) return <ErrorDisplay error={error} onRetry={refetch} />

  const bedName = (bedId?: number) => {
    if (!bedId || !beds) return '—'
    const bed = beds.find((b: BedWithGardenResponse) => b.id === bedId)
    return bed ? bed.name : '—'
  }

  const sortedLogs = (data ?? []).slice().sort((a, b) =>
    CATEGORIES.indexOf(a.category as typeof CATEGORIES[number]) - CATEGORIES.indexOf(b.category as typeof CATEGORIES[number]) ||
    a.observedDate.localeCompare(b.observedDate)
  )

  const formFields = (
    <div className="space-y-4">
      <div>
        <label className="field-label">{t('pestDisease.observedDate')} *</label>
        <input type="date" value={formObservedDate} onChange={e => setFormObservedDate(e.target.value)} className="input" />
      </div>
      <div>
        <label className="field-label">{t('pestDisease.category')} *</label>
        <select value={formCategory} onChange={e => setFormCategory(e.target.value)} className="input">
          {CATEGORIES.map(c => (
            <option key={c} value={c}>{t(`pestCategories.${c}`)}</option>
          ))}
        </select>
      </div>
      <div>
        <label className="field-label">{t('pestDisease.name')} *</label>
        <input type="text" value={formName} onChange={e => setFormName(e.target.value)} className="input" />
      </div>
      <div>
        <label className="field-label">{t('pestDisease.severity')} *</label>
        <select value={formSeverity} onChange={e => setFormSeverity(e.target.value)} className="input">
          {SEVERITIES.map(s => (
            <option key={s} value={s}>{t(`severities.${s}`)}</option>
          ))}
        </select>
      </div>
      <div>
        <label className="field-label">{t('sow.bedLabel')}</label>
        <select value={formBedId} onChange={e => setFormBedId(e.target.value)} className="input">
          <option value="">—</option>
          {beds?.map((b: BedWithGardenResponse) => (
            <option key={b.id} value={b.id}>{b.gardenName} / {b.name}</option>
          ))}
        </select>
      </div>
      <div>
        <label className="field-label">{t('common.speciesLabel')}</label>
        <SpeciesAutocomplete value={formSpecies} onChange={setFormSpecies} />
      </div>
      <div>
        <label className="field-label">{t('pestDisease.treatment')}</label>
        <textarea value={formTreatment} onChange={e => setFormTreatment(e.target.value)} placeholder={t('common.optional')} rows={2} className="input" />
      </div>
      <div>
        <label className="field-label">{t('pestDisease.outcome')}</label>
        <select value={formOutcome} onChange={e => setFormOutcome(e.target.value)} className="input">
          <option value="">—</option>
          {OUTCOMES.map(o => (
            <option key={o} value={o}>{t(`outcomes.${o}`)}</option>
          ))}
        </select>
      </div>
      <div>
        <label className="field-label">{t('common.notesLabel')}</label>
        <textarea value={formNotes} onChange={e => setFormNotes(e.target.value)} placeholder={t('common.optional')} rows={2} className="input" />
      </div>
      <div>
        <label className="field-label">{t('seasons.title')}</label>
        <select value={formSeasonId} onChange={e => setFormSeasonId(e.target.value)} className="input">
          <option value="">—</option>
          {seasons?.map(s => (
            <option key={s.id} value={s.id}>{s.name}</option>
          ))}
        </select>
      </div>
      {formError && <p className="text-error text-sm">{formError}</p>}
    </div>
  )

  return (
    <div>
      <Masthead
        left={t('nav.pestDisease')}
        center="— Skadeliggaren —"
        right={
          <button onClick={openAdd} className="btn-primary" data-onboarding="add-pest-btn">
            {t('pestDisease.new')}
          </button>
        }
      />
      <OnboardingHint />
      <div style={{ padding: '28px 40px' }}>
        {/* Season filter */}
        <div style={{ marginBottom: 22 }}>
          <select
            value={seasonFilter ?? ''}
            onChange={e => setSeasonFilter(e.target.value ? Number(e.target.value) : undefined)}
            className="input w-auto"
          >
            <option value="">{t('pestDisease.allSeasons')}</option>
            {seasons?.map(s => (
              <option key={s.id} value={s.id}>{s.name}</option>
            ))}
          </select>
        </div>

        {sortedLogs.length === 0 && (
          <div style={{ padding: '40px 22px', textAlign: 'center', borderBottom: '1px solid var(--color-ink)', borderTop: '1px solid var(--color-ink)' }}>
            <div style={{ ...HEADER_STYLE, marginBottom: 6 }}>{t('pestDisease.noEntries')}</div>
          </div>
        )}

        {sortedLogs.length > 0 && (
          <div>
            {/* Header */}
            <div style={{ display: 'grid', gridTemplateColumns: TEMPLATE, gap: 18, padding: '10px 0', borderBottom: '1px solid var(--color-ink)', ...HEADER_STYLE, opacity: 1 }}>
              <span style={HEADER_STYLE}>№</span>
              <span style={HEADER_STYLE}>{t('pestDisease.name')}</span>
              <span style={HEADER_STYLE}>{t('pestDisease.severity')}</span>
              <span style={HEADER_STYLE}>{t('pestDisease.outcome')}</span>
              <span style={HEADER_STYLE}>{t('sow.bedLabel')}</span>
              <span style={HEADER_STYLE}>{t('pestDisease.observedDate')}</span>
              <span />
            </div>

            {/* Body with category section headers */}
            {(() => {
              const visible = sortedLogs.slice(page * pageSize, (page + 1) * pageSize)
              let prevCategory: string | null = null
              return visible.map((log, i) => {
                const globalIndex = page * pageSize + i
                const needsHeader = log.category !== prevCategory
                prevCategory = log.category
                return (
                  <React.Fragment key={log.id}>
                    {needsHeader && (
                      <div style={{
                        ...HEADER_STYLE,
                        opacity: 1,
                        padding: '16px 0 6px',
                        color: CATEGORY_COLOR[log.category] ?? 'var(--color-forest)',
                      }}>
                        § {t(`pestCategories.${log.category}`)}
                      </div>
                    )}
                    <button
                      onClick={() => openEdit(log)}
                      style={{
                        display: 'grid',
                        gridTemplateColumns: TEMPLATE,
                        gap: 18,
                        padding: '14px 0',
                        borderBottom: '1px solid color-mix(in srgb, var(--color-ink) 20%, transparent)',
                        width: '100%',
                        background: 'transparent',
                        border: 'none',
                        borderBottomWidth: 1,
                        borderBottomStyle: 'solid',
                        borderBottomColor: 'color-mix(in srgb, var(--color-ink) 20%, transparent)',
                        textAlign: 'left',
                        cursor: 'pointer',
                        alignItems: 'center',
                      }}
                      className="ledger-row"
                    >
                      <span style={{
                        fontFamily: 'var(--font-display)',
                        fontStyle: 'italic',
                        fontSize: 22,
                        color: CATEGORY_COLOR[log.category] ?? 'var(--color-forest)',
                      }}>
                        {String(globalIndex + 1).padStart(2, '0')}
                      </span>
                      <span style={{ fontFamily: 'var(--font-display)', fontSize: 18 }}>{log.name}</span>
                      <Chip tone={SEVERITY_TONE[log.severity] ?? 'sage'}>
                        {t(`severities.${log.severity}`)}
                      </Chip>
                      {log.outcome
                        ? <Chip tone={OUTCOME_TONE[log.outcome] ?? 'sage'}>{t(`outcomes.${log.outcome}`)}</Chip>
                        : <span style={{ fontFamily: 'var(--font-mono)', fontSize: 10 }}>—</span>
                      }
                      <span style={{ fontFamily: 'var(--font-mono)', fontSize: 10 }}>{bedName(log.bedId)}</span>
                      <span style={{ fontFamily: 'var(--font-mono)', fontSize: 10 }}>{log.observedDate}</span>
                      <span style={{ fontFamily: 'var(--font-mono)', color: 'var(--color-clay)' }}>→</span>
                    </button>
                  </React.Fragment>
                )
              })
            })()}
            <LedgerPagination page={page} pageSize={pageSize} total={sortedLogs.length} onChange={setPage} />
          </div>
        )}
      </div>

      <Dialog open={showAdd} onClose={() => { setShowAdd(false); resetForm() }} title={t('pestDisease.new')} actions={
        <>
          <button onClick={() => { setShowAdd(false); resetForm() }} className="px-4 py-2 text-sm text-text-secondary">{t('common.cancel')}</button>
          <button
            onClick={() => createMut.mutate()}
            disabled={!formName || !formObservedDate || createMut.isPending}
            className="btn-primary text-sm"
          >
            {createMut.isPending ? t('common.saving') : t('common.add')}
          </button>
        </>
      }>
        {formFields}
      </Dialog>

      <Dialog open={editItem !== null} onClose={() => { setEditItem(null); resetForm() }} title={t('pestDisease.edit')} actions={
        <>
          <button onClick={() => { setEditItem(null); resetForm() }} className="px-4 py-2 text-sm text-text-secondary">{t('common.cancel')}</button>
          <button
            onClick={() => updateMut.mutate()}
            disabled={!formName || !formObservedDate || updateMut.isPending}
            className="btn-primary text-sm"
          >
            {updateMut.isPending ? t('common.saving') : t('common.save')}
          </button>
        </>
      }>
        {formFields}
        <button
          onClick={() => { setEditItem(null); resetForm(); setDeleteItem(editItem) }}
          className="text-sm text-error hover:underline mt-4"
        >
          {t('pestDisease.deleteEntry')}
        </button>
      </Dialog>

      <Dialog open={deleteItem !== null} onClose={() => { setDeleteItem(null); setDeleteError(null) }} title={t('pestDisease.deleteEntry')} actions={
        <>
          <button onClick={() => { setDeleteItem(null); setDeleteError(null) }} className="px-4 py-2 text-sm text-text-secondary">{t('common.cancel')}</button>
          <button onClick={() => deleteItem && deleteMut.mutate(deleteItem.id)} className="px-4 py-2 text-sm text-error font-semibold">{t('common.delete')}</button>
        </>
      }>
        <p className="text-text-secondary">{t('pestDisease.deleteConfirm')}</p>
        {deleteError && <p className="text-error text-sm mt-2">{deleteError}</p>}
      </Dialog>
    </div>
  )
}
