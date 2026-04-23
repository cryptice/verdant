import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { api, type SeasonResponse } from '../api/client'
import { Masthead, Ledger } from '../components/faltet'
import { ErrorDisplay } from '../components/ErrorDisplay'
import { useOnboarding } from '../onboarding/OnboardingContext'
import { Dialog } from '../components/Dialog'

export function SeasonList() {
  const qc = useQueryClient()
  const { t } = useTranslation()
  const { isActive: isOnboardingActive, isStepComplete, completeStep } = useOnboarding()
  const isSeasonStepComplete = isStepComplete('create_season')
  const { data: seasons = [], error, isLoading, refetch } = useQuery({
    queryKey: ['seasons'],
    queryFn: () => api.seasons.list(),
  })

  const [showAdd, setShowAdd] = useState(false)
  const [editItem, setEditItem] = useState<SeasonResponse | null>(null)
  const [deleteItem, setDeleteItem] = useState<SeasonResponse | null>(null)

  // Form state
  const [formName, setFormName] = useState('')
  const [formYear, setFormYear] = useState('')
  const [formStartDate, setFormStartDate] = useState('')
  const [formEndDate, setFormEndDate] = useState('')
  const [formLastFrost, setFormLastFrost] = useState('')
  const [formFirstFrost, setFormFirstFrost] = useState('')
  const [formNotes, setFormNotes] = useState('')
  const [formActive, setFormActive] = useState(true)

  const [formError, setFormError] = useState<string | null>(null)
  const [deleteError, setDeleteError] = useState<string | null>(null)

  const resetForm = () => {
    setFormName(''); setFormYear(''); setFormStartDate(''); setFormEndDate('')
    setFormLastFrost(''); setFormFirstFrost(''); setFormNotes(''); setFormActive(false); setFormError(null)
  }

  const openAdd = () => {
    resetForm()
    const y = new Date().getFullYear()
    const year = String(y)
    setFormYear(year)
    setFormName(t('seasons.defaultName', { year }))
    setFormStartDate(`${year}-01-01`)
    setFormEndDate(`${year}-12-31`)
    // Last frost: Sunday closest to May 15
    const mid = new Date(y, 4, 15)
    const dayOfWeek = mid.getDay()
    const offset = dayOfWeek === 0 ? 0 : (dayOfWeek <= 3 ? -dayOfWeek : 7 - dayOfWeek)
    const lastFrost = new Date(y, 4, 15 + offset)
    setFormLastFrost(lastFrost.toISOString().split('T')[0])
    // First frost: October 1
    setFormFirstFrost(`${year}-10-01`)
    setFormActive(true)
    setShowAdd(true)
  }

  const openEdit = (s: SeasonResponse) => {
    setFormName(s.name)
    setFormYear(String(s.year))
    setFormStartDate(s.startDate ?? '')
    setFormEndDate(s.endDate ?? '')
    setFormLastFrost(s.lastFrostDate ?? '')
    setFormFirstFrost(s.firstFrostDate ?? '')
    setFormNotes(s.notes ?? '')
    setFormActive(s.isActive)
    setFormError(null)
    setEditItem(s)
  }

  const createMut = useMutation({
    mutationFn: () => api.seasons.create({
      name: formName,
      year: Number(formYear),
      startDate: formStartDate || undefined,
      endDate: formEndDate || undefined,
      lastFrostDate: formLastFrost || undefined,
      firstFrostDate: formFirstFrost || undefined,
      notes: formNotes || undefined,
      isActive: formActive,
    }),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['seasons'] }); setShowAdd(false); resetForm(); completeStep('create_season') },
    onError: (err) => { setFormError(err instanceof Error ? err.message : String(err)) },
  })

  const updateMut = useMutation({
    mutationFn: () => api.seasons.update(editItem!.id, {
      name: formName,
      year: Number(formYear),
      startDate: formStartDate || undefined,
      endDate: formEndDate || undefined,
      lastFrostDate: formLastFrost || undefined,
      firstFrostDate: formFirstFrost || undefined,
      notes: formNotes || undefined,
      isActive: formActive,
    }),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['seasons'] }); setEditItem(null); resetForm() },
    onError: (err) => { setFormError(err instanceof Error ? err.message : String(err)) },
  })

  const deleteMut = useMutation({
    mutationFn: (id: number) => api.seasons.delete(id),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['seasons'] }); setDeleteItem(null); setDeleteError(null) },
    onError: (err) => { setDeleteError(err instanceof Error ? err.message : String(err)) },
  })

  if (isLoading) return <div className="flex justify-center p-16"><div className="animate-spin h-8 w-8 border-2 border-accent border-t-transparent rounded-full" /></div>
  if (error) return <ErrorDisplay error={error} onRetry={refetch} />

  const formFields = (
    <div data-onboarding="season-form" className="space-y-4">
      <div>
        <label className="field-label">{t('seasons.name')} *</label>
        <input type="text" value={formName} onChange={e => setFormName(e.target.value)} placeholder="e.g. Spring 2026" className="input w-full" />
      </div>
      <div className="grid grid-cols-2 gap-3">
        <div>
          <label className="field-label">{t('seasons.year')} *</label>
          <input type="number" value={formYear} onChange={e => {
            const y = e.target.value
            setFormYear(y)
            if (y.length === 4) {
              if (!formStartDate) setFormStartDate(`${y}-01-01`)
              if (!formEndDate) setFormEndDate(`${y}-12-31`)
              setFormActive(Number(y) === new Date().getFullYear())
            }
          }} placeholder="e.g. 2026" className="input w-full" />
        </div>
        <div className="flex items-end pb-1.5">
          <label className="flex items-center gap-2 cursor-pointer text-sm">
            <input type="checkbox" checked={formActive} onChange={e => setFormActive(e.target.checked)} className="rounded" />
            {t('seasons.active')}
          </label>
        </div>
      </div>
      <div className="grid grid-cols-2 gap-3">
        <div>
          <label className="field-label">{t('seasons.startDate')}</label>
          <input type="date" value={formStartDate} onChange={e => setFormStartDate(e.target.value)} className="input w-full" />
        </div>
        <div>
          <label className="field-label">{t('seasons.endDate')}</label>
          <input type="date" value={formEndDate} onChange={e => setFormEndDate(e.target.value)} className="input w-full" />
        </div>
      </div>
      <div className="grid grid-cols-2 gap-3">
        <div>
          <label className="field-label">{t('seasons.lastFrost')}</label>
          <input type="date" value={formLastFrost} onChange={e => setFormLastFrost(e.target.value)} className="input w-full" />
        </div>
        <div>
          <label className="field-label">{t('seasons.firstFrost')}</label>
          <input type="date" value={formFirstFrost} onChange={e => setFormFirstFrost(e.target.value)} className="input w-full" />
        </div>
      </div>
      <div>
        <label className="field-label">{t('seasons.notes')}</label>
        <textarea value={formNotes} onChange={e => setFormNotes(e.target.value)} placeholder={t('common.optional')} rows={2} className="input w-full" />
      </div>
      {formError && <p className="text-error text-sm">{formError}</p>}
    </div>
  )

  return (
    <div>
      <Masthead
        left={t('nav.seasons')}
        center="— Säsongsliggaren —"
        right={
          <button onClick={openAdd} className="btn-primary">
            {t('seasons.newSeason')}
          </button>
        }
      />

      {isOnboardingActive && !isSeasonStepComplete && (
        <div className="bg-accent-light/50 border border-accent/15 rounded-2xl px-6 py-6 text-center mx-10 mt-6">
          <div className="w-10 h-10 rounded-full bg-accent/10 flex items-center justify-center mx-auto mb-3">
            <span className="text-xl">📅</span>
          </div>
          <p className="font-semibold text-text-primary">{t('onboarding.steps.create_season')}</p>
          <p className="text-sm text-text-secondary mt-1 max-w-md mx-auto">{t('onboarding.hints.create_season')}</p>
          <button onClick={openAdd} className="btn-primary mt-4">
            {t('seasons.newSeason')}
          </button>
        </div>
      )}

      <div style={{ padding: '28px 40px' }}>
        <Ledger
          columns={[
            {
              key: 'id',
              label: '№',
              width: '60px',
              render: (_s, i) => (
                <span style={{ fontFamily: 'var(--font-display)', fontStyle: 'italic', fontSize: 22, color: 'var(--color-mustard)' }}>
                  {String(i + 1).padStart(2, '0')}
                </span>
              ),
            },
            {
              key: 'name',
              label: t('seasons.name'),
              width: '1.5fr',
              render: (s) => (
                <span style={{ fontFamily: 'var(--font-display)', fontSize: 20 }}>{s.name}</span>
              ),
            },
            {
              key: 'year',
              label: t('seasons.year'),
              width: '80px',
              align: 'right',
              render: (s) => (
                <span style={{ fontVariantNumeric: 'tabular-nums' }}>{s.year}</span>
              ),
            },
            {
              key: 'active',
              label: t('seasons.active'),
              width: '80px',
              align: 'right',
              render: (s) => (
                s.isActive ? <span style={{ color: 'var(--color-accent)' }}>●</span> : null
              ),
            },
            {
              key: 'frost',
              label: t('seasons.lastFrost'),
              width: '1.2fr',
              render: (s) => (
                <span style={{ fontFamily: 'var(--font-mono)', fontSize: 10 }}>
                  {s.lastFrostDate ?? '—'} → {s.firstFrostDate ?? '—'}
                </span>
              ),
            },
            {
              key: 'goto',
              label: '',
              width: '40px',
              align: 'right',
              render: () => (
                <span style={{ color: 'var(--color-accent)', fontFamily: 'var(--font-mono)' }}>→</span>
              ),
            },
          ]}
          rows={seasons}
          rowKey={(s) => s.id}
          onRowClick={(s) => openEdit(s)}
        />
      </div>

      <Dialog open={showAdd} onClose={() => { setShowAdd(false); resetForm() }} title={t('seasons.newSeason')} actions={
        <>
          <button onClick={() => { setShowAdd(false); resetForm() }} className="px-4 py-2 text-sm text-text-secondary">{t('common.cancel')}</button>
          <button
            onClick={() => createMut.mutate()}
            disabled={!formName || !formYear || createMut.isPending}
            className="btn-primary text-sm"
          >
            {createMut.isPending ? t('common.saving') : t('common.add')}
          </button>
        </>
      }>
        {formFields}
      </Dialog>

      <Dialog open={editItem !== null} onClose={() => { setEditItem(null); resetForm() }} title={t('seasons.editSeason')} actions={
        <>
          <button onClick={() => { setEditItem(null); resetForm() }} className="px-4 py-2 text-sm text-text-secondary">{t('common.cancel')}</button>
          <button
            onClick={() => updateMut.mutate()}
            disabled={!formName || !formYear || updateMut.isPending}
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
          {t('seasons.deleteSeason')}
        </button>
      </Dialog>

      <Dialog open={deleteItem !== null} onClose={() => { setDeleteItem(null); setDeleteError(null) }} title={t('seasons.deleteSeason')} actions={
        <>
          <button onClick={() => { setDeleteItem(null); setDeleteError(null) }} className="px-4 py-2 text-sm text-text-secondary">{t('common.cancel')}</button>
          <button onClick={() => deleteItem && deleteMut.mutate(deleteItem.id)} className="px-4 py-2 text-sm text-error font-semibold">{t('common.delete')}</button>
        </>
      }>
        <p className="text-text-secondary">{t('seasons.deleteSeasonConfirm')}</p>
        {deleteError && <p className="text-error text-sm mt-2">{deleteError}</p>}
      </Dialog>
    </div>
  )
}
