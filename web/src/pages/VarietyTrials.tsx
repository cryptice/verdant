import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useState, useEffect } from 'react'
import { useTranslation } from 'react-i18next'
import { api, type VarietyTrialResponse, type SpeciesResponse, type BedWithGardenResponse } from '../api/client'
import { Masthead, Chip, LedgerPagination } from '../components/faltet'
import { ErrorDisplay } from '../components/ErrorDisplay'
import { Dialog } from '../components/Dialog'
import { SpeciesAutocomplete } from '../components/SpeciesAutocomplete'
import { OnboardingHint } from '../onboarding/OnboardingHint'
import { useOnboarding } from '../onboarding/OnboardingContext'

const VERDICTS = ['KEEP', 'EXPAND', 'REDUCE', 'DROP', 'UNDECIDED'] as const
const RECEPTIONS = ['LOVED', 'LIKED', 'NEUTRAL', 'DISLIKED'] as const

const VERDICT_TONE: Record<string, 'sage' | 'mustard' | 'clay' | 'forest'> = {
  KEEP: 'sage',
  EXPAND: 'sage',
  REDUCE: 'mustard',
  DROP: 'clay',
  UNDECIDED: 'forest',
}

const RECEPTION_TONE: Record<string, 'sage' | 'sky' | 'forest' | 'berry'> = {
  LOVED: 'sage',
  LIKED: 'sky',
  NEUTRAL: 'forest',
  DISLIKED: 'berry',
}

export function VarietyTrials() {
  const qc = useQueryClient()
  const { t: tr } = useTranslation()
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
  const pageSize = 50
  useEffect(() => { setPage(0) }, [seasonFilter])

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
    setFormSpecies(null)
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

  const resolveSpecies = (speciesId: number) => speciesList?.find(s => s.id === speciesId)

  const bedName = (bedId?: number) => {
    if (!bedId || !beds) return '—'
    const bed = beds.find((b: BedWithGardenResponse) => b.id === bedId)
    return bed ? bed.name : '—'
  }

  const trials = data ?? []

  const canSubmitCreate = formSeasonId && formSpecies
  const canSubmitEdit = formSeasonId && (formSpecies || editItem?.speciesId)

  const formFields = (
    <div className="space-y-4">
      <div>
        <label className="field-label">{tr('seasons.title')} *</label>
        <select value={formSeasonId} onChange={e => setFormSeasonId(e.target.value)} className="input">
          <option value="">—</option>
          {seasons?.map(s => (
            <option key={s.id} value={s.id}>{s.name}</option>
          ))}
        </select>
      </div>
      <div>
        <label className="field-label">{tr('common.speciesLabel')}</label>
        <SpeciesAutocomplete value={formSpecies} onChange={setFormSpecies} />
      </div>
      <div>
        <label className="field-label">{tr('sow.bedLabel')}</label>
        <select value={formBedId} onChange={e => setFormBedId(e.target.value)} className="input">
          <option value="">—</option>
          {beds?.map((b: BedWithGardenResponse) => (
            <option key={b.id} value={b.id}>{b.gardenName} / {b.name}</option>
          ))}
        </select>
      </div>
      <div>
        <label className="field-label">{tr('trials.plantCount')}</label>
        <input type="number" value={formPlantCount} onChange={e => setFormPlantCount(e.target.value)} className="input" />
      </div>
      <div>
        <label className="field-label">{tr('trials.stemYield')}</label>
        <input type="number" value={formStemYield} onChange={e => setFormStemYield(e.target.value)} className="input" />
      </div>
      <div>
        <label className="field-label">{tr('trials.avgLength')}</label>
        <input type="number" value={formAvgLength} onChange={e => setFormAvgLength(e.target.value)} className="input" />
      </div>
      <div>
        <label className="field-label">{tr('trials.avgVaseLife')}</label>
        <input type="number" value={formAvgVaseLife} onChange={e => setFormAvgVaseLife(e.target.value)} className="input" />
      </div>
      <div>
        <label className="field-label">{tr('trials.qualityScore')} (1-10)</label>
        <input type="number" min="1" max="10" value={formQualityScore} onChange={e => setFormQualityScore(e.target.value)} className="input" />
      </div>
      <div>
        <label className="field-label">{tr('trials.reception')}</label>
        <select value={formReception} onChange={e => setFormReception(e.target.value)} className="input">
          <option value="">—</option>
          {RECEPTIONS.map(r => (
            <option key={r} value={r}>{tr(`receptions.${r}`)}</option>
          ))}
        </select>
      </div>
      <div>
        <label className="field-label">{tr('trials.verdict')} *</label>
        <select value={formVerdict} onChange={e => setFormVerdict(e.target.value)} className="input">
          {VERDICTS.map(v => (
            <option key={v} value={v}>{tr(`verdicts.${v}`)}</option>
          ))}
        </select>
      </div>
      <div>
        <label className="field-label">{tr('common.notesLabel')}</label>
        <textarea value={formNotes} onChange={e => setFormNotes(e.target.value)} placeholder={tr('common.optional')} rows={2} className="input" />
      </div>
      {formError && <p className="text-error text-sm">{formError}</p>}
    </div>
  )

  return (
    <div>
      <Masthead
        left={tr('nav.trials')}
        center="— Sorteringsförsöksliggaren —"
        right={
          <button onClick={openAdd} className="btn-primary" data-onboarding="add-trial-btn">
            {tr('trials.new')}
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
            <option value="">{tr('pestDisease.allSeasons')}</option>
            {seasons?.map(s => (
              <option key={s.id} value={s.id}>{s.name}</option>
            ))}
          </select>
        </div>

        {trials.length === 0 && (
          <div className="empty-state">
            <div style={{ fontFamily: 'var(--font-mono)', fontSize: 9, letterSpacing: 1.4, textTransform: 'uppercase', color: 'var(--color-forest)', opacity: 0.7, marginBottom: 6 }}>
              {tr('trials.noTrials')}
            </div>
          </div>
        )}

        {trials.length > 0 && (
          <div>
            {trials.slice(page * pageSize, (page + 1) * pageSize).map((trial, i) => {
              const globalIndex = page * pageSize + i
              const sp = resolveSpecies(trial.speciesId)
              const speciesDisplayName = sp?.commonNameSv ?? sp?.commonName ?? `#${trial.speciesId}`
              const variantDisplayName = sp?.variantNameSv ?? sp?.variantName

              return (
                <button
                  key={trial.id}
                  onClick={() => openEdit(trial)}
                  style={{
                    display: 'grid',
                    gridTemplateColumns: '60px 1fr 80px 200px 40px',
                    gap: 22,
                    padding: '20px 0',
                    borderBottom: '1px solid color-mix(in srgb, var(--color-ink) 20%, transparent)',
                    background: 'transparent',
                    border: 'none',
                    borderBottomWidth: 1,
                    borderBottomStyle: 'solid',
                    borderBottomColor: 'color-mix(in srgb, var(--color-ink) 20%, transparent)',
                    textAlign: 'left',
                    cursor: 'pointer',
                    alignItems: 'center',
                    width: '100%',
                  }}
                  className="ledger-row"
                >
                  <span style={{ fontFamily: 'var(--font-display)', fontStyle: 'italic', fontSize: 26, color: 'var(--color-accent)' }}>
                    {String(globalIndex + 1).padStart(2, '0')}
                  </span>
                  <div>
                    <div style={{ fontFamily: 'var(--font-display)', fontSize: 26, fontWeight: 300 }}>
                      {speciesDisplayName}
                      {variantDisplayName && (
                        <span style={{ fontStyle: 'italic', color: 'var(--color-accent)' }}> '{variantDisplayName}'</span>
                      )}
                    </div>
                    <div style={{ fontFamily: 'var(--font-mono)', fontSize: 10, letterSpacing: 1.4, textTransform: 'uppercase', color: 'var(--color-forest)', opacity: 0.7, marginTop: 4 }}>
                      {bedName(trial.bedId)} · {trial.notes?.slice(0, 60) ?? ''}
                    </div>
                  </div>
                  <div style={{ fontFamily: 'var(--font-display)', fontSize: 32, fontWeight: 300, textAlign: 'center' }}>
                    {trial.qualityScore ?? '—'}
                  </div>
                  <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap' }}>
                    {trial.verdict && (
                      <Chip tone={VERDICT_TONE[trial.verdict] ?? 'forest'}>
                        {tr(`verdicts.${trial.verdict}`)}
                      </Chip>
                    )}
                    {trial.customerReception && (
                      <Chip tone={RECEPTION_TONE[trial.customerReception] ?? 'forest'}>
                        {tr(`receptions.${trial.customerReception}`)}
                      </Chip>
                    )}
                  </div>
                  <span style={{ fontFamily: 'var(--font-mono)', color: 'var(--color-accent)' }}>→</span>
                </button>
              )
            })}
            <LedgerPagination page={page} pageSize={pageSize} total={trials.length} onChange={setPage} />
          </div>
        )}
      </div>

      <Dialog open={showAdd} onClose={() => { setShowAdd(false); resetForm() }} title={tr('trials.new')} actions={
        <>
          <button onClick={() => { setShowAdd(false); resetForm() }} className="px-4 py-2 text-sm text-text-secondary">{tr('common.cancel')}</button>
          <button
            onClick={() => createMut.mutate()}
            disabled={!canSubmitCreate || createMut.isPending}
            className="btn-primary text-sm"
          >
            {createMut.isPending ? tr('common.saving') : tr('common.add')}
          </button>
        </>
      }>
        {formFields}
      </Dialog>

      <Dialog open={editItem !== null} onClose={() => { setEditItem(null); resetForm() }} title={tr('trials.edit')} actions={
        <>
          <button onClick={() => { setEditItem(null); resetForm() }} className="px-4 py-2 text-sm text-text-secondary">{tr('common.cancel')}</button>
          <button
            onClick={() => updateMut.mutate()}
            disabled={!canSubmitEdit || updateMut.isPending}
            className="btn-primary text-sm"
          >
            {updateMut.isPending ? tr('common.saving') : tr('common.save')}
          </button>
        </>
      }>
        {formFields}
        <button
          onClick={() => { setEditItem(null); resetForm(); setDeleteItem(editItem) }}
          className="text-sm text-error hover:underline mt-4"
        >
          {tr('common.delete')}
        </button>
      </Dialog>

      <Dialog open={deleteItem !== null} onClose={() => { setDeleteItem(null); setDeleteError(null) }} title={tr('common.delete')} actions={
        <>
          <button onClick={() => { setDeleteItem(null); setDeleteError(null) }} className="px-4 py-2 text-sm text-text-secondary">{tr('common.cancel')}</button>
          <button onClick={() => deleteItem && deleteMut.mutate(deleteItem.id)} className="px-4 py-2 text-sm text-error font-semibold">{tr('common.delete')}</button>
        </>
      }>
        <p className="text-text-secondary">{tr('trials.noTrials')}</p>
        {deleteError && <p className="text-error text-sm mt-2">{deleteError}</p>}
      </Dialog>
    </div>
  )
}
