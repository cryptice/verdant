import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { api, type SuccessionScheduleResponse, type SpeciesResponse, type BedWithGardenResponse } from '../api/client'
import { ErrorDisplay } from '../components/ErrorDisplay'
import { Dialog } from '../components/Dialog'
import { SpeciesAutocomplete } from '../components/SpeciesAutocomplete'
import { OnboardingHint } from '../onboarding/OnboardingHint'
import { useOnboarding } from '../onboarding/OnboardingContext'
import { Masthead } from '../components/faltet'

const PAGE_SIZE = 50

const TEMPLATE = '60px 1.5fr 120px 100px 80px 160px 40px'

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

export function SuccessionSchedules() {
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
    queryKey: ['succession-schedules', seasonFilter],
    queryFn: () => api.successionSchedules.list(seasonFilter),
  })

  const [page, setPage] = useState(0)
  const [showAdd, setShowAdd] = useState(false)
  const [editItem, setEditItem] = useState<SuccessionScheduleResponse | null>(null)
  const [deleteItem, setDeleteItem] = useState<SuccessionScheduleResponse | null>(null)
  const [taskMessage, setTaskMessage] = useState<string | null>(null)

  // Form state
  const [formSeasonId, setFormSeasonId] = useState<string>('')
  const [formSpecies, setFormSpecies] = useState<SpeciesResponse | null>(null)
  const [formBedId, setFormBedId] = useState<string>('')
  const [formFirstSowDate, setFormFirstSowDate] = useState('')
  const [formIntervalDays, setFormIntervalDays] = useState('')
  const [formTotalSuccessions, setFormTotalSuccessions] = useState('')
  const [formSeedsPerSuccession, setFormSeedsPerSuccession] = useState('')
  const [formNotes, setFormNotes] = useState('')

  const [formError, setFormError] = useState<string | null>(null)
  const [deleteError, setDeleteError] = useState<string | null>(null)

  const resetForm = () => {
    setFormSeasonId(''); setFormSpecies(null); setFormBedId('')
    setFormFirstSowDate(''); setFormIntervalDays(''); setFormTotalSuccessions('')
    setFormSeedsPerSuccession(''); setFormNotes(''); setFormError(null)
  }

  const openAdd = () => { resetForm(); setShowAdd(true) }

  const openEdit = (item: SuccessionScheduleResponse) => {
    setFormSeasonId(String(item.seasonId))
    setFormSpecies(null)
    setFormBedId(item.bedId ? String(item.bedId) : '')
    setFormFirstSowDate(item.firstSowDate)
    setFormIntervalDays(String(item.intervalDays))
    setFormTotalSuccessions(String(item.totalSuccessions))
    setFormSeedsPerSuccession(String(item.seedsPerSuccession))
    setFormNotes(item.notes ?? '')
    setFormError(null)
    setEditItem(item)
  }

  const buildPayload = () => ({
    seasonId: Number(formSeasonId),
    speciesId: formSpecies?.id ?? editItem?.speciesId,
    bedId: formBedId ? Number(formBedId) : undefined,
    firstSowDate: formFirstSowDate,
    intervalDays: Number(formIntervalDays),
    totalSuccessions: Number(formTotalSuccessions),
    seedsPerSuccession: Number(formSeedsPerSuccession),
    notes: formNotes || undefined,
  })

  const createMut = useMutation({
    mutationFn: () => api.successionSchedules.create(buildPayload()),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['succession-schedules'] }); setShowAdd(false); resetForm(); completeStep('setup_succession') },
    onError: (err) => { setFormError(err instanceof Error ? err.message : String(err)) },
  })

  const updateMut = useMutation({
    mutationFn: () => api.successionSchedules.update(editItem!.id, buildPayload()),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['succession-schedules'] }); setEditItem(null); resetForm() },
    onError: (err) => { setFormError(err instanceof Error ? err.message : String(err)) },
  })

  const deleteMut = useMutation({
    mutationFn: (id: number) => api.successionSchedules.delete(id),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['succession-schedules'] }); setDeleteItem(null); setDeleteError(null) },
    onError: (err) => { setDeleteError(err instanceof Error ? err.message : String(err)) },
  })

  const generateTasksMut = useMutation({
    mutationFn: (id: number) => api.successionSchedules.generateTasks(id),
    onSuccess: (taskIds) => {
      qc.invalidateQueries({ queryKey: ['tasks'] })
      setTaskMessage(t('successions.tasksGenerated', { count: taskIds.length }))
      setTimeout(() => setTaskMessage(null), 4000)
    },
    onError: (err) => {
      setTaskMessage(err instanceof Error ? err.message : String(err))
      setTimeout(() => setTaskMessage(null), 4000)
    },
  })

  if (isLoading) return <div className="flex justify-center p-16"><div className="animate-spin h-8 w-8 border-2 border-accent border-t-transparent rounded-full" /></div>
  if (error) return <ErrorDisplay error={error} onRetry={refetch} />

  const canSubmitCreate = formSeasonId && formSpecies && formFirstSowDate && formIntervalDays && formTotalSuccessions && formSeedsPerSuccession
  const canSubmitEdit = formSeasonId && (formSpecies || editItem?.speciesId) && formFirstSowDate && formIntervalDays && formTotalSuccessions && formSeedsPerSuccession

  const schedules = data ?? []
  const paged = schedules.slice(page * PAGE_SIZE, (page + 1) * PAGE_SIZE)

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
        <label className="field-label">{t('successions.firstSow')} *</label>
        <input type="date" value={formFirstSowDate} onChange={e => setFormFirstSowDate(e.target.value)} className="input" />
      </div>
      <div>
        <label className="field-label">{t('successions.interval')} *</label>
        <input type="number" value={formIntervalDays} onChange={e => setFormIntervalDays(e.target.value)} min="1" className="input" />
      </div>
      <div>
        <label className="field-label">{t('successions.totalSuccessions')} *</label>
        <input type="number" value={formTotalSuccessions} onChange={e => setFormTotalSuccessions(e.target.value)} min="1" className="input" />
      </div>
      <div>
        <label className="field-label">{t('successions.seedsPerRound')} *</label>
        <input type="number" value={formSeedsPerSuccession} onChange={e => setFormSeedsPerSuccession(e.target.value)} min="1" className="input" />
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
        left={t('nav.successions')}
        center="— Successionsliggaren —"
        right={
          <button onClick={openAdd} className="btn-primary" data-onboarding="add-succession-btn">
            {t('successions.new')}
          </button>
        }
      />
      <OnboardingHint />

      <div style={{ padding: '28px 40px' }}>
        {/* Season filter */}
        <div style={{ marginBottom: 22 }}>
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

        {/* Task generation toast */}
        {taskMessage && (
          <div style={{ marginBottom: 16, padding: '8px 16px', background: 'color-mix(in srgb, var(--color-sage) 15%, transparent)', border: '1px solid color-mix(in srgb, var(--color-sage) 40%, transparent)', fontFamily: 'var(--font-mono)', fontSize: 10, letterSpacing: 1.2, textTransform: 'uppercase', color: 'var(--color-forest)' }}>
            {taskMessage}
          </div>
        )}

        {schedules.length === 0 && (
          <div style={{ padding: '40px 0', textAlign: 'center', borderBottom: '1px solid var(--color-ink)', borderTop: '1px solid var(--color-ink)' }}>
            <div style={{ fontFamily: 'var(--font-mono)', fontSize: 9, letterSpacing: 1.4, textTransform: 'uppercase', color: 'var(--color-forest)', opacity: 0.7 }}>
              {t('successions.noSchedules')}
            </div>
          </div>
        )}

        {schedules.length > 0 && (
          <>
            {/* Header row */}
            <div style={headerStyle}>
              <span>№</span>
              <span>{t('successions.col.species')}</span>
              <span>{t('successions.col.firstSow')}</span>
              <span>{t('successions.col.interval')}</span>
              <span>{t('successions.col.total')}</span>
              <span>{t('successions.col.generate')}</span>
              <span />
            </div>

            {/* Body rows */}
            {paged.map((s, i) => (
              <div
                key={s.id}
                style={{
                  display: 'grid',
                  gridTemplateColumns: TEMPLATE,
                  gap: 18,
                  padding: '14px 0',
                  borderBottom: '1px solid color-mix(in srgb, var(--color-ink) 20%, transparent)',
                  alignItems: 'center',
                }}
              >
                <span style={{ fontFamily: 'var(--font-display)', fontStyle: 'italic', fontSize: 22, color: 'var(--color-clay)' }}>
                  {String(page * PAGE_SIZE + i + 1).padStart(2, '0')}
                </span>
                <span style={{ fontFamily: 'var(--font-display)', fontSize: 20 }}>{s.speciesName}</span>
                <span style={{ fontFamily: 'var(--font-mono)', fontSize: 10 }}>{s.firstSowDate}</span>
                <span style={{ fontVariantNumeric: 'tabular-nums', fontFamily: 'var(--font-display)', fontSize: 16 }}>{s.intervalDays} d</span>
                <span style={{ fontVariantNumeric: 'tabular-nums', fontFamily: 'var(--font-display)', fontSize: 16 }}>{s.totalSuccessions}</span>
                <button
                  onClick={() => generateTasksMut.mutate(s.id)}
                  disabled={generateTasksMut.isPending}
                  style={{ background: 'transparent', border: 'none', fontFamily: 'var(--font-mono)', fontSize: 10, letterSpacing: 1.4, textTransform: 'uppercase', color: 'var(--color-clay)', cursor: 'pointer', textAlign: 'left' }}
                >
                  {t('successions.generateTasks')} →
                </button>
                <button
                  onClick={() => openEdit(s)}
                  style={{ background: 'transparent', border: 'none', color: 'var(--color-clay)', fontFamily: 'var(--font-mono)', cursor: 'pointer' }}
                >
                  →
                </button>
              </div>
            ))}

            {/* Pagination (simple) */}
            {schedules.length > PAGE_SIZE && (
              <div style={{ display: 'flex', gap: 8, marginTop: 16, fontFamily: 'var(--font-mono)', fontSize: 10 }}>
                <button
                  onClick={() => setPage(p => Math.max(0, p - 1))}
                  disabled={page === 0}
                  style={{ background: 'transparent', border: 'none', cursor: page === 0 ? 'default' : 'pointer', opacity: page === 0 ? 0.3 : 1 }}
                >
                  ←
                </button>
                <span>{page + 1} / {Math.ceil(schedules.length / PAGE_SIZE)}</span>
                <button
                  onClick={() => setPage(p => Math.min(Math.ceil(schedules.length / PAGE_SIZE) - 1, p + 1))}
                  disabled={(page + 1) * PAGE_SIZE >= schedules.length}
                  style={{ background: 'transparent', border: 'none', cursor: (page + 1) * PAGE_SIZE >= schedules.length ? 'default' : 'pointer', opacity: (page + 1) * PAGE_SIZE >= schedules.length ? 0.3 : 1 }}
                >
                  →
                </button>
              </div>
            )}
          </>
        )}
      </div>

      <Dialog open={showAdd} onClose={() => { setShowAdd(false); resetForm() }} title={t('successions.new')} actions={
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

      <Dialog open={editItem !== null} onClose={() => { setEditItem(null); resetForm() }} title={t('successions.edit')} actions={
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
        <p className="text-text-secondary">{t('successions.noSchedules')}</p>
        {deleteError && <p className="text-error text-sm mt-2">{deleteError}</p>}
      </Dialog>
    </div>
  )
}
