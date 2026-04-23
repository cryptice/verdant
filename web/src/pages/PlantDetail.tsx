import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useParams, useNavigate } from 'react-router-dom'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { api } from '../api/client'
import { Masthead, Chip, Rule } from '../components/faltet'
import { ErrorDisplay } from '../components/ErrorDisplay'
import { Dialog } from '../components/Dialog'
import { useOnboarding } from '../onboarding/OnboardingContext'

const eventIcons: Record<string, string> = {
  SEEDED: '🌰', POTTED_UP: '🪴', PLANTED_OUT: '🌳', GROWING: '🌿',
  HARVESTED: '🌾', RECOVERED: '💚', REMOVED: '🗑️', NOTE: '📝',
  BUDDING: '🌼', FIRST_BLOOM: '🌸', PEAK_BLOOM: '💐', LAST_BLOOM: '🥀',
  LIFTED: '⛏️', DIVIDED: '✂️', STORED: '📦', PINCHED: '🤏', DISBUDDED: '✂️',
  APPLIED_SUPPLY: '💧',
}

export function PlantDetail() {
  const { id } = useParams<{ id: string }>()
  const plantId = Number(id)
  const navigate = useNavigate()
  const qc = useQueryClient()
  const { t } = useTranslation()
  const { completeStep } = useOnboarding()

  const { data: plant, error, isLoading, refetch } = useQuery({
    queryKey: ['plant', plantId],
    queryFn: () => api.plants.get(plantId),
  })

  const { data: events } = useQuery({
    queryKey: ['plant-events', plantId],
    queryFn: () => api.plants.events(plantId),
  })

  const { data: bed } = useQuery({
    queryKey: ['bed', plant?.bedId],
    queryFn: () => api.beds.get(plant!.bedId!),
    enabled: !!plant?.bedId,
  })

  const { data: customers } = useQuery({
    queryKey: ['customers'],
    queryFn: () => api.customers.list(),
  })

  const [showDelete, setShowDelete] = useState(false)
  const [showAddEvent, setShowAddEvent] = useState(false)
  const [eventType, setEventType] = useState('NOTE')
  const [eventNotes, setEventNotes] = useState('')
  const [eventCount, setEventCount] = useState('')
  const [eventWeight, setEventWeight] = useState('')
  const [eventStemCount, setEventStemCount] = useState('')
  const [eventStemLength, setEventStemLength] = useState('')
  const [eventQuality, setEventQuality] = useState('')
  const [eventDestination, setEventDestination] = useState('')
  const [deleteEventId, setDeleteEventId] = useState<number | null>(null)

  const deletePlantMut = useMutation({
    mutationFn: () => api.plants.delete(plantId),
    onSuccess: () => { navigate(-1); qc.invalidateQueries({ queryKey: ['bed-plants'] }) },
  })

  const addEventMut = useMutation({
    mutationFn: () => api.plants.addEvent(plantId, {
      eventType,
      eventDate: new Date().toISOString().split('T')[0],
      notes: eventNotes || undefined,
      plantCount: eventCount ? Number(eventCount) : undefined,
      weightGrams: eventWeight ? Number(eventWeight) : undefined,
      stemCount: eventStemCount ? Number(eventStemCount) : undefined,
      stemLengthCm: eventStemLength ? Number(eventStemLength) : undefined,
      qualityGrade: eventQuality || undefined,
      harvestDestinationId: eventDestination ? Number(eventDestination) : undefined,
    }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['plant-events', plantId] })
      qc.invalidateQueries({ queryKey: ['plant', plantId] })
      if (eventType === 'POTTED_UP') completeStep('pot_up')
      else if (eventType === 'PLANTED_OUT') completeStep('plant_out')
      else if (eventType === 'HARVESTED') completeStep('record_harvest')
      setShowAddEvent(false)
      setEventNotes(''); setEventCount(''); setEventWeight('')
      setEventStemCount(''); setEventStemLength(''); setEventQuality(''); setEventDestination('')
    },
  })

  const deleteEventMut = useMutation({
    mutationFn: (eventId: number) => api.plants.deleteEvent(plantId, eventId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['plant-events', plantId] })
      qc.invalidateQueries({ queryKey: ['plant', plantId] })
      setDeleteEventId(null)
    },
  })

  if (isLoading) return (
    <div className="flex justify-center p-16">
      <div className="animate-spin h-8 w-8 border-2 border-accent border-t-transparent rounded-full" />
    </div>
  )
  if (error) return <ErrorDisplay error={error} onRetry={refetch} />
  if (!plant) return null

  const eventTypes = [
    'SEEDED', 'POTTED_UP', 'PLANTED_OUT', 'HARVESTED', 'RECOVERED', 'REMOVED', 'NOTE',
    'BUDDING', 'FIRST_BLOOM', 'PEAK_BLOOM', 'LAST_BLOOM',
    'LIFTED', 'DIVIDED', 'STORED', 'PINCHED', 'DISBUDDED',
  ]
  const showCount = ['SEEDED', 'POTTED_UP', 'PLANTED_OUT'].includes(eventType)
  const showWeight = eventType === 'HARVESTED'
  const showHarvestFields = eventType === 'HARVESTED'

  const plantDisplayName = plant.name ?? plant.speciesName ?? t('plant.untitled')
  const speciesLabel = plant.speciesName ?? ''
  const bedLabel = bed?.name

  return (
    <div>
      <Masthead
        left={
          <span>
            {t('nav.plants')}
            {speciesLabel && (
              <> / {speciesLabel}</>
            )}
            {' '}/ <span style={{ color: 'var(--color-accent)' }}>{plantDisplayName}</span>
          </span>
        }
      />

      <div style={{ padding: '28px 40px' }}>
        {/* Hero */}
        <div style={{ display: 'grid', gridTemplateColumns: '1.2fr 1fr', gap: 40, alignItems: 'start' }}>
          <div>
            {/* Status + bed chips */}
            <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap', marginBottom: 14 }}>
              {plant.status && (
                <Chip tone="sage">{t(`status.${plant.status}`, { defaultValue: plant.status })}</Chip>
              )}
              {bedLabel && <Chip tone="mustard">{bedLabel}</Chip>}
            </div>

            <h1
              style={{
                fontFamily: 'var(--font-display)',
                fontSize: 60,
                fontWeight: 300,
                letterSpacing: -1,
                margin: 0,
                fontVariationSettings: '"SOFT" 100, "opsz" 144',
              }}
            >
              {plantDisplayName}<span style={{ color: 'var(--color-accent)' }}>.</span>
            </h1>

            {plant.speciesName && plant.name && plant.name !== plant.speciesName && (
              <p
                style={{
                  fontFamily: 'var(--font-display)',
                  fontStyle: 'italic',
                  fontSize: 18,
                  color: 'var(--color-accent)',
                  marginTop: 6,
                }}
              >
                {plant.speciesName}
              </p>
            )}
          </div>
        </div>

        {/* Event timeline section */}
        <div style={{ marginTop: 40, display: 'flex', alignItems: 'baseline', gap: 12 }}>
          <h2
            style={{
              fontFamily: 'var(--font-display)',
              fontStyle: 'italic',
              fontSize: 30,
              fontWeight: 300,
              margin: 0,
              fontVariationSettings: '"SOFT" 100, "opsz" 144',
            }}
          >
            {t('plant.events')}<span style={{ color: 'var(--color-accent)' }}>.</span>
          </h2>
          <Rule inline variant="ink" />
          <button onClick={() => setShowAddEvent(true)} className="btn-secondary">
            + {t('plant.addEvent')}
          </button>
          <button
            onClick={() => setShowDelete(true)}
            style={{
              background: 'transparent',
              border: 'none',
              fontFamily: 'var(--font-mono)',
              fontSize: 10,
              letterSpacing: 1.4,
              textTransform: 'uppercase',
              color: 'var(--color-accent)',
              cursor: 'pointer',
              marginLeft: 'auto',
            }}
          >
            → {t('plant.deletePlant')}
          </button>
        </div>

        {events && events.length === 0 && (
          <p
            style={{
              fontFamily: 'var(--font-display)',
              fontStyle: 'italic',
              color: 'var(--color-forest)',
              marginTop: 14,
            }}
          >
            {t('plant.noEventsYet')}
          </p>
        )}

        <div style={{ marginTop: 14 }}>
          {[...(events ?? [])].reverse().map((ev) => (
            <div
              key={ev.id}
              style={{
                padding: '14px 0',
                borderBottom: '1px solid color-mix(in srgb, var(--color-ink) 20%, transparent)',
              }}
            >
              <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                  <span style={{ fontSize: 20 }}>{eventIcons[ev.eventType] ?? '📌'}</span>
                  <span
                    style={{
                      fontFamily: 'var(--font-display)',
                      fontSize: 18,
                      fontWeight: 300,
                    }}
                  >
                    {t(`eventType.${ev.eventType}`, { defaultValue: ev.eventType.replace(/_/g, ' ') })}
                  </span>
                  {ev.eventType === 'APPLIED_SUPPLY' && ev.notes && (
                    <span
                      style={{
                        fontFamily: 'var(--font-mono)',
                        fontSize: 10,
                        letterSpacing: 1.4,
                        textTransform: 'uppercase',
                        color: 'var(--color-forest)',
                        opacity: 0.7,
                      }}
                    >
                      {ev.notes}
                    </span>
                  )}
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
                  <span
                    style={{
                      fontFamily: 'var(--font-mono)',
                      fontSize: 10,
                      letterSpacing: 1.4,
                      color: 'var(--color-forest)',
                      opacity: 0.7,
                    }}
                  >
                    {ev.eventDate}
                  </span>
                  <button
                    onClick={() => setDeleteEventId(ev.id)}
                    style={{
                      background: 'transparent',
                      border: 'none',
                      fontFamily: 'var(--font-mono)',
                      fontSize: 10,
                      letterSpacing: 1.4,
                      textTransform: 'uppercase',
                      color: 'var(--color-accent)',
                      cursor: 'pointer',
                      padding: 0,
                    }}
                  >
                    {t('common.delete')}
                  </button>
                </div>
              </div>

              <div
                style={{
                  marginTop: 6,
                  paddingLeft: 30,
                  fontFamily: 'var(--font-display)',
                  fontSize: 15,
                  color: 'var(--color-forest)',
                  display: 'flex',
                  flexDirection: 'column',
                  gap: 2,
                }}
              >
                {ev.plantCount != null && (
                  <span>{t('plant.countField')} {ev.plantCount}</span>
                )}
                {ev.weightGrams != null && (
                  <span>{t('plant.weightField')} {ev.weightGrams}g</span>
                )}
                {ev.quantity != null && ev.eventType === 'APPLIED_SUPPLY' && (
                  <span>{t('plant.quantityField')} {ev.quantity}</span>
                )}
                {ev.stemCount != null && (
                  <span>{t('plant.stemCount')}: {ev.stemCount}</span>
                )}
                {ev.stemLengthCm != null && (
                  <span>{t('plant.stemLength')}: {ev.stemLengthCm} cm</span>
                )}
                {ev.qualityGrade && (
                  <span>{t('plant.qualityGrade')}: {ev.qualityGrade}</span>
                )}
                {ev.customerName && (
                  <span>{t('plant.destination')}: {ev.customerName}</span>
                )}
                {ev.notes && ev.eventType !== 'APPLIED_SUPPLY' && (
                  <span style={{ fontStyle: 'italic', opacity: 0.8 }}>{ev.notes}</span>
                )}
                {ev.imageUrl && (
                  <img
                    src={ev.imageUrl}
                    alt={t('common.image')}
                    style={{ borderRadius: 6, marginTop: 6, maxHeight: 160, objectFit: 'cover' }}
                  />
                )}
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Add event dialog */}
      <Dialog
        open={showAddEvent}
        onClose={() => setShowAddEvent(false)}
        title={t('plant.addEventTitle')}
        actions={
          <>
            <button onClick={() => setShowAddEvent(false)} className="px-4 py-2 text-sm text-text-secondary">{t('common.cancel')}</button>
            <button onClick={() => addEventMut.mutate()} disabled={addEventMut.isPending} className="btn-primary text-sm">
              {addEventMut.isPending ? t('common.saving') : t('common.save')}
            </button>
          </>
        }
      >
        <div className="space-y-4">
          <div>
            <label className="field-label">{t('plant.eventTypeLabel')}</label>
            <select value={eventType} onChange={e => setEventType(e.target.value)} className="input">
              {eventTypes.map(tp => (
                <option key={tp} value={tp}>{t(`eventType.${tp}`, { defaultValue: tp.replace(/_/g, ' ') })}</option>
              ))}
            </select>
          </div>
          {showCount && (
            <div>
              <label className="field-label">{t('plant.countLabel')}</label>
              <input type="number" value={eventCount} onChange={e => setEventCount(e.target.value)} placeholder="e.g. 3" className="input" />
            </div>
          )}
          {showWeight && (
            <div>
              <label className="field-label">{t('plant.weightGrams')}</label>
              <input type="number" value={eventWeight} onChange={e => setEventWeight(e.target.value)} placeholder="e.g. 250" className="input" />
            </div>
          )}
          {showHarvestFields && (
            <>
              <div>
                <label className="field-label">{t('plant.stemCount')}</label>
                <input type="number" value={eventStemCount} onChange={e => setEventStemCount(e.target.value)} placeholder="e.g. 10" className="input" />
              </div>
              <div>
                <label className="field-label">{t('plant.stemLength')}</label>
                <input type="number" value={eventStemLength} onChange={e => setEventStemLength(e.target.value)} placeholder="e.g. 45" className="input" />
              </div>
              <div>
                <label className="field-label">{t('plant.qualityGrade')}</label>
                <div className="flex gap-2">
                  {['A', 'B', 'C'].map(grade => (
                    <button
                      key={grade}
                      type="button"
                      onClick={() => setEventQuality(eventQuality === grade ? '' : grade)}
                      className={`flex-1 py-2 rounded-lg text-sm font-medium border transition-colors ${
                        eventQuality === grade
                          ? 'bg-accent text-white border-accent'
                          : 'bg-surface border-divider text-text-secondary hover:bg-white/60'
                      }`}
                    >
                      {grade}
                    </button>
                  ))}
                </div>
              </div>
              <div>
                <label className="field-label">{t('plant.destination')}</label>
                <select value={eventDestination} onChange={e => setEventDestination(e.target.value)} className="input">
                  <option value="">{t('plant.noneSelected')}</option>
                  {(customers ?? []).map(c => (
                    <option key={c.id} value={c.id}>{c.name}</option>
                  ))}
                </select>
              </div>
            </>
          )}
          <div>
            <label className="field-label">{t('common.notesLabel')}</label>
            <textarea value={eventNotes} onChange={e => setEventNotes(e.target.value)} placeholder={t('common.optional')} rows={2} className="input" />
          </div>
        </div>
      </Dialog>

      {/* Delete plant dialog */}
      <Dialog
        open={showDelete}
        onClose={() => setShowDelete(false)}
        title={t('plant.deletePlantTitle')}
        actions={
          <>
            <button onClick={() => setShowDelete(false)} className="px-4 py-2 text-sm text-text-secondary">{t('common.cancel')}</button>
            <button onClick={() => deletePlantMut.mutate()} className="px-4 py-2 text-sm text-error font-semibold">{t('common.delete')}</button>
          </>
        }
      >
        <p className="text-text-secondary">{t('plant.deletePlantConfirm')}</p>
      </Dialog>

      {/* Delete event dialog */}
      <Dialog
        open={deleteEventId !== null}
        onClose={() => setDeleteEventId(null)}
        title={t('plant.deleteEventTitle')}
        actions={
          <>
            <button onClick={() => setDeleteEventId(null)} className="px-4 py-2 text-sm text-text-secondary">{t('common.cancel')}</button>
            <button
              onClick={() => deleteEventId && deleteEventMut.mutate(deleteEventId)}
              className="px-4 py-2 text-sm text-error font-semibold"
            >
              {t('common.delete')}
            </button>
          </>
        }
      >
        <p className="text-text-secondary">{t('plant.deleteEventConfirm')}</p>
      </Dialog>
    </div>
  )
}
