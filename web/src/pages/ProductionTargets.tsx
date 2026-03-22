import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { api, type ProductionTargetResponse, type SpeciesResponse } from '../api/client'
import { PageHeader } from '../components/PageHeader'
import { ErrorDisplay } from '../components/ErrorDisplay'
import { Dialog } from '../components/Dialog'
import { Pagination } from '../components/Pagination'
import { SpeciesAutocomplete } from '../components/SpeciesAutocomplete'

const PAGE_SIZE = 50

export function ProductionTargets() {
  const qc = useQueryClient()
  const { t } = useTranslation()

  const [seasonFilter, setSeasonFilter] = useState<number | undefined>(undefined)
  const [selectedId, setSelectedId] = useState<number | null>(null)

  const { data: seasons } = useQuery({
    queryKey: ['seasons'],
    queryFn: () => api.seasons.list(),
  })

  const { data, error, isLoading, refetch } = useQuery({
    queryKey: ['production-targets', seasonFilter],
    queryFn: () => api.productionTargets.list(seasonFilter),
  })

  const { data: forecast, isLoading: forecastLoading } = useQuery({
    queryKey: ['production-target-forecast', selectedId],
    queryFn: () => api.productionTargets.forecast(selectedId!),
    enabled: selectedId !== null,
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
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['production-targets'] }); setShowAdd(false); resetForm() },
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
      if (deleteItem && selectedId === deleteItem.id) setSelectedId(null)
    },
    onError: (err) => { setDeleteError(err instanceof Error ? err.message : String(err)) },
  })

  if (isLoading) return <div className="flex justify-center p-16"><div className="animate-spin h-8 w-8 border-2 border-accent border-t-transparent rounded-full" /></div>
  if (error) return <ErrorDisplay error={error} onRetry={refetch} />

  const canSubmitCreate = formSeasonId && formSpecies && formStemsPerWeek && formStartDate && formEndDate
  const canSubmitEdit = formSeasonId && (formSpecies || editItem?.speciesId) && formStemsPerWeek && formStartDate && formEndDate

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
      <PageHeader title={t('targets.title')} action={{ label: t('targets.new'), onClick: openAdd }} />
      <div className="px-4 py-4">
        {/* Season filter */}
        <div className="mb-4">
          <select
            value={seasonFilter ?? ''}
            onChange={e => { setSeasonFilter(e.target.value ? Number(e.target.value) : undefined); setPage(0); setSelectedId(null) }}
            className="input w-auto"
          >
            <option value="">{t('pestDisease.allSeasons')}</option>
            {seasons?.map(s => (
              <option key={s.id} value={s.id}>{s.name}</option>
            ))}
          </select>
        </div>

        <div className="flex flex-col lg:flex-row gap-4">
          {/* Left: Table */}
          <div className="flex-1 min-w-0">
            {data && data.length === 0 && (
              <p className="text-text-secondary text-sm text-center py-4">{t('targets.noTargets')}</p>
            )}

            {data && data.length > 0 && (<>
              <div className="border border-divider rounded-xl overflow-hidden bg-bg shadow-sm">
                <table className="w-full">
                  <thead>
                    <tr className="border-b border-divider bg-surface">
                      <th className="text-left px-4 py-2 text-xs font-medium text-text-secondary">{t('common.speciesLabel')}</th>
                      <th className="text-right px-4 py-2 text-xs font-medium text-text-secondary">{t('targets.stemsPerWeek')}</th>
                      <th className="text-left px-4 py-2 text-xs font-medium text-text-secondary">{t('targets.deliveryWindow')}</th>
                      <th className="text-left px-4 py-2 text-xs font-medium text-text-secondary">{t('common.notesLabel')}</th>
                    </tr>
                  </thead>
                  <tbody>
                    {data.slice(page * PAGE_SIZE, (page + 1) * PAGE_SIZE).map(item => (
                      <tr
                        key={item.id}
                        className={`border-b border-divider last:border-0 hover:bg-surface cursor-pointer transition-colors ${selectedId === item.id ? 'bg-accent-light/30' : ''}`}
                        onClick={() => setSelectedId(item.id)}
                        onDoubleClick={() => openEdit(item)}
                      >
                        <td className="px-4 py-2.5 text-sm">{item.speciesName}</td>
                        <td className="px-4 py-2.5 text-sm text-right tabular-nums">{item.stemsPerWeek}</td>
                        <td className="px-4 py-2.5 text-sm text-text-secondary">{item.startDate} — {item.endDate}</td>
                        <td className="px-4 py-2.5 text-sm text-text-secondary truncate max-w-[120px]">{item.notes ?? '—'}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
              <Pagination page={page} pageSize={PAGE_SIZE} total={data.length} onPageChange={setPage} />
            </>)}
          </div>

          {/* Right: Forecast panel */}
          <div className="lg:w-72 shrink-0">
            <div className="border border-divider rounded-xl bg-bg shadow-sm p-4">
              <h3 className="text-sm font-medium text-text-primary mb-3">{t('targets.forecast')}</h3>

              {!selectedId && (
                <p className="text-text-secondary text-sm">{t('targets.selectTarget')}</p>
              )}

              {selectedId && forecastLoading && (
                <div className="flex justify-center py-4">
                  <div className="animate-spin h-5 w-5 border-2 border-accent border-t-transparent rounded-full" />
                </div>
              )}

              {selectedId && forecast && !forecastLoading && (
                <div className="space-y-3">
                  <div>
                    <p className="text-xs text-text-secondary">{t('targets.totalStems')}</p>
                    <p className="text-lg font-semibold tabular-nums">{forecast.totalStemsNeeded.toLocaleString()}</p>
                  </div>
                  <div>
                    <p className="text-xs text-text-secondary">{t('targets.plantsNeeded')}</p>
                    <p className="text-lg font-semibold tabular-nums">{forecast.plantsNeeded.toLocaleString()}</p>
                  </div>
                  <div>
                    <p className="text-xs text-text-secondary">{t('targets.seedsNeeded')}</p>
                    <p className="text-lg font-semibold tabular-nums">{forecast.seedsNeeded.toLocaleString()}</p>
                  </div>
                  {forecast.suggestedSowDate && (
                    <div>
                      <p className="text-xs text-text-secondary">{t('targets.suggestedSow')}</p>
                      <p className="text-sm font-medium">{forecast.suggestedSowDate}</p>
                    </div>
                  )}
                  {forecast.warnings.length > 0 && (
                    <div className="space-y-1">
                      {forecast.warnings.map((w, i) => (
                        <div key={i} className="px-3 py-2 rounded-lg bg-yellow-50 border border-yellow-200 text-yellow-800 text-xs">
                          {w}
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              )}
            </div>
          </div>
        </div>
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
