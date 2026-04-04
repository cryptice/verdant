import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { api, type VarietyTrialResponse, type SpeciesResponse, type BedWithGardenResponse } from '../api/client'
import { PageHeader } from '../components/PageHeader'
import { ErrorDisplay } from '../components/ErrorDisplay'
import { Dialog } from '../components/Dialog'
import { Pagination } from '../components/Pagination'
import { SpeciesAutocomplete } from '../components/SpeciesAutocomplete'
import { OnboardingHint } from '../onboarding/OnboardingHint'
import { useOnboarding } from '../onboarding/OnboardingContext'

const PAGE_SIZE = 50

const VERDICTS = ['KEEP', 'EXPAND', 'REDUCE', 'DROP', 'UNDECIDED'] as const
const RECEPTIONS = ['LOVED', 'LIKED', 'NEUTRAL', 'DISLIKED'] as const

export function VarietyTrials() {
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

  const { data: speciesList } = useQuery({
    queryKey: ['species'],
    queryFn: () => api.species.list(),
  })

  const { data, error, isLoading, refetch } = useQuery({
    queryKey: ['variety-trials', seasonFilter],
    queryFn: () => api.varietyTrials.list(seasonFilter),
  })

  const [page, setPage] = useState(0)
  const [showAdd, setShowAdd] = useState(false)
  const [editItem, setEditItem] = useState<VarietyTrialResponse | null>(null)
  const [deleteItem, setDeleteItem] = useState<VarietyTrialResponse | null>(null)

  // Form state
  const [formSeasonId, setFormSeasonId] = useState<string>('')
  const [formSpecies, setFormSpecies] = useState<SpeciesResponse | null>(null)
  const [formBedId, setFormBedId] = useState<string>('')
  const [formPlantCount, setFormPlantCount] = useState('')
  const [formStemYield, setFormStemYield] = useState('')
  const [formAvgLength, setFormAvgLength] = useState('')
  const [formAvgVaseLife, setFormAvgVaseLife] = useState('')
  const [formQualityScore, setFormQualityScore] = useState('')
  const [formReception, setFormReception] = useState<string>('')
  const [formVerdict, setFormVerdict] = useState<string>('UNDECIDED')
  const [formNotes, setFormNotes] = useState('')

  const [formError, setFormError] = useState<string | null>(null)
  const [deleteError, setDeleteError] = useState<string | null>(null)

  const resetForm = () => {
    setFormSeasonId(''); setFormSpecies(null); setFormBedId('')
    setFormPlantCount(''); setFormStemYield(''); setFormAvgLength('')
    setFormAvgVaseLife(''); setFormQualityScore(''); setFormReception('')
    setFormVerdict('UNDECIDED'); setFormNotes(''); setFormError(null)
  }

  const openAdd = () => { resetForm(); setShowAdd(true) }

  const openEdit = (trial: VarietyTrialResponse) => {
    setFormSeasonId(String(trial.seasonId))
    setFormSpecies(null) // Species autocomplete requires full object; user can re-select
    setFormBedId(trial.bedId ? String(trial.bedId) : '')
    setFormPlantCount(trial.plantCount != null ? String(trial.plantCount) : '')
    setFormStemYield(trial.stemYield != null ? String(trial.stemYield) : '')
    setFormAvgLength(trial.avgStemLengthCm != null ? String(trial.avgStemLengthCm) : '')
    setFormAvgVaseLife(trial.avgVaseLifeDays != null ? String(trial.avgVaseLifeDays) : '')
    setFormQualityScore(trial.qualityScore != null ? String(trial.qualityScore) : '')
    setFormReception(trial.customerReception ?? '')
    setFormVerdict(trial.verdict)
    setFormNotes(trial.notes ?? '')
    setFormError(null)
    setEditItem(trial)
  }

  const buildPayload = () => ({
    seasonId: Number(formSeasonId),
    speciesId: formSpecies?.id ?? editItem?.speciesId,
    bedId: formBedId ? Number(formBedId) : undefined,
    plantCount: formPlantCount ? Number(formPlantCount) : undefined,
    stemYield: formStemYield ? Number(formStemYield) : undefined,
    avgStemLengthCm: formAvgLength ? Number(formAvgLength) : undefined,
    avgVaseLifeDays: formAvgVaseLife ? Number(formAvgVaseLife) : undefined,
    qualityScore: formQualityScore ? Number(formQualityScore) : undefined,
    customerReception: formReception || undefined,
    verdict: formVerdict,
    notes: formNotes || undefined,
  })

  const createMut = useMutation({
    mutationFn: () => api.varietyTrials.create(buildPayload()),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['variety-trials'] }); setShowAdd(false); resetForm(); completeStep('start_trial') },
    onError: (err) => { setFormError(err instanceof Error ? err.message : String(err)) },
  })

  const updateMut = useMutation({
    mutationFn: () => api.varietyTrials.update(editItem!.id, buildPayload()),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['variety-trials'] }); setEditItem(null); resetForm() },
    onError: (err) => { setFormError(err instanceof Error ? err.message : String(err)) },
  })

  const deleteMut = useMutation({
    mutationFn: (id: number) => api.varietyTrials.delete(id),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['variety-trials'] }); setDeleteItem(null); setDeleteError(null) },
    onError: (err) => { setDeleteError(err instanceof Error ? err.message : String(err)) },
  })

  if (isLoading) return <div className="flex justify-center p-16"><div className="animate-spin h-8 w-8 border-2 border-accent border-t-transparent rounded-full" /></div>
  if (error) return <ErrorDisplay error={error} onRetry={refetch} />

  const speciesName = (speciesId: number) => {
    const sp = speciesList?.find(s => s.id === speciesId)
    return sp ? sp.commonName : `#${speciesId}`
  }

  const bedName = (bedId?: number) => {
    if (!bedId || !beds) return '—'
    const bed = beds.find((b: BedWithGardenResponse) => b.id === bedId)
    return bed ? bed.name : '—'
  }

  const verdictBadge = (verdict: string) => {
    const colors: Record<string, string> = {
      KEEP: 'bg-green-100 text-green-700',
      EXPAND: 'bg-blue-100 text-blue-700',
      REDUCE: 'bg-yellow-100 text-yellow-700',
      DROP: 'bg-red-100 text-red-700',
      UNDECIDED: 'bg-gray-100 text-gray-700',
    }
    return (
      <span className={`inline-block px-2 py-0.5 rounded-full text-xs font-medium ${colors[verdict] ?? colors.UNDECIDED}`}>
        {t(`verdicts.${verdict}`)}
      </span>
    )
  }

  const receptionBadge = (reception: string | undefined) => {
    if (!reception) return '—'
    const colors: Record<string, string> = {
      LOVED: 'bg-pink-100 text-pink-700',
      LIKED: 'bg-green-100 text-green-700',
      NEUTRAL: 'bg-gray-100 text-gray-700',
      DISLIKED: 'bg-red-100 text-red-700',
    }
    return (
      <span className={`inline-block px-2 py-0.5 rounded-full text-xs font-medium ${colors[reception] ?? 'bg-gray-100 text-gray-700'}`}>
        {t(`receptions.${reception}`)}
      </span>
    )
  }

  const canSubmitCreate = formSeasonId && formSpecies
  const canSubmitEdit = formSeasonId && (formSpecies || editItem?.speciesId)

  const formFields = (
    <div className="space-y-4">
      <div>
        <label className="field-label">{t('seasons.title')} *</label>
        <select value={formSeasonId} onChange={e => setFormSeasonId(e.target.value)} className="input">
          <option value="">—</option>
          {seasons?.map(s => (
            <option key={s.id} value={s.id}>{s.name}</option>
          ))}
        </select>
      </div>
      <div>
        <label className="field-label">{t('common.speciesLabel')}</label>
        <SpeciesAutocomplete value={formSpecies} onChange={setFormSpecies} />
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
        <label className="field-label">{t('trials.plantCount')}</label>
        <input type="number" value={formPlantCount} onChange={e => setFormPlantCount(e.target.value)} className="input" />
      </div>
      <div>
        <label className="field-label">{t('trials.stemYield')}</label>
        <input type="number" value={formStemYield} onChange={e => setFormStemYield(e.target.value)} className="input" />
      </div>
      <div>
        <label className="field-label">{t('trials.avgLength')}</label>
        <input type="number" value={formAvgLength} onChange={e => setFormAvgLength(e.target.value)} className="input" />
      </div>
      <div>
        <label className="field-label">{t('trials.avgVaseLife')}</label>
        <input type="number" value={formAvgVaseLife} onChange={e => setFormAvgVaseLife(e.target.value)} className="input" />
      </div>
      <div>
        <label className="field-label">{t('trials.qualityScore')} (1-10)</label>
        <input type="number" min="1" max="10" value={formQualityScore} onChange={e => setFormQualityScore(e.target.value)} className="input" />
      </div>
      <div>
        <label className="field-label">{t('trials.reception')}</label>
        <select value={formReception} onChange={e => setFormReception(e.target.value)} className="input">
          <option value="">—</option>
          {RECEPTIONS.map(r => (
            <option key={r} value={r}>{t(`receptions.${r}`)}</option>
          ))}
        </select>
      </div>
      <div>
        <label className="field-label">{t('trials.verdict')} *</label>
        <select value={formVerdict} onChange={e => setFormVerdict(e.target.value)} className="input">
          {VERDICTS.map(v => (
            <option key={v} value={v}>{t(`verdicts.${v}`)}</option>
          ))}
        </select>
      </div>
      <div>
        <label className="field-label">{t('common.notesLabel')}</label>
        <textarea value={formNotes} onChange={e => setFormNotes(e.target.value)} placeholder={t('common.optional')} rows={2} className="input" />
      </div>
      {formError && <p className="text-error text-sm">{formError}</p>}
    </div>
  )

  return (
    <div>
      <PageHeader title={t('trials.title')} action={{ label: t('trials.new'), onClick: openAdd, 'data-onboarding': 'add-trial-btn' }} />
      <OnboardingHint />
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
          <p className="text-text-secondary text-sm text-center py-4">{t('trials.noTrials')}</p>
        )}

        {data && data.length > 0 && (<>
          <div className="border border-divider rounded-xl overflow-hidden bg-bg shadow-sm">
            <table className="w-full">
              <thead>
                <tr className="border-b border-divider bg-surface">
                  <th className="text-left px-4 py-2 text-xs font-medium text-text-secondary">{t('common.speciesLabel')}</th>
                  <th className="text-left px-4 py-2 text-xs font-medium text-text-secondary">{t('sow.bedLabel')}</th>
                  <th className="text-right px-4 py-2 text-xs font-medium text-text-secondary">{t('trials.plantCount')}</th>
                  <th className="text-right px-4 py-2 text-xs font-medium text-text-secondary">{t('trials.stemYield')}</th>
                  <th className="text-right px-4 py-2 text-xs font-medium text-text-secondary">{t('trials.avgLength')}</th>
                  <th className="text-right px-4 py-2 text-xs font-medium text-text-secondary">{t('trials.avgVaseLife')}</th>
                  <th className="text-right px-4 py-2 text-xs font-medium text-text-secondary">{t('trials.qualityScore')}</th>
                  <th className="text-left px-4 py-2 text-xs font-medium text-text-secondary">{t('trials.reception')}</th>
                  <th className="text-left px-4 py-2 text-xs font-medium text-text-secondary">{t('trials.verdict')}</th>
                </tr>
              </thead>
              <tbody>
                {data.slice(page * PAGE_SIZE, (page + 1) * PAGE_SIZE).map(trial => (
                  <tr
                    key={trial.id}
                    className="border-b border-divider last:border-0 hover:bg-surface cursor-pointer transition-colors"
                    onClick={() => openEdit(trial)}
                  >
                    <td className="px-4 py-2.5 text-sm">{speciesName(trial.speciesId)}</td>
                    <td className="px-4 py-2.5 text-sm text-text-secondary">{bedName(trial.bedId)}</td>
                    <td className="px-4 py-2.5 text-sm text-right tabular-nums">{trial.plantCount ?? '—'}</td>
                    <td className="px-4 py-2.5 text-sm text-right tabular-nums">{trial.stemYield ?? '—'}</td>
                    <td className="px-4 py-2.5 text-sm text-right tabular-nums">{trial.avgStemLengthCm != null ? `${trial.avgStemLengthCm} cm` : '—'}</td>
                    <td className="px-4 py-2.5 text-sm text-right tabular-nums">{trial.avgVaseLifeDays != null ? `${trial.avgVaseLifeDays} d` : '—'}</td>
                    <td className="px-4 py-2.5 text-sm text-right tabular-nums">{trial.qualityScore != null ? `${trial.qualityScore}/10` : '—'}</td>
                    <td className="px-4 py-2.5 text-sm">{receptionBadge(trial.customerReception)}</td>
                    <td className="px-4 py-2.5 text-sm">{verdictBadge(trial.verdict)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <Pagination page={page} pageSize={PAGE_SIZE} total={data.length} onPageChange={setPage} />
        </>)}
      </div>

      <Dialog open={showAdd} onClose={() => { setShowAdd(false); resetForm() }} title={t('trials.new')} actions={
        <>
          <button onClick={() => { setShowAdd(false); resetForm() }} className="px-4 py-2 text-sm text-text-secondary">{t('common.cancel')}</button>
          <button
            onClick={() => createMut.mutate()}
            disabled={!canSubmitCreate || createMut.isPending}
            className="btn-primary text-sm"
          >
            {createMut.isPending ? t('common.saving') : t('common.add')}
          </button>
        </>
      }>
        {formFields}
      </Dialog>

      <Dialog open={editItem !== null} onClose={() => { setEditItem(null); resetForm() }} title={t('trials.edit')} actions={
        <>
          <button onClick={() => { setEditItem(null); resetForm() }} className="px-4 py-2 text-sm text-text-secondary">{t('common.cancel')}</button>
          <button
            onClick={() => updateMut.mutate()}
            disabled={!canSubmitEdit || updateMut.isPending}
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
          {t('common.delete')}
        </button>
      </Dialog>

      <Dialog open={deleteItem !== null} onClose={() => { setDeleteItem(null); setDeleteError(null) }} title={t('common.delete')} actions={
        <>
          <button onClick={() => { setDeleteItem(null); setDeleteError(null) }} className="px-4 py-2 text-sm text-text-secondary">{t('common.cancel')}</button>
          <button onClick={() => deleteItem && deleteMut.mutate(deleteItem.id)} className="px-4 py-2 text-sm text-error font-semibold">{t('common.delete')}</button>
        </>
      }>
        <p className="text-text-secondary">{t('trials.noTrials')}</p>
        {deleteError && <p className="text-error text-sm mt-2">{deleteError}</p>}
      </Dialog>
    </div>
  )
}
