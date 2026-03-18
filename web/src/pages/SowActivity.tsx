import { useState, useEffect } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { api } from '../api/client'
import { PageHeader } from '../components/PageHeader'
import type { BreadcrumbItem } from '../components/Breadcrumb'

export function SowActivity() {
  const navigate = useNavigate()
  const qc = useQueryClient()
  const { t, i18n } = useTranslation()
  const [params] = useSearchParams()
  const presetBedId = params.get('bedId') ? Number(params.get('bedId')) : null
  const presetSpeciesId = params.get('speciesId') ? Number(params.get('speciesId')) : null
  const taskId = params.get('taskId') ? Number(params.get('taskId')) : null

  const { data: species } = useQuery({ queryKey: ['species'], queryFn: api.species.list })
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

  const [speciesId, setSpeciesId] = useState(presetSpeciesId ? String(presetSpeciesId) : '')
  const [bedId, setBedId] = useState(presetBedId ? String(presetBedId) : '')
  const [sowInTray, setSowInTray] = useState(false)
  const [seedCount, setSeedCount] = useState('')
  const [notes, setNotes] = useState('')
  const [speciesSearch, setSpeciesSearch] = useState('')

  useEffect(() => {
    if (task && !speciesId) {
      setSpeciesId(String(task.speciesId))
      setSeedCount(String(task.remainingCount))
    }
  }, [task, speciesId])

  const { data: seedBatches } = useQuery({
    queryKey: ['seed-batches', speciesId],
    queryFn: () => api.inventory.list(Number(speciesId)),
    enabled: !!speciesId,
    select: (items) => items.filter(i => i.quantity > 0),
  })
  const [seedBatchId, setSeedBatchId] = useState('')

  const sowMut = useMutation({
    mutationFn: async () => {
      const selectedSpecies = species?.find(s => s.id === Number(speciesId))
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

  const filteredSpecies = species?.filter(s =>
    !speciesSearch ||
    s.commonName.toLowerCase().includes(speciesSearch.toLowerCase()) ||
    s.commonNameSv?.toLowerCase().includes(speciesSearch.toLowerCase()) ||
    s.variantName?.toLowerCase().includes(speciesSearch.toLowerCase()) ||
    s.variantNameSv?.toLowerCase().includes(speciesSearch.toLowerCase())
  ) ?? []

  const selectedSpecies = species?.find(s => s.id === Number(speciesId))
  const selectedSpeciesName = selectedSpecies
    ? (i18n.language === 'sv' ? selectedSpecies.commonNameSv ?? selectedSpecies.commonName : selectedSpecies.commonName)
    : ''
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

        <div className="relative">
          <label className="field-label">{t('common.speciesLabel')}</label>
          <input
            value={speciesSearch || selectedSpeciesName}
            onChange={e => { setSpeciesSearch(e.target.value); setSpeciesId('') }}
            placeholder={t('common.searchSpecies')}
            className="input"
          />
          {speciesSearch && (
            <div className="absolute z-10 left-0 right-0 mt-1 border border-divider rounded-md bg-bg shadow-md max-h-48 overflow-y-auto">
              {filteredSpecies.slice(0, 20).map(s => (
                <button
                  key={s.id}
                  onClick={() => { setSpeciesId(String(s.id)); setSpeciesSearch(''); setSeedBatchId('') }}
                  className="w-full text-left px-3 py-2 text-sm hover:bg-surface transition-colors"
                >
                  {i18n.language === 'sv' ? (s.commonNameSv ?? s.commonName) : s.commonName}
                  {(i18n.language === 'sv' ? (s.variantNameSv ?? s.variantName) : s.variantName)
                    ? ` — ${i18n.language === 'sv' ? (s.variantNameSv ?? s.variantName) : s.variantName}`
                    : ''}
                </button>
              ))}
              {filteredSpecies.length === 0 && (
                <p className="px-3 py-2 text-sm text-text-secondary">{t('species.noSpeciesFoundDropdown')}</p>
              )}
            </div>
          )}
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
