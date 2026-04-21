import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { api, type ProductionTargetResponse, type SpeciesResponse } from '../api/client'
import { ErrorDisplay } from '../components/ErrorDisplay'
import { Dialog } from '../components/Dialog'
import { SpeciesAutocomplete } from '../components/SpeciesAutocomplete'
import { OnboardingHint } from '../onboarding/OnboardingHint'
import { useOnboarding } from '../onboarding/OnboardingContext'
import { Masthead, Chip } from '../components/faltet'

const PAGE_SIZE = 50

const TEMPLATE = '60px 1.5fr 120px 1fr 40px'

const headerStyle: React.CSSProperties = {
  display: 'grid',
  gridTemplateColumns: TEMPLATE,
  gap: 18,
  padding: '10px 0',
  borderBottom: '1px solid var(--color-ink)',
  fontFamily: 'var(--font-mono)',
  fontSize: 9,
  letterSpacing: 1.4,
  textTransform: 'uppercase',
  color: 'var(--color-forest)',
  opacity: 0.7,
  alignItems: 'center',
}

function ForecastPanel({ targetId }: { targetId: number }) {
  const { t } = useTranslation()
  const { data } = useQuery({
    queryKey: ['production-target-forecast', targetId],
    queryFn: () => api.productionTargets.forecast(targetId),
  })

  if (!data) {
    return (
      <div style={{ fontFamily: 'var(--font-mono)', fontSize: 10, letterSpacing: 1.2, color: 'var(--color-forest)', opacity: 0.6 }}>
        {t('targets.forecastPanel.loading')}
      </div>
    )
  }

  return (
    <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 18 }}>
      <div>
        <div className="field-label">{t('targets.forecastPanel.totalStems')}</div>
        <div style={{ fontFamily: 'var(--font-display)', fontSize: 24, fontWeight: 300, fontVariantNumeric: 'tabular-nums' }}>
          {data.totalStemsNeeded.toLocaleString()}
        </div>
      </div>
      <div>
        <div className="field-label">{t('targets.forecastPanel.plants')}</div>
        <div style={{ fontFamily: 'var(--font-display)', fontSize: 24, fontWeight: 300, fontVariantNumeric: 'tabular-nums' }}>
          {data.plantsNeeded.toLocaleString()}
        </div>
      </div>
      <div>
        <div className="field-label">{t('targets.forecastPanel.seeds')}</div>
        <div style={{ fontFamily: 'var(--font-display)', fontSize: 24, fontWeight: 300, fontVariantNumeric: 'tabular-nums' }}>
          {data.seedsNeeded.toLocaleString()}
        </div>
      </div>
      <div>
        <div className="field-label">{t('targets.forecastPanel.sowDate')}</div>
        <div style={{ fontFamily: 'var(--font-display)', fontStyle: 'italic', fontSize: 16, color: 'var(--color-mustard)' }}>
          {data.suggestedSowDate ?? '—'}
        </div>
      </div>
      {data.warnings.length > 0 && (
        <div style={{ gridColumn: '1 / -1', display: 'flex', flexWrap: 'wrap', gap: 6, marginTop: 4 }}>
          {data.warnings.map((w, i) => (
            <Chip key={i} tone="berry">{w}</Chip>
          ))}
        </div>
      )}
    </div>
  )
}

export function ProductionTargets() {
  const qc = useQueryClient()
  const { t } = useTranslation()
  const { completeStep } = useOnboarding()

  const [seasonFilter, setSeasonFilter] = useState<number | undefined>(undefined)
  const [expanded, setExpanded] = useState<Set<number>>(new Set())

  const toggle = (id: number) => setExpanded(prev => {
    const next = new Set(prev)
    next.has(id) ? next.delete(id) : next.add(id)
    return next
  })

  const { data: seasons } = useQuery({
    queryKey: ['seasons'],
    queryFn: () => api.seasons.list(),
  })

  const { data, error, isLoading, refetch } = useQuery({
    queryKey: ['production-targets', seasonFilter],
    queryFn: () => api.productionTargets.list(seasonFilter),
  })

  const [page, setPage] = useState(0)
  const [showAdd, setShowAdd] = useState(false)
  const [editItem, setEditItem] = useState<ProductionTargetResponse | null>(null)
  const [deleteItem, setDeleteItem] = useState<ProductionTargetResponse | null>(null)

  // Form state
  const [formSeasonId, setFormSeasonId] = useState<string>('')
  const [formSpecies, setFormSpecies] = useState<SpeciesResponse | null>(null)
  const [formStemsPerWeek, setFormStemsPerWeek] = useState('')
  const [formStartDate, setFormStartDate] = useState('')
  const [formEndDate, setFormEndDate] = useState('')
  const [formNotes, setFormNotes] = useState('')

  const [formError, setFormError] = useState<string | null>(null)
  const [deleteError, setDeleteError] = useState<string | null>(null)

  const resetForm = () => {
    setFormSeasonId(''); setFormSpecies(null)
    setFormStemsPerWeek(''); setFormStartDate(''); setFormEndDate('')
    setFormNotes(''); setFormError(null)
  }

  const openAdd = () => { resetForm(); setShowAdd(true) }

  const openEdit = (item: ProductionTargetResponse) => {
    setFormSeasonId(String(item.seasonId))
    setFormSpecies(null)
    setFormStemsPerWeek(String(item.stemsPerWeek))
    setFormStartDate(item.startDate)
    setFormEndDate(item.endDate)
    setFormNotes(item.notes ?? '')
    setFormError(null)
    setEditItem(item)
  }

  const buildPayload = () => ({
    seasonId: Number(formSeasonId),
    speciesId: formSpecies?.id ?? editItem?.speciesId,
    stemsPerWeek: Number(formStemsPerWeek),
    startDate: formStartDate,
    endDate: formEndDate,
    notes: formNotes || undefined,
  })

  const createMut = useMutation({
    mutationFn: () => api.productionTargets.create(buildPayload()),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['production-targets'] }); setShowAdd(false); resetForm(); completeStep('set_target') },
    onError: (err) => { setFormError(err instanceof Error ? err.message : String(err)) },
  })

  const updateMut = useMutation({
    mutationFn: () => api.productionTargets.update(editItem!.id, buildPayload()),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['production-targets'] }); setEditItem(null); resetForm() },
    onError: (err) => { setFormError(err instanceof Error ? err.message : String(err)) },
  })

  const deleteMut = useMutation({
    mutationFn: (id: number) => api.productionTargets.delete(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['production-targets'] })
      setDeleteItem(null); setDeleteError(null)
      if (deleteItem) setExpanded(prev => { const next = new Set(prev); next.delete(deleteItem.id); return next })
    },
    onError: (err) => { setDeleteError(err instanceof Error ? err.message : String(err)) },
  })

  if (isLoading) return <div className="flex justify-center p-16"><div className="animate-spin h-8 w-8 border-2 border-accent border-t-transparent rounded-full" /></div>
  if (error) return <ErrorDisplay error={error} onRetry={refetch} />

  const canSubmitCreate = formSeasonId && formSpecies && formStemsPerWeek && formStartDate && formEndDate
  const canSubmitEdit = formSeasonId && (formSpecies || editItem?.speciesId) && formStemsPerWeek && formStartDate && formEndDate

  const targets = data ?? []
  const paged = targets.slice(page * PAGE_SIZE, (page + 1) * PAGE_SIZE)

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
        <label className="field-label">{t('targets.stemsPerWeek')} *</label>
        <input type="number" value={formStemsPerWeek} onChange={e => setFormStemsPerWeek(e.target.value)} min="1" className="input" />
      </div>
      <div>
        <label className="field-label">{t('targets.startDate')} *</label>
        <input type="date" value={formStartDate} onChange={e => setFormStartDate(e.target.value)} className="input" />
      </div>
      <div>
        <label className="field-label">{t('targets.endDate')} *</label>
        <input type="date" value={formEndDate} onChange={e => setFormEndDate(e.target.value)} className="input" />
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
      <Masthead
        left={t('nav.targets')}
        center="— Målliggaren —"
        right={
          <button onClick={openAdd} className="btn-primary" data-onboarding="add-target-btn">
            {t('targets.new')}
          </button>
        }
      />
      <OnboardingHint />

      <div style={{ padding: '28px 40px' }}>
        {/* Season filter */}
        <div style={{ marginBottom: 22 }}>
          <select
            value={seasonFilter ?? ''}
            onChange={e => { setSeasonFilter(e.target.value ? Number(e.target.value) : undefined); setPage(0); setExpanded(new Set()) }}
            className="input w-auto"
          >
            <option value="">{t('pestDisease.allSeasons')}</option>
            {seasons?.map(s => (
              <option key={s.id} value={s.id}>{s.name}</option>
            ))}
          </select>
        </div>

        {targets.length === 0 && (
          <div style={{ padding: '40px 0', textAlign: 'center', borderBottom: '1px solid var(--color-ink)', borderTop: '1px solid var(--color-ink)' }}>
            <div style={{ fontFamily: 'var(--font-mono)', fontSize: 9, letterSpacing: 1.4, textTransform: 'uppercase', color: 'var(--color-forest)', opacity: 0.7 }}>
              {t('targets.noTargets')}
            </div>
          </div>
        )}

        {targets.length > 0 && (
          <>
            {/* Header row */}
            <div style={headerStyle}>
              <span>№</span>
              <span>{t('targets.col.species')}</span>
              <span>{t('targets.col.stemsPerWeek')}</span>
              <span>{t('targets.col.window')}</span>
              <span />
            </div>

            {/* Body rows */}
            {paged.map((target, i) => (
              <div key={target.id}>
                <button
                  onClick={() => toggle(target.id)}
                  style={{
                    display: 'grid',
                    gridTemplateColumns: TEMPLATE,
                    gap: 18,
                    padding: '14px 0',
                    borderBottom: expanded.has(target.id)
                      ? 'none'
                      : '1px solid color-mix(in srgb, var(--color-ink) 20%, transparent)',
                    width: '100%',
                    background: 'transparent',
                    border: 'none',
                    textAlign: 'left',
                    cursor: 'pointer',
                    alignItems: 'center',
                  }}
                  className="ledger-row"
                >
                  <span style={{ fontFamily: 'var(--font-display)', fontStyle: 'italic', fontSize: 22, color: 'var(--color-mustard)' }}>
                    {String(page * PAGE_SIZE + i + 1).padStart(2, '0')}
                  </span>
                  <span style={{ fontFamily: 'var(--font-display)', fontSize: 20 }}>{target.speciesName}</span>
                  <span style={{ fontVariantNumeric: 'tabular-nums', fontFamily: 'var(--font-display)', fontSize: 20 }}>
                    {target.stemsPerWeek}
                  </span>
                  <span style={{ fontFamily: 'var(--font-mono)', fontSize: 10 }}>
                    {target.startDate} — {target.endDate}
                  </span>
                  <span style={{ fontFamily: 'var(--font-mono)', fontSize: 12 }}>
                    {expanded.has(target.id) ? '▼' : '▶'}
                  </span>
                </button>

                {expanded.has(target.id) && (
                  <div style={{
                    padding: '16px 78px',
                    background: 'color-mix(in srgb, var(--color-ink) 3%, transparent)',
                    borderBottom: '1px solid color-mix(in srgb, var(--color-ink) 20%, transparent)',
                  }}>
                    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 12 }}>
                      <div style={{ fontFamily: 'var(--font-mono)', fontSize: 9, letterSpacing: 1.4, textTransform: 'uppercase', color: 'var(--color-forest)', opacity: 0.7 }}>
                        {t('targets.forecast')}
                      </div>
                      <button
                        onClick={() => openEdit(target)}
                        style={{ fontFamily: 'var(--font-mono)', fontSize: 10, letterSpacing: 1.4, textTransform: 'uppercase', color: 'var(--color-clay)', background: 'transparent', border: 'none', cursor: 'pointer' }}
                      >
                        {t('targets.edit')} →
                      </button>
                    </div>
                    <ForecastPanel targetId={target.id} />
                  </div>
                )}
              </div>
            ))}

            {/* Pagination (simple) */}
            {targets.length > PAGE_SIZE && (
              <div style={{ display: 'flex', gap: 8, marginTop: 16, fontFamily: 'var(--font-mono)', fontSize: 10 }}>
                <button
                  onClick={() => setPage(p => Math.max(0, p - 1))}
                  disabled={page === 0}
                  style={{ background: 'transparent', border: 'none', cursor: page === 0 ? 'default' : 'pointer', opacity: page === 0 ? 0.3 : 1 }}
                >
                  ←
                </button>
                <span>{page + 1} / {Math.ceil(targets.length / PAGE_SIZE)}</span>
                <button
                  onClick={() => setPage(p => Math.min(Math.ceil(targets.length / PAGE_SIZE) - 1, p + 1))}
                  disabled={(page + 1) * PAGE_SIZE >= targets.length}
                  style={{ background: 'transparent', border: 'none', cursor: (page + 1) * PAGE_SIZE >= targets.length ? 'default' : 'pointer', opacity: (page + 1) * PAGE_SIZE >= targets.length ? 0.3 : 1 }}
                >
                  →
                </button>
              </div>
            )}
          </>
        )}
      </div>

      <Dialog open={showAdd} onClose={() => { setShowAdd(false); resetForm() }} title={t('targets.new')} actions={
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

      <Dialog open={editItem !== null} onClose={() => { setEditItem(null); resetForm() }} title={t('targets.edit')} actions={
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
        <p className="text-text-secondary">{t('targets.noTargets')}</p>
        {deleteError && <p className="text-error text-sm mt-2">{deleteError}</p>}
      </Dialog>
    </div>
  )
}
