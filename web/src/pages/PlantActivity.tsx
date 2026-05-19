import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { api } from '../api/client'
import { Masthead } from '../components/faltet'
import { ErrorDisplay } from '../components/ErrorDisplay'

type ActivityKind = 'pot-up' | 'plant-out' | 'harvest' | 'recover' | 'discard'

const KIND_TO_EVENT: Record<ActivityKind, string> = {
  'pot-up':    'POTTED_UP',
  'plant-out': 'PLANTED_OUT',
  'harvest':   'HARVESTED',
  'recover':   'RECOVERED',
  'discard':   'REMOVED',
}

const KIND_TITLE_KEY: Record<ActivityKind, string> = {
  'pot-up':    'activity.potUp',
  'plant-out': 'activity.plantOut',
  'harvest':   'activity.harvest',
  'recover':   'activity.recover',
  'discard':   'activity.discard',
}

export function PlantActivity() {
  const { plantId: plantIdStr, kind: kindStr } = useParams<{ plantId: string; kind: ActivityKind }>()
  const plantId = Number(plantIdStr)
  const kind = kindStr as ActivityKind
  const navigate = useNavigate()
  const qc = useQueryClient()
  const { t } = useTranslation()

  const isHarvest = kind === 'harvest'

  const plantQuery = useQuery({
    queryKey: ['plant', plantId],
    queryFn: () => api.plants.get(plantId),
    enabled: Number.isFinite(plantId),
  })

  const commentsQuery = useQuery({
    queryKey: ['frequent-comments'],
    queryFn: () => api.comments.list(),
  })

  const [count, setCount] = useState('')
  const [weightG, setWeightG] = useState('')
  const [stemCount, setStemCount] = useState('')
  const [notes, setNotes] = useState('')
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    setError(null)
  }, [plantId, kind])

  const submitMut = useMutation({
    mutationFn: () => api.plants.addEvent(plantId, {
      eventType: KIND_TO_EVENT[kind],
      eventDate: new Date().toISOString().slice(0, 10),
      plantCount: count ? Number(count) : undefined,
      weightGrams: isHarvest && weightG ? parseFloat(weightG.replace(',', '.')) : undefined,
      quantity: isHarvest && stemCount ? Number(stemCount) : undefined,
      notes: notes.trim() || undefined,
    }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['plant', plantId] })
      qc.invalidateQueries({ queryKey: ['plants'] })
      qc.invalidateQueries({ queryKey: ['plant-events', plantId] })
      if (notes.trim()) {
        api.comments.record(notes.trim()).catch(() => { /* best-effort */ })
      }
      navigate(`/plant/${plantId}`)
    },
    onError: (err) => setError(err instanceof Error ? err.message : String(err)),
  })

  if (!plantIdStr || !kindStr || !(kindStr in KIND_TO_EVENT)) {
    return <div className="page-body"><p>Unknown activity.</p></div>
  }
  if (plantQuery.isLoading) {
    return (
      <div className="flex justify-center p-16">
        <div className="animate-spin h-8 w-8 border-2 border-accent border-t-transparent rounded-full" />
      </div>
    )
  }
  if (plantQuery.error) return <ErrorDisplay error={plantQuery.error} onRetry={plantQuery.refetch} />

  const plant = plantQuery.data
  if (!plant) return null

  const countNum = parseInt(count, 10)
  const valid = Number.isFinite(countNum) && countNum >= 1
  const suggestions = (commentsQuery.data ?? []).map((c) => c.text).slice(0, 8)

  return (
    <div>
      <Masthead
        left={
          <Link to={`/plant/${plantId}`} className="text-sm text-text-secondary hover:text-accent">
            ← {plant.name}
          </Link>
        }
        center={t(KIND_TITLE_KEY[kind])}
      />

      <div className="page-body">
        <div className="space-y-4 max-w-lg">
          <div>
            <label className="field-label">{t('activity.countLabel')} *</label>
            <input
              type="number"
              min={1}
              value={count}
              onChange={(e) => setCount(e.target.value.replace(/[^\d]/g, ''))}
              className="input"
              autoFocus
            />
          </div>

          {isHarvest && (
            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="field-label">{t('activity.weightGrams')}</label>
                <input
                  type="text"
                  inputMode="decimal"
                  value={weightG}
                  onChange={(e) => setWeightG(e.target.value.replace(/[^\d.,]/g, ''))}
                  placeholder={t('common.optional')}
                  className="input"
                />
              </div>
              <div>
                <label className="field-label">{t('activity.stemCount')}</label>
                <input
                  type="number"
                  min={0}
                  value={stemCount}
                  onChange={(e) => setStemCount(e.target.value.replace(/[^\d]/g, ''))}
                  placeholder={t('common.optional')}
                  className="input"
                />
              </div>
            </div>
          )}

          <div>
            <label className="field-label">{t('common.notesLabel')}</label>
            <textarea
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              rows={3}
              placeholder={t('common.optional')}
              className="input"
            />
            {suggestions.length > 0 && (
              <div className="flex flex-wrap gap-2 mt-2">
                {suggestions.map((s) => (
                  <button
                    key={s}
                    type="button"
                    onClick={() => setNotes(s)}
                    className="px-2 py-1 rounded-full border border-divider text-xs text-text-secondary hover:bg-surface"
                  >
                    {s}
                  </button>
                ))}
              </div>
            )}
          </div>

          {error && <p className="text-error text-sm">{error}</p>}

          <div className="flex gap-2 pt-2">
            <button
              onClick={() => navigate(`/plant/${plantId}`)}
              className="px-4 py-2 text-sm text-text-secondary"
            >
              {t('common.cancel')}
            </button>
            <button
              onClick={() => submitMut.mutate()}
              disabled={!valid || submitMut.isPending}
              className="btn-primary text-sm"
            >
              {submitMut.isPending ? t('common.saving') : t(KIND_TITLE_KEY[kind])}
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}
