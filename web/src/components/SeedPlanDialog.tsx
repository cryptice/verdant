import { useEffect, useId, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { api, type SeedBatchPlanRequest, type SeedBatchPlanResponse, type SeedInventoryResponse } from '../api/client'
import { Dialog } from './Dialog'
import { DATE_INPUT_MAX } from '../lib/dateInput'

interface Props {
  batch: SeedInventoryResponse
  /** The variety's expected germination, to suggest how many plants the seeds give. */
  germinationRate?: number
  onClose: () => void
}

const today = () => new Date().toISOString().slice(0, 10)

/**
 * Plans a seed batch: "sow this many seeds to have this many plants ready on this date". The
 * server counts back through the variety's workflow and creates a dated task for each step.
 */
export function SeedPlanDialog({ batch, germinationRate, onClose }: Props) {
  const { t, i18n } = useTranslation()
  const qc = useQueryClient()
  const navigate = useNavigate()
  const id = useId()
  const [seeds, setSeeds] = useState(String(batch.quantity))
  const [plants, setPlants] = useState('')
  const [readyDate, setReadyDate] = useState('')
  const [created, setCreated] = useState<SeedBatchPlanResponse | null>(null)

  const seedCount = parseInt(seeds, 10)
  const plantCount = parseInt(plants, 10)
  const inputError =
    seedCount > batch.quantity ? t('seedPlan.tooManySeeds', { count: batch.quantity })
    : plantCount > seedCount ? t('seedPlan.morePlantsThanSeeds')
    : null
  const request: SeedBatchPlanRequest | null =
    seedCount >= 1 && plantCount >= 1 && readyDate && !inputError ? { seeds: seedCount, plants: plantCount, readyDate } : null

  // Wait for typing to settle before asking the server for the dates.
  const [debounced, setDebounced] = useState<SeedBatchPlanRequest | null>(null)
  const requestKey = JSON.stringify(request)
  useEffect(() => {
    const timer = setTimeout(() => setDebounced(request), 300)
    return () => clearTimeout(timer)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [requestKey])

  const preview = useQuery({
    queryKey: ['seed-plan-preview', batch.id, debounced],
    queryFn: () => api.inventory.previewPlan(batch.id, debounced!),
    enabled: debounced !== null,
    retry: false,
  })
  const plan = request && JSON.stringify(debounced) === requestKey ? preview.data : undefined
  const startsInPast = plan ? plan.steps[0].date < today() : false

  const createMut = useMutation({
    mutationFn: () => api.inventory.createPlan(batch.id, request!),
    onSuccess: result => {
      qc.invalidateQueries({ queryKey: ['tasks'] })
      setCreated(result)
    },
  })

  const formatDate = (iso: string, withYear = true) =>
    new Date(`${iso}T00:00:00`).toLocaleDateString(i18n.language, { weekday: 'short', day: 'numeric', month: 'short', ...(withYear && { year: 'numeric' }) })
  const suggestedPlants = germinationRate && seedCount > 0 ? Math.floor(seedCount * germinationRate / 100) : null

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
            {createMut.isPending ? t('common.saving') : t('seedPlan.create', { count: plan?.steps.filter(s => !s.optional).length ?? 0 })}
          </button>
        </>
      }
    >
      <div className="space-y-4">
        <p className="text-sm text-text-secondary">
          {t('seedPlan.intro', { name: batch.speciesName, count: batch.quantity })}
        </p>
        <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
          <div>
            <label htmlFor={`${id}-seeds`} className="field-label">{t('seedPlan.seeds')}</label>
            <input id={`${id}-seeds`} type="number" min="1" max={batch.quantity} value={seeds} onChange={e => setSeeds(e.target.value)} className="input w-full" />
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

        {inputError && <p className="text-error text-sm">{inputError}</p>}
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
