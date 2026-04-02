import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { api, type PestDiseaseLogResponse, type SpeciesResponse, type BedWithGardenResponse } from '../api/client'
import { PageHeader } from '../components/PageHeader'
import { ErrorDisplay } from '../components/ErrorDisplay'
import { Dialog } from '../components/Dialog'
import { Pagination } from '../components/Pagination'
import { SpeciesAutocomplete } from '../components/SpeciesAutocomplete'

const PAGE_SIZE = 50

const CATEGORIES = ['PEST', 'DISEASE', 'DEFICIENCY', 'OTHER'] as const
const SEVERITIES = ['LOW', 'MODERATE', 'HIGH', 'CRITICAL'] as const
const OUTCOMES = ['RESOLVED', 'ONGOING', 'CROP_LOSS', 'MONITORING'] as const

export function PestDiseaseLog() {
  const qc = useQueryClient()
  const { t } = useTranslation()

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
    setFormSpecies(null) // Species autocomplete requires full object; user can re-select
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
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['pest-disease'] }); setShowAdd(false); resetForm() },
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

  const categoryBadge = (cat: string) => {
    const colors: Record<string, string> = {
      PEST: 'bg-orange-100 text-orange-700',
      DISEASE: 'bg-red-100 text-red-700',
      DEFICIENCY: 'bg-yellow-100 text-yellow-700',
      OTHER: 'bg-gray-100 text-gray-700',
    }
    return (
      <span className={`inline-block px-2 py-0.5 rounded-full text-xs font-medium ${colors[cat] ?? colors.OTHER}`}>
        {t(`pestCategories.${cat}`)}
      </span>
    )
  }

  const severityBadge = (sev: string) => {
    const colors: Record<string, string> = {
      LOW: 'bg-green-100 text-green-700',
      MODERATE: 'bg-yellow-100 text-yellow-700',
      HIGH: 'bg-orange-100 text-orange-700',
      CRITICAL: 'bg-red-100 text-red-700',
    }
    return (
      <span className={`inline-block px-2 py-0.5 rounded-full text-xs font-medium ${colors[sev] ?? 'bg-gray-100 text-gray-700'}`}>
        {t(`severities.${sev}`)}
      </span>
    )
  }

  const outcomeBadge = (outcome: string | undefined) => {
    if (!outcome) return '—'
    const colors: Record<string, string> = {
      RESOLVED: 'bg-green-100 text-green-700',
      ONGOING: 'bg-yellow-100 text-yellow-700',
      CROP_LOSS: 'bg-red-100 text-red-700',
      MONITORING: 'bg-blue-100 text-blue-700',
    }
    return (
      <span className={`inline-block px-2 py-0.5 rounded-full text-xs font-medium ${colors[outcome] ?? 'bg-gray-100 text-gray-700'}`}>
        {t(`outcomes.${outcome}`)}
      </span>
    )
  }

  const bedName = (bedId?: number) => {
    if (!bedId || !beds) return '—'
    const bed = beds.find((b: BedWithGardenResponse) => b.id === bedId)
    return bed ? bed.name : '—'
  }

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
      <PageHeader title={t('pestDisease.title')} action={{ label: t('pestDisease.new'), onClick: openAdd, 'data-onboarding': 'add-pest-btn' }} />
      <div className="px-4 py-4">
        {/* Season filter */}
        <div className="mb-4">
          <select
            value={seasonFilter ?? ''}
            onChange={e => { setSeasonFilter(e.target.value ? Number(e.target.value) : undefined); setPage(0) }}
            className="input w-auto"
          >
            <option value="">{t('pestDisease.allSeasons')}</option>
            {seasons?.map(s => (
              <option key={s.id} value={s.id}>{s.name}</option>
            ))}
          </select>
        </div>

        {data && data.length === 0 && (
          <p className="text-text-secondary text-sm text-center py-4">{t('pestDisease.noEntries')}</p>
        )}

        {data && data.length > 0 && (<>
          <div className="border border-divider rounded-xl overflow-hidden bg-bg shadow-sm">
            <table className="w-full">
              <thead>
                <tr className="border-b border-divider bg-surface">
                  <th className="text-left px-4 py-2 text-xs font-medium text-text-secondary">{t('pestDisease.observedDate')}</th>
                  <th className="text-left px-4 py-2 text-xs font-medium text-text-secondary">{t('pestDisease.name')}</th>
                  <th className="text-left px-4 py-2 text-xs font-medium text-text-secondary">{t('pestDisease.category')}</th>
                  <th className="text-left px-4 py-2 text-xs font-medium text-text-secondary">{t('pestDisease.severity')}</th>
                  <th className="text-left px-4 py-2 text-xs font-medium text-text-secondary">{t('sow.bedLabel')}</th>
                  <th className="text-left px-4 py-2 text-xs font-medium text-text-secondary">{t('pestDisease.outcome')}</th>
                </tr>
              </thead>
              <tbody>
                {data.slice(page * PAGE_SIZE, (page + 1) * PAGE_SIZE).map(entry => (
                  <tr
                    key={entry.id}
                    className="border-b border-divider last:border-0 hover:bg-surface cursor-pointer transition-colors"
                    onClick={() => openEdit(entry)}
                  >
                    <td className="px-4 py-2.5 text-sm tabular-nums">{entry.observedDate}</td>
                    <td className="px-4 py-2.5 text-sm">{entry.name}</td>
                    <td className="px-4 py-2.5 text-sm">{categoryBadge(entry.category)}</td>
                    <td className="px-4 py-2.5 text-sm">{severityBadge(entry.severity)}</td>
                    <td className="px-4 py-2.5 text-sm text-text-secondary">{bedName(entry.bedId)}</td>
                    <td className="px-4 py-2.5 text-sm">{outcomeBadge(entry.outcome)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <Pagination page={page} pageSize={PAGE_SIZE} total={data.length} onPageChange={setPage} />
        </>)}
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
