import { useEffect, useId, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { api, type SeedBatchPlanRequest, type SeedBatchPlanResponse, type SeedInventoryResponse } from '../api/client'
import { Dialog } from './Dialog'
import { DATE_INPUT_MAX } from '../lib/dateInput'

interface Props {
  /** The variety's batches that still hold seeds, in the order to draw from them. */
  batches: SeedInventoryResponse[]
  /** The batches chosen when the dialog opens; a variety with one batch always starts with it. */
  initialBatchIds?: number[]
  /** The variety's expected germination, to suggest how many plants the seeds give. */
  germinationRate?: number
  onClose: () => void
}

const today = () => new Date().toISOString().slice(0, 10)

/**
 * Plans seed batches: "sow this many seeds to have this many plants ready on this date". The seeds
 * are drawn from the chosen batches in order, so a plan can sow more than one batch holds. The server
 * counts back through the variety's workflow and creates a dated task for each step.
 */
export function SeedPlanDialog({ batches, initialBatchIds, germinationRate, onClose }: Props) {
  const { t, i18n } = useTranslation()
  const qc = useQueryClient()
  const navigate = useNavigate()
  const id = useId()
  const [selectedIds, setSelectedIds] = useState<number[]>(
    batches.length === 1 ? [batches[0].id] : (initialBatchIds ?? []).filter(b => batches.some(x => x.id === b)),
  )
  const [pickerOpen, setPickerOpen] = useState(batches.length > 1 && selectedIds.length === 0)
  // The seed count follows the chosen batches until the grower types their own.
  const [seedsTyped, setSeedsTyped] = useState<string | null>(null)
  const [plants, setPlants] = useState('')
  const [readyDate, setReadyDate] = useState('')
  const [created, setCreated] = useState<SeedBatchPlanResponse | null>(null)

  const selected = batches.filter(b => selectedIds.includes(b.id))
  const available = selected.reduce((sum, b) => sum + b.quantity, 0)
  const seeds = seedsTyped ?? (available > 0 ? String(available) : '')
  const seedCount = parseInt(seeds, 10)
  const plantCount = parseInt(plants, 10)
  const inputError =
    selected.length === 0 ? t('seedPlan.noBatchChosen')
    : seedCount > available ? t(selected.length === 1 ? 'seedPlan.tooManySeeds' : 'seedPlan.tooManySeedsSeveral', { count: available })
    : plantCount > seedCount ? t('seedPlan.morePlantsThanSeeds')
    : null
  const request: SeedBatchPlanRequest | null =
    seedCount >= 1 && plantCount >= 1 && readyDate && !inputError
      ? { seeds: seedCount, plants: plantCount, readyDate, seedBatchIds: selected.map(b => b.id) }
      : null

  // Wait for typing to settle before asking the server for the dates.
  const [debounced, setDebounced] = useState<SeedBatchPlanRequest | null>(null)
  const requestKey = JSON.stringify(request)
  useEffect(() => {
    const timer = setTimeout(() => setDebounced(request), 300)
    return () => clearTimeout(timer)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [requestKey])

  const preview = useQuery({
    queryKey: ['seed-plan-preview', debounced],
    queryFn: () => api.inventory.previewPlan(debounced!),
    enabled: debounced !== null,
    retry: false,
  })
  const plan = request && JSON.stringify(debounced) === requestKey ? preview.data : undefined
  const startsInPast = plan ? plan.steps[0].date < today() : false
  const taskCount = plan ? plan.steps.filter(s => !s.optional).length + Math.max(plan.allocations.length - 1, 0) : 0

  const createMut = useMutation({
    mutationFn: () => api.inventory.createPlan(request!),
    onSuccess: result => {
      qc.invalidateQueries({ queryKey: ['tasks'] })
      setCreated(result)
    },
  })

  const formatDate = (iso: string, withYear = true) =>
    new Date(`${iso}T00:00:00`).toLocaleDateString(i18n.language, { weekday: 'short', day: 'numeric', month: 'short', ...(withYear && { year: 'numeric' }) })
  const suggestedPlants = germinationRate && seedCount > 0 ? Math.floor(seedCount * germinationRate / 100) : null
  const batchLabel = (b: SeedInventoryResponse) =>
    [b.providerName ?? t('seedPlan.batchNumber', { number: batches.indexOf(b) + 1 }), b.expirationDate && t('seedPlan.expires', { date: b.expirationDate })]
      .filter(Boolean).join(' · ')
  const toggleBatch = (batchId: number) =>
    setSelectedIds(ids => ids.includes(batchId) ? ids.filter(x => x !== batchId) : [...ids, batchId])

  if (created) {
    return (
      <Dialog
        open
        onClose={onClose}
        title={t('seedPlan.title')}
        actions={
          <>
            <button onClick={onClose} className="px-4 py-2 text-sm text-text-secondary">{t('common.close')}</button>
            <button onClick={() => navigate('/tasks')} className="btn-primary text-sm">{t('seedPlan.showTasks')}</button>
          </>
        }
      >
        <p className="text-sm text-text-primary">
          {t('seedPlan.created', { count: created.taskIds.length, date: formatDate(created.steps[0].date) })}
        </p>
      </Dialog>
    )
  }

  return (
    <Dialog
      open
      onClose={onClose}
      title={t('seedPlan.title')}
      isDirty={plants !== '' || readyDate !== ''}
      actions={
        <>
          <button onClick={onClose} className="px-4 py-2 text-sm text-text-secondary">{t('common.cancel')}</button>
          <button
            onClick={() => createMut.mutate()}
            disabled={!plan || startsInPast || createMut.isPending}
            className="btn-primary text-sm"
          >
            {createMut.isPending ? t('common.saving') : t('seedPlan.create', { count: taskCount })}
          </button>
        </>
      }
    >
      <div className="space-y-4">
        <p className="text-sm text-text-secondary">
          {t('seedPlan.intro', { name: batches[0].speciesName })}
        </p>

        <div>
          <span className="field-label" id={`${id}-batches`}>{t(batches.length === 1 ? 'seedPlan.batch' : 'seedPlan.batches')}</span>
          {batches.length === 1 ? (
            <p className="text-sm text-text-primary py-2">
              {batchLabel(batches[0])} · {t('seedPlan.seedCount', { count: batches[0].quantity })}
            </p>
          ) : (
            <>
              <button
                type="button"
                aria-expanded={pickerOpen}
                aria-labelledby={`${id}-batches`}
                onClick={() => setPickerOpen(o => !o)}
                className="input w-full"
                style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', textAlign: 'left', cursor: 'pointer' }}
              >
                <span>
                  {selected.length === 0
                    ? t('seedPlan.chooseBatches')
                    : t('seedPlan.chosenBatches', { count: selected.length, seeds: available })}
                </span>
                <span aria-hidden style={{ fontSize: 10 }}>{pickerOpen ? '▲' : '▼'}</span>
              </button>
              {pickerOpen && (
                <ul style={{ listStyle: 'none', margin: '4px 0 0', padding: 0, border: '1px solid color-mix(in srgb, var(--color-ink) 20%, transparent)' }}>
                  {batches.map(b => (
                    <li key={b.id} style={{ borderBottom: '1px solid color-mix(in srgb, var(--color-ink) 10%, transparent)' }}>
                      <label style={{ display: 'flex', alignItems: 'center', gap: 10, padding: '8px 10px', cursor: 'pointer' }}>
                        <input type="checkbox" checked={selectedIds.includes(b.id)} onChange={() => toggleBatch(b.id)} />
                        <span className="text-sm" style={{ flex: 1, minWidth: 0 }}>{batchLabel(b)}</span>
                        <span style={{ fontFamily: 'var(--font-mono)', fontSize: 11, fontVariantNumeric: 'tabular-nums', whiteSpace: 'nowrap' }}>
                          {t('seedPlan.seedCount', { count: b.quantity })}
                        </span>
                      </label>
                    </li>
                  ))}
                </ul>
              )}
            </>
          )}
        </div>

        <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
          <div>
            <label htmlFor={`${id}-seeds`} className="field-label">{t('seedPlan.seeds')}</label>
            <input id={`${id}-seeds`} type="number" min="1" max={available || undefined} value={seeds} onChange={e => setSeedsTyped(e.target.value)} className="input w-full" />
          </div>
          <div>
            <label htmlFor={`${id}-plants`} className="field-label">{t('seedPlan.plants')}</label>
            <input
              id={`${id}-plants`}
              type="number"
              min="1"
              value={plants}
              onChange={e => setPlants(e.target.value)}
              placeholder={suggestedPlants ? String(suggestedPlants) : undefined}
              className="input w-full"
            />
          </div>
          <div className="col-span-2 sm:col-span-1">
            <label htmlFor={`${id}-ready`} className="field-label">{t('seedPlan.readyDate')}</label>
            <input id={`${id}-ready`} type="date" min={today()} max={DATE_INPUT_MAX} value={readyDate} onChange={e => setReadyDate(e.target.value)} className="input w-full" />
          </div>
        </div>
        {suggestedPlants != null && !plants && (
          <p className="text-xs text-text-secondary -mt-2">
            {t('seedPlan.germinationHint', { rate: germinationRate, count: suggestedPlants })}
          </p>
        )}

        {inputError && (selected.length > 0 || !pickerOpen) && <p className="text-error text-sm">{inputError}</p>}
        {preview.error && request && <p className="text-error text-sm">{(preview.error as Error).message}</p>}

        {plan && (
          <div>
            <p className="field-label">{t('seedPlan.schedule')} · {[...new Set(plan.steps.map(s => s.date.slice(0, 4)))].join('–')}</p>
            <ol style={{ listStyle: 'none', margin: 0, padding: 0 }}>
              {plan.steps.map((step, i) => (
                <li
                  key={i}
                  style={{
                    display: 'grid',
                    gridTemplateColumns: '88px minmax(0, 1fr) auto',
                    gap: 10,
                    padding: '6px 0',
                    borderBottom: '1px solid color-mix(in srgb, var(--color-ink) 12%, transparent)',
                    opacity: step.optional ? 0.55 : 1,
                    alignItems: 'baseline',
                  }}
                >
                  <span style={{ fontFamily: 'var(--font-mono)', fontSize: 11, fontVariantNumeric: 'tabular-nums', whiteSpace: 'nowrap' }}>{formatDate(step.date, false)}</span>
                  <span style={{ fontFamily: 'var(--font-display)', fontSize: 15, overflowWrap: 'anywhere' }}>
                    {step.name}
                    {step.optional && <span className="text-xs text-text-secondary"> · {t('seedPlan.optional')}</span>}
                    {step.activityType === 'SOW' && plan.allocations.length > 1 && (
                      <span className="text-xs text-text-secondary" style={{ display: 'block' }}>
                        {plan.allocations.map(a => {
                          const b = batches.find(x => x.id === a.seedBatchId)
                          return `${b ? batchLabel(b) : a.seedBatchId}: ${t('seedPlan.seedCount', { count: a.seeds })}`
                        }).join(' + ')}
                      </span>
                    )}
                  </span>
                  <span style={{ fontFamily: 'var(--font-mono)', fontSize: 11, fontVariantNumeric: 'tabular-nums', whiteSpace: 'nowrap' }}>
                    {step.activityType === 'SOW' ? t('seedPlan.seedCount', { count: step.count }) : t('seedPlan.plantCount', { count: step.count })}
                  </span>
                </li>
              ))}
            </ol>
          </div>
        )}
        {startsInPast && plan && (
          <p className="text-error text-sm">{t('seedPlan.startsInPast', { date: formatDate(plan.steps[0].date) })}</p>
        )}
        {createMut.error && (
          <p className="text-error text-sm">{createMut.error instanceof Error ? createMut.error.message : String(createMut.error)}</p>
        )}
      </div>
    </Dialog>
  )
}
