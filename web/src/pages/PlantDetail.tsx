import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useParams, useNavigate } from 'react-router-dom'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { api } from '../api/client'
import type { BreadcrumbItem } from '../components/Breadcrumb'
import { PageHeader } from '../components/PageHeader'
import { ErrorDisplay } from '../components/ErrorDisplay'
import { StatusBadge } from '../components/StatusBadge'
import { Dialog } from '../components/Dialog'

const eventIcons: Record<string, string> = {
  SEEDED: '🌰', POTTED_UP: '🪴', PLANTED_OUT: '🌳', GROWING: '🌿',
  HARVESTED: '🌾', RECOVERED: '💚', REMOVED: '🗑️', NOTE: '📝',
}

export function PlantDetail() {
  const { id } = useParams<{ id: string }>()
  const plantId = Number(id)
  const navigate = useNavigate()
  const qc = useQueryClient()
  const { t } = useTranslation()

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

  const { data: garden } = useQuery({
    queryKey: ['garden', bed?.gardenId],
    queryFn: () => api.gardens.get(bed!.gardenId),
    enabled: !!bed,
  })

  const [showDelete, setShowDelete] = useState(false)
  const [showAddEvent, setShowAddEvent] = useState(false)
  const [eventType, setEventType] = useState('NOTE')
  const [eventNotes, setEventNotes] = useState('')
  const [eventCount, setEventCount] = useState('')
  const [eventWeight, setEventWeight] = useState('')
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
    }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['plant-events', plantId] })
      qc.invalidateQueries({ queryKey: ['plant', plantId] })
      setShowAddEvent(false); setEventNotes(''); setEventCount(''); setEventWeight('')
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

  if (isLoading) return <div className="flex justify-center p-16"><div className="animate-spin h-8 w-8 border-2 border-accent border-t-transparent rounded-full" /></div>
  if (error) return <ErrorDisplay error={error} onRetry={refetch} />
  if (!plant) return null

  const breadcrumbs: BreadcrumbItem[] = [{ label: t('nav.myWorld'), to: '/' }]
  if (garden && bed) {
    breadcrumbs.push({ label: garden.name, to: `/garden/${garden.id}` })
    breadcrumbs.push({ label: bed.name, to: `/bed/${bed.id}` })
  } else if (garden) {
    breadcrumbs.push({ label: garden.name, to: `/garden/${garden.id}` })
  }

  const eventTypes = ['SEEDED', 'POTTED_UP', 'PLANTED_OUT', 'HARVESTED', 'RECOVERED', 'REMOVED', 'NOTE']
  const showCount = ['SEEDED', 'POTTED_UP', 'PLANTED_OUT'].includes(eventType)
  const showWeight = eventType === 'HARVESTED'

  return (
    <div>
      <PageHeader title={plant.name} breadcrumbs={breadcrumbs} />

      <div className="px-4 py-4 space-y-4">
        <div className="card">
          <p className="font-bold text-xl">{plant.name}</p>
          {plant.speciesName && <p className="text-text-secondary mt-1">{plant.speciesName}</p>}
          <div className="flex gap-2 mt-3 flex-wrap">
            <StatusBadge status={plant.status} />
            {plant.seedCount != null && (
              <span className="inline-block px-2.5 py-0.5 rounded-full text-xs font-medium bg-surface text-text-secondary">
                {t('plant.seedCount', { count: plant.seedCount })}
              </span>
            )}
            {plant.survivingCount != null && (
              <span className="inline-block px-2.5 py-0.5 rounded-full text-xs font-medium bg-surface text-text-secondary">
                {t('plant.aliveCount', { count: plant.survivingCount })}
              </span>
            )}
          </div>
        </div>

        <div className="flex items-center justify-between">
          <h2 className="font-bold text-lg">{t('plant.timeline')}</h2>
          <button onClick={() => setShowDelete(true)} className="text-error text-sm font-medium">{t('plant.deletePlant')}</button>
        </div>

        {events && events.length === 0 && (
          <div className="card text-center py-4">
            <p className="text-text-secondary text-sm">{t('plant.noEventsYet')}</p>
          </div>
        )}

        {[...(events ?? [])].reverse().map(ev => (
          <div key={ev.id} className="card">
            <div className="flex items-start justify-between">
              <div className="flex items-center gap-2">
                <span>{eventIcons[ev.eventType] ?? '📌'}</span>
                <span className="font-medium text-sm">{t(`eventType.${ev.eventType}`, { defaultValue: ev.eventType.replace(/_/g, ' ') })}</span>
              </div>
              <div className="flex items-center gap-3">
                <span className="text-xs text-text-secondary">{ev.eventDate}</span>
                <button onClick={() => setDeleteEventId(ev.id)} className="text-error text-xs">{t('common.delete')}</button>
              </div>
            </div>
            <div className="mt-2 text-sm space-y-1">
              {ev.plantCount != null && <p>{t('plant.countField')} {ev.plantCount}</p>}
              {ev.weightGrams != null && <p>{t('plant.weightField')} {ev.weightGrams}g</p>}
              {ev.quantity != null && <p>{t('plant.quantityField')} {ev.quantity}</p>}
              {ev.notes && <p className="text-text-secondary">{ev.notes}</p>}
              {ev.imageUrl && <img src={ev.imageUrl} alt="" className="rounded-lg mt-2 max-h-40 object-cover" />}
            </div>
          </div>
        ))}

        <button onClick={() => setShowAddEvent(true)} className="btn-primary w-full">{t('plant.addEvent')}</button>
      </div>

      <Dialog open={showAddEvent} onClose={() => setShowAddEvent(false)} title={t('plant.addEventTitle')} actions={
        <>
          <button onClick={() => setShowAddEvent(false)} className="px-4 py-2 text-sm text-text-secondary">{t('common.cancel')}</button>
          <button onClick={() => addEventMut.mutate()} disabled={addEventMut.isPending} className="btn-primary text-sm">
            {addEventMut.isPending ? t('common.saving') : t('common.save')}
          </button>
        </>
      }>
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
          <div>
            <label className="field-label">{t('common.notesLabel')}</label>
            <textarea value={eventNotes} onChange={e => setEventNotes(e.target.value)} placeholder={t('common.optional')} rows={2} className="input" />
          </div>
        </div>
      </Dialog>

      <Dialog open={showDelete} onClose={() => setShowDelete(false)} title={t('plant.deletePlantTitle')} actions={
        <>
          <button onClick={() => setShowDelete(false)} className="px-4 py-2 text-sm text-text-secondary">{t('common.cancel')}</button>
          <button onClick={() => deletePlantMut.mutate()} className="px-4 py-2 text-sm text-error font-semibold">{t('common.delete')}</button>
        </>
      }>
        <p className="text-text-secondary">{t('plant.deletePlantConfirm')}</p>
      </Dialog>

      <Dialog open={deleteEventId !== null} onClose={() => setDeleteEventId(null)} title={t('plant.deleteEventTitle')} actions={
        <>
          <button onClick={() => setDeleteEventId(null)} className="px-4 py-2 text-sm text-text-secondary">{t('common.cancel')}</button>
          <button onClick={() => deleteEventId && deleteEventMut.mutate(deleteEventId)} className="px-4 py-2 text-sm text-error font-semibold">{t('common.delete')}</button>
        </>
      }>
        <p className="text-text-secondary">{t('plant.deleteEventConfirm')}</p>
      </Dialog>
    </div>
  )
}
