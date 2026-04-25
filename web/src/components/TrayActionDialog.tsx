import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../api/client'
import { Dialog } from './Dialog'

export interface TrayActionEntry {
  speciesId: number
  speciesName: string
  variantName?: string
  status: string
  count: number
}

type Step = 'choose' | 'pot_up' | 'plant_out' | 'discard'

interface Props {
  open: boolean
  entry: TrayActionEntry | null
  onClose: () => void
}

const formatSpecies = (entry: TrayActionEntry) =>
  entry.variantName ? `${entry.speciesName} – ${entry.variantName}` : entry.speciesName

export function TrayActionDialog({ open, entry, onClose }: Props) {
  const { t } = useTranslation()
  const qc = useQueryClient()

  const [step, setStep] = useState<Step>('choose')
  const [count, setCount] = useState<string>('')
  const [notes, setNotes] = useState<string>('')
  const [targetBedId, setTargetBedId] = useState<string>('')
  const [submitError, setSubmitError] = useState<string | null>(null)

  useEffect(() => {
    if (entry) {
      setStep('choose')
      setCount(String(entry.count))
      setNotes('')
      setTargetBedId('')
      setSubmitError(null)
    }
  }, [entry])

  const { data: beds } = useQuery({
    queryKey: ['beds'],
    queryFn: () => api.beds.list(),
    enabled: step === 'plant_out',
  })

  const eventTypeFor = (s: Step): string =>
    s === 'pot_up' ? 'POTTED_UP' : s === 'plant_out' ? 'PLANTED_OUT' : 'REMOVED'

  const mut = useMutation({
    mutationFn: () => {
      if (!entry) throw new Error('No entry')
      return api.plants.batchEvent({
        speciesId: entry.speciesId,
        status: entry.status,
        eventType: eventTypeFor(step),
        count: Number(count),
        notes: notes.trim() ? notes.trim() : undefined,
        targetBedId: step === 'plant_out' ? Number(targetBedId) : undefined,
      })
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['tray-summary'] })
      qc.invalidateQueries({ queryKey: ['dashboard'] })
      qc.invalidateQueries({ queryKey: ['beds'] })
      qc.invalidateQueries({ queryKey: ['plants'] })
      onClose()
    },
    onError: (err: unknown) => {
      setSubmitError(err instanceof Error ? err.message : String(err))
    },
  })

  if (!entry) {
    return <Dialog open={open} onClose={onClose} title="">{null}</Dialog>
  }

  const species = formatSpecies(entry)
  const max = entry.count

  const countNum = Number(count)
  const countValid = Number.isInteger(countNum) && countNum >= 1 && countNum <= max
  const targetBedValid = step !== 'plant_out' || (targetBedId !== '' && Number(targetBedId) > 0)
  const canSubmit = step !== 'choose' && countValid && targetBedValid && !mut.isPending

  const titleKey =
    step === 'choose' ? 'dashboard.trays.action.chooseAction'
    : step === 'pot_up' ? 'dashboard.trays.action.potUpTitle'
    : step === 'plant_out' ? 'dashboard.trays.action.plantOutTitle'
    : 'dashboard.trays.action.discardTitle'

  return (
    <Dialog
      open={open}
      onClose={onClose}
      title={t(titleKey, { species })}
      actions={
        step === 'choose' ? (
          <button onClick={onClose} className="px-4 py-2 text-sm text-text-secondary">
            {t('common.cancel')}
          </button>
        ) : (
          <>
            <button
              onClick={() => setStep('choose')}
              className="px-4 py-2 text-sm text-text-secondary"
            >
              ← {t('common.back')}
            </button>
            <button onClick={onClose} className="px-4 py-2 text-sm text-text-secondary">
              {t('common.cancel')}
            </button>
            <button
              onClick={() => { setSubmitError(null); mut.mutate() }}
              disabled={!canSubmit}
              className="btn-primary text-sm"
            >
              {mut.isPending ? t('common.saving') : t('common.save')}
            </button>
          </>
        )
      }
    >
      {step === 'choose' && (
        <div className="flex flex-col gap-2">
          {entry.status === 'SEEDED' && (
            <button
              onClick={() => setStep('pot_up')}
              className="btn-secondary w-full text-left"
            >
              {t('dashboard.trays.action.potUp')}
            </button>
          )}
          {(entry.status === 'SEEDED' || entry.status === 'POTTED_UP') && (
            <button
              onClick={() => setStep('plant_out')}
              className="btn-secondary w-full text-left"
            >
              {t('dashboard.trays.action.plantOut')}
            </button>
          )}
          <button
            onClick={() => setStep('discard')}
            className="btn-secondary w-full text-left"
          >
            {t('dashboard.trays.action.discard')}
          </button>
        </div>
      )}

      {step !== 'choose' && (
        <div className="space-y-4">
          {step === 'discard' && (
            <p className="text-text-secondary text-sm">
              {t('dashboard.trays.action.discardConfirm', { count: max, species })}
            </p>
          )}

          <div>
            <label className="field-label">{t('dashboard.trays.action.count')}</label>
            <input
              type="number"
              min={1}
              max={max}
              value={count}
              onChange={(e) => setCount(e.target.value)}
              className="input"
            />
            {!countValid && count !== '' && (
              <p className="text-xs text-error mt-1">
                {t('dashboard.trays.action.countError', { max })}
              </p>
            )}
          </div>

          {step === 'plant_out' && (
            <div>
              <label className="field-label">{t('dashboard.trays.action.targetBed')}</label>
              <select
                value={targetBedId}
                onChange={(e) => setTargetBedId(e.target.value)}
                className="input"
              >
                <option value="">{t('dashboard.trays.action.selectBed')}</option>
                {(beds ?? []).map((b) => (
                  <option key={b.id} value={b.id}>
                    {b.gardenName ? `${b.gardenName} · ${b.name}` : b.name}
                  </option>
                ))}
              </select>
              {!targetBedValid && targetBedId === '' && (
                <p className="text-xs text-error mt-1">
                  {t('dashboard.trays.action.targetBedRequired')}
                </p>
              )}
            </div>
          )}

          <div>
            <label className="field-label">{t('dashboard.trays.action.notes')}</label>
            <textarea
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              placeholder={t('common.optional')}
              rows={2}
              className="input"
            />
          </div>

          {submitError && (
            <p className="text-sm text-error">{submitError}</p>
          )}
        </div>
      )}
    </Dialog>
  )
}
