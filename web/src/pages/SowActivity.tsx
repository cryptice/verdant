import { useState, useEffect } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { api, type SpeciesResponse } from '../api/client'
import { PageHeader } from '../components/PageHeader'
import { SpeciesAutocomplete } from '../components/SpeciesAutocomplete'
import type { BreadcrumbItem } from '../components/Breadcrumb'

export function SowActivity() {
  const navigate = useNavigate()
  const qc = useQueryClient()
  const { t, i18n } = useTranslation()
  const [params] = useSearchParams()
  const presetBedId = params.get('bedId') ? Number(params.get('bedId')) : null
  const presetSpeciesId = params.get('speciesId') ? Number(params.get('speciesId')) : null
  const taskId = params.get('taskId') ? Number(params.get('taskId')) : null

  const { data: beds } = useQuery({ queryKey: ['beds'], queryFn: api.beds.list })
  const { data: task } = useQuery({
    queryKey: ['task', taskId],
    queryFn: () => api.tasks.get(taskId!),
    enabled: !!taskId,
  })

  const { data: sowBed } = useQuery({
    queryKey: ['bed', presetBedId],
    queryFn: () => api.beds.get(presetBedId!),
    enabled: !!presetBedId,
  })

  const { data: sowGarden } = useQuery({
    queryKey: ['garden', sowBed?.gardenId],
    queryFn: () => api.gardens.get(sowBed!.gardenId),
    enabled: !!sowBed,
  })

  // Fetch preset species (from URL param or task)
  const presetId = presetSpeciesId ?? (task?.speciesId ?? null)
  const { data: presetSpecies } = useQuery({
    queryKey: ['species-by-id', presetId],
    queryFn: () => api.species.search(String(presetId), 1).then(list => list.find(s => s.id === presetId) ?? null),
    enabled: !!presetId,
  })

  const [selectedSpecies, setSelectedSpecies] = useState<SpeciesResponse | null>(null)
  const [bedId, setBedId] = useState(presetBedId ? String(presetBedId) : '')
  const [sowInTray, setSowInTray] = useState(false)
  const [seedCount, setSeedCount] = useState('')
  const [notes, setNotes] = useState('')

  useEffect(() => {
    if (presetSpecies && !selectedSpecies) setSelectedSpecies(presetSpecies)
  }, [presetSpecies, selectedSpecies])

  useEffect(() => {
    if (task && !seedCount) setSeedCount(String(task.remainingCount))
  }, [task, seedCount])

  const speciesId = selectedSpecies?.id ? String(selectedSpecies.id) : ''

  const { data: seedBatches } = useQuery({
    queryKey: ['seed-batches', speciesId],
    queryFn: () => api.inventory.list(Number(speciesId)),
    enabled: !!speciesId,
    select: (items) => items.filter(i => i.quantity > 0),
  })
  const [seedBatchId, setSeedBatchId] = useState('')

  const sowMut = useMutation({
    mutationFn: async () => {
      const lang = i18n.language
      const name = selectedSpecies
        ? ((lang === 'sv' ? selectedSpecies.commonNameSv ?? selectedSpecies.commonName : selectedSpecies.commonName) +
           (selectedSpecies.variantName ? ` — ${lang === 'sv' ? selectedSpecies.variantNameSv ?? selectedSpecies.variantName : selectedSpecies.variantName}` : ''))
        : ''
      const count = Number(seedCount)
      await api.plants.batchSow({
        bedId: sowInTray ? undefined : Number(bedId),
        speciesId: Number(speciesId),
        name,
        seedCount: count,
        notes: notes || undefined,
      })
      if (seedBatchId && count > 0) {
        await api.inventory.decrement(Number(seedBatchId), count)
      }
      if (notes) {
        await api.comments.record(notes)
      }
      if (taskId && count > 0) {
        await api.tasks.complete(taskId, count)
      }
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['bed-plants'] })
      qc.invalidateQueries({ queryKey: ['dashboard'] })
      qc.invalidateQueries({ queryKey: ['tasks'] })
      qc.invalidateQueries({ queryKey: ['seed-inventory'] })
      navigate(-1)
    },
  })

  const valid = speciesId && (sowInTray || bedId) && Number(seedCount) > 0

  const breadcrumbs: BreadcrumbItem[] = taskId
    ? [{ label: t('nav.tasks'), to: '/tasks' }]
    : sowGarden && sowBed
      ? [{ label: t('nav.myWorld'), to: '/' }, { label: sowGarden.name, to: `/garden/${sowGarden.id}` }, { label: sowBed.name, to: `/bed/${sowBed.id}` }]
      : [{ label: t('nav.myWorld'), to: '/' }]

  return (
    <div className="max-w-lg">
      <PageHeader title={t('sow.title')} breadcrumbs={breadcrumbs} />
      <div className="form-card">

        <div>
          <label className="field-label">{t('common.speciesLabel')}</label>
          <SpeciesAutocomplete
            value={selectedSpecies}
            onChange={s => { setSelectedSpecies(s); setSeedBatchId('') }}
          />
        </div>

        {speciesId && seedBatches && seedBatches.length > 0 && (
          <div>
            <label className="field-label">{t('sow.seedBatch')}</label>
            <select value={seedBatchId} onChange={e => setSeedBatchId(e.target.value)} className="input">
              <option value="">{t('sow.seedBatchNone')}</option>
              {seedBatches.map(b => (
                <option key={b.id} value={b.id}>
                  {t('seeds.seedCount', { count: b.quantity })}{b.collectionDate ? ` · ${b.collectionDate}` : ''}
                </option>
              ))}
            </select>
          </div>
        )}

        {!presetBedId && (
          <div>
            <label className="field-label">{t('sow.destination')}</label>
            <div className="flex gap-2">
              <button
                type="button"
                onClick={() => { setSowInTray(false) }}
                className={`flex-1 py-2 rounded-md border text-sm font-medium transition-colors ${!sowInTray ? 'border-accent bg-accent-light text-accent' : 'border-divider bg-surface text-text-secondary hover:bg-divider'}`}
              >
                {t('sow.bed')}
              </button>
              <button
                type="button"
                onClick={() => { setSowInTray(true); setBedId('') }}
                className={`flex-1 py-2 rounded-md border text-sm font-medium transition-colors ${sowInTray ? 'border-accent bg-accent-light text-accent' : 'border-divider bg-surface text-text-secondary hover:bg-divider'}`}
              >
                {t('sow.tray')}
              </button>
            </div>
          </div>
        )}

        {!sowInTray && (
          <div>
            <label className="field-label">{t('sow.bedLabel')}</label>
            <select value={bedId} onChange={e => setBedId(e.target.value)} className="input">
              <option value="">{t('sow.selectBed')}</option>
              {beds?.map(b => <option key={b.id} value={b.id}>{b.gardenName} — {b.name}</option>)}
            </select>
          </div>
        )}

        <div>
          <label className="field-label">{t('sow.seedCount')}</label>
          <input type="number" value={seedCount} onChange={e => setSeedCount(e.target.value)} placeholder="e.g. 12" className="input" />
        </div>

        <div>
          <label className="field-label">{t('common.notesLabel')}</label>
          <textarea value={notes} onChange={e => setNotes(e.target.value)} placeholder={t('common.optional')} rows={2} className="input" />
        </div>

      </div>

      {sowMut.error && <p className="text-error text-sm mt-3">{sowMut.error instanceof Error ? sowMut.error.message : String(sowMut.error)}</p>}
      <div className="mt-4 flex justify-end">
        <button onClick={() => sowMut.mutate()} disabled={!valid || sowMut.isPending} className="btn-primary">
          {sowMut.isPending ? t('sow.sowing') : t('sow.sow')}
        </button>
      </div>
    </div>
  )
}
