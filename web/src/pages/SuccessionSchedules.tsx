import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { api, type SuccessionScheduleResponse, type SpeciesResponse, type BedWithGardenResponse } from '../api/client'
import { PageHeader } from '../components/PageHeader'
import { ErrorDisplay } from '../components/ErrorDisplay'
import { Dialog } from '../components/Dialog'
import { Pagination } from '../components/Pagination'
import { SpeciesAutocomplete } from '../components/SpeciesAutocomplete'

const PAGE_SIZE = 50

export function SuccessionSchedules() {
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
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['succession-schedules'] }); setShowAdd(false); resetForm() },
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
      <PageHeader title={t('successions.title')} action={{ label: t('successions.new'), onClick: openAdd }} />
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

        {/* Task generation toast */}
        {taskMessage && (
          <div className="mb-4 px-4 py-2 rounded-xl bg-green-50 border border-green-200 text-green-700 text-sm">
            {taskMessage}
          </div>
        )}

        {data && data.length === 0 && (
          <p className="text-text-secondary text-sm text-center py-4">{t('successions.noSchedules')}</p>
        )}

        {data && data.length > 0 && (<>
          <div className="border border-divider rounded-xl overflow-hidden bg-bg shadow-sm">
            <table className="w-full">
              <thead>
                <tr className="border-b border-divider bg-surface">
                  <th className="text-left px-4 py-2 text-xs font-medium text-text-secondary">{t('common.speciesLabel')}</th>
                  <th className="text-left px-4 py-2 text-xs font-medium text-text-secondary">{t('successions.firstSow')}</th>
                  <th className="text-right px-4 py-2 text-xs font-medium text-text-secondary">{t('successions.interval')}</th>
                  <th className="text-right px-4 py-2 text-xs font-medium text-text-secondary">{t('successions.totalSuccessions')}</th>
                  <th className="text-right px-4 py-2 text-xs font-medium text-text-secondary">{t('successions.seedsPerRound')}</th>
                  <th className="text-right px-4 py-2 text-xs font-medium text-text-secondary">{t('successions.totalSeeds')}</th>
                  <th className="text-center px-4 py-2 text-xs font-medium text-text-secondary"></th>
                </tr>
              </thead>
              <tbody>
                {data.slice(page * PAGE_SIZE, (page + 1) * PAGE_SIZE).map(item => (
                  <tr
                    key={item.id}
                    className="border-b border-divider last:border-0 hover:bg-surface cursor-pointer transition-colors"
                    onClick={() => openEdit(item)}
                  >
                    <td className="px-4 py-2.5 text-sm">{item.speciesName}</td>
                    <td className="px-4 py-2.5 text-sm text-text-secondary">{item.firstSowDate}</td>
                    <td className="px-4 py-2.5 text-sm text-right tabular-nums">{item.intervalDays}d</td>
                    <td className="px-4 py-2.5 text-sm text-right tabular-nums">{item.totalSuccessions}</td>
                    <td className="px-4 py-2.5 text-sm text-right tabular-nums">{item.seedsPerSuccession}</td>
                    <td className="px-4 py-2.5 text-sm text-right tabular-nums font-medium">{item.totalSuccessions * item.seedsPerSuccession}</td>
                    <td className="px-4 py-2.5 text-sm text-center">
                      <button
                        onClick={(e) => { e.stopPropagation(); generateTasksMut.mutate(item.id) }}
                        disabled={generateTasksMut.isPending}
                        className="text-xs px-2 py-1 rounded-lg bg-accent-light text-accent hover:bg-accent hover:text-white transition-colors"
                      >
                        {t('successions.generateTasks')}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <Pagination page={page} pageSize={PAGE_SIZE} total={data.length} onPageChange={setPage} />
        </>)}
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
