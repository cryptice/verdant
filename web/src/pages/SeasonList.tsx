import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { api, type SeasonResponse } from '../api/client'
import { PageHeader } from '../components/PageHeader'
import { ErrorDisplay } from '../components/ErrorDisplay'
import { useOnboarding } from '../onboarding/OnboardingContext'
import { Dialog } from '../components/Dialog'
import { Pagination } from '../components/Pagination'

const PAGE_SIZE = 50

export function SeasonList() {
  const qc = useQueryClient()
  const { t } = useTranslation()
  const { isActive: isOnboardingActive, isStepComplete } = useOnboarding()
  const isSeasonStepComplete = isStepComplete('create_season')
  const { data, error, isLoading, refetch } = useQuery({
    queryKey: ['seasons'],
    queryFn: () => api.seasons.list(),
  })

  const [page, setPage] = useState(0)
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

  const [formError, setFormError] = useState<string | null>(null)
  const [deleteError, setDeleteError] = useState<string | null>(null)

  const resetForm = () => {
    setFormName(''); setFormYear(''); setFormStartDate(''); setFormEndDate('')
    setFormLastFrost(''); setFormFirstFrost(''); setFormNotes(''); setFormError(null)
  }

  const openAdd = () => { resetForm(); setShowAdd(true) }

  const openEdit = (s: SeasonResponse) => {
    setFormName(s.name)
    setFormYear(String(s.year))
    setFormStartDate(s.startDate ?? '')
    setFormEndDate(s.endDate ?? '')
    setFormLastFrost(s.lastFrostDate ?? '')
    setFormFirstFrost(s.firstFrostDate ?? '')
    setFormNotes(s.notes ?? '')
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
    }),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['seasons'] }); setShowAdd(false); resetForm() },
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
        <input type="text" value={formName} onChange={e => setFormName(e.target.value)} placeholder="e.g. Spring 2026" className="input" />
      </div>
      <div>
        <label className="field-label">{t('seasons.year')} *</label>
        <input type="number" value={formYear} onChange={e => setFormYear(e.target.value)} placeholder="e.g. 2026" className="input" />
      </div>
      <div>
        <label className="field-label">{t('seasons.startDate')}</label>
        <input type="date" value={formStartDate} onChange={e => setFormStartDate(e.target.value)} className="input" />
      </div>
      <div>
        <label className="field-label">{t('seasons.endDate')}</label>
        <input type="date" value={formEndDate} onChange={e => setFormEndDate(e.target.value)} className="input" />
      </div>
      <div>
        <label className="field-label">{t('seasons.lastFrost')}</label>
        <input type="date" value={formLastFrost} onChange={e => setFormLastFrost(e.target.value)} className="input" />
      </div>
      <div>
        <label className="field-label">{t('seasons.firstFrost')}</label>
        <input type="date" value={formFirstFrost} onChange={e => setFormFirstFrost(e.target.value)} className="input" />
      </div>
      <div>
        <label className="field-label">{t('seasons.notes')}</label>
        <textarea value={formNotes} onChange={e => setFormNotes(e.target.value)} placeholder={t('common.optional')} rows={2} className="input" />
      </div>
      {formError && <p className="text-error text-sm">{formError}</p>}
    </div>
  )

  return (
    <div>
      <PageHeader title={t('seasons.title')} action={{ label: t('seasons.newSeason'), onClick: openAdd }} />
      {isOnboardingActive && !isSeasonStepComplete && (
        <div className="bg-accent-light/50 border border-accent/15 rounded-2xl px-6 py-6 text-center">
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
      <div className="px-4 py-4">
        {data && data.length === 0 && !(isOnboardingActive && !isSeasonStepComplete) && (
          <p className="text-text-secondary text-sm text-center py-4">{t('seasons.noSeasons')}</p>
        )}

        {data && data.length > 0 && (<>
          <div className="border border-divider rounded-xl overflow-hidden bg-bg shadow-sm">
            <table className="w-full">
              <thead>
                <tr className="border-b border-divider bg-surface">
                  <th className="text-left px-4 py-2 text-xs font-medium text-text-secondary">{t('seasons.name')}</th>
                  <th className="text-right px-4 py-2 text-xs font-medium text-text-secondary">{t('seasons.year')}</th>
                  <th className="text-left px-4 py-2 text-xs font-medium text-text-secondary">{t('seasons.active')}</th>
                  <th className="text-left px-4 py-2 text-xs font-medium text-text-secondary">{t('seasons.lastFrost')}</th>
                  <th className="text-left px-4 py-2 text-xs font-medium text-text-secondary">{t('seasons.firstFrost')}</th>
                </tr>
              </thead>
              <tbody>
                {data.slice(page * PAGE_SIZE, (page + 1) * PAGE_SIZE).map(s => (
                  <tr
                    key={s.id}
                    className={`border-b border-divider last:border-0 hover:bg-surface cursor-pointer transition-colors ${s.isActive ? 'bg-accent-light/30' : ''}`}
                    onClick={() => openEdit(s)}
                  >
                    <td className="px-4 py-2.5 text-sm">{s.name}</td>
                    <td className="px-4 py-2.5 text-sm text-right tabular-nums">{s.year}</td>
                    <td className="px-4 py-2.5 text-sm">
                      {s.isActive && (
                        <span className="inline-block px-2 py-0.5 rounded-full text-xs font-medium bg-green-100 text-green-700">
                          {t('seasons.active')}
                        </span>
                      )}
                    </td>
                    <td className="px-4 py-2.5 text-sm text-text-secondary">{s.lastFrostDate ?? '—'}</td>
                    <td className="px-4 py-2.5 text-sm text-text-secondary">{s.firstFrostDate ?? '—'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <Pagination page={page} pageSize={PAGE_SIZE} total={data.length} onPageChange={setPage} />
        </>)}
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
