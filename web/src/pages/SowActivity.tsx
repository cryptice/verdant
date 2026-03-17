import { useState, useEffect } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { api } from '../api/client'
import { PageHeader } from '../components/PageHeader'

export function SowActivity() {
  const navigate = useNavigate()
  const qc = useQueryClient()
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
      const name = selectedSpecies
        ? (selectedSpecies.commonName + (selectedSpecies.variantName ? ` — ${selectedSpecies.variantName}` : ''))
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

  const valid = speciesId && (sowInTray || bedId) && Number(seedCount) > 0

  return (
    <div>
      <PageHeader title="Sow" back />
      <div className="px-4 py-4 space-y-4">
        <div>
          <label className="font-bold text-sm block mb-1">Species *</label>
          <input
            value={speciesSearch || species?.find(s => s.id === Number(speciesId))?.commonName || ''}
            onChange={e => { setSpeciesSearch(e.target.value); setSpeciesId('') }}
            placeholder="Search species..."
            className="w-full border border-cream-dark rounded-xl px-3 py-2 bg-cream"
          />
          {speciesSearch && (
            <div className="border border-cream-dark rounded-xl mt-1 max-h-48 overflow-y-auto bg-cream">
              {filteredSpecies.slice(0, 20).map(s => (
                <button
                  key={s.id}
                  onClick={() => { setSpeciesId(String(s.id)); setSpeciesSearch(''); setSeedBatchId('') }}
                  className="w-full text-left px-3 py-2 text-sm hover:bg-cream-dark"
                >
                  {s.commonName}{s.variantName ? ` — ${s.variantName}` : ''}
                </button>
              ))}
              {filteredSpecies.length === 0 && <p className="px-3 py-2 text-sm text-text-secondary">No species found</p>}
            </div>
          )}
        </div>

        {speciesId && seedBatches && seedBatches.length > 0 && (
          <div>
            <label className="font-bold text-sm block mb-1">Seed Batch</label>
            <select value={seedBatchId} onChange={e => setSeedBatchId(e.target.value)} className="w-full border border-cream-dark rounded-xl px-3 py-2 bg-cream">
              <option value="">Select batch (optional)...</option>
              {seedBatches.map(b => (
                <option key={b.id} value={b.id}>
                  {b.quantity} seeds{b.collectionDate ? ` (${b.collectionDate})` : ''}
                </option>
              ))}
            </select>
          </div>
        )}

        {!presetBedId && (
          <div className="flex items-center justify-between">
            <span className="text-sm">Sow in tray</span>
            <button
              onClick={() => { setSowInTray(!sowInTray); if (!sowInTray) setBedId('') }}
              className={`w-12 h-6 rounded-full transition-colors ${sowInTray ? 'bg-green-primary' : 'bg-cream-dark'} relative`}
            >
              <span className={`absolute top-0.5 w-5 h-5 rounded-full bg-white shadow transition-transform ${sowInTray ? 'translate-x-6' : 'translate-x-0.5'}`} />
            </button>
          </div>
        )}

        {!sowInTray && (
          <div>
            <label className="font-bold text-sm block mb-1">Bed *</label>
            <select value={bedId} onChange={e => setBedId(e.target.value)} className="w-full border border-cream-dark rounded-xl px-3 py-2 bg-cream">
              <option value="">Select bed...</option>
              {beds?.map(b => <option key={b.id} value={b.id}>{b.gardenName} — {b.name}</option>)}
            </select>
          </div>
        )}

        <div>
          <label className="font-bold text-sm block mb-1">Seed count *</label>
          <input type="number" value={seedCount} onChange={e => setSeedCount(e.target.value)} placeholder="Number of seeds" className="w-full border border-cream-dark rounded-xl px-3 py-2 bg-cream" />
        </div>

        <div>
          <label className="font-bold text-sm block mb-1">Notes</label>
          <textarea value={notes} onChange={e => setNotes(e.target.value)} placeholder="Notes (optional)" rows={2} className="w-full border border-cream-dark rounded-xl px-3 py-2 bg-cream" />
        </div>

        {sowMut.error && <p className="text-error text-sm">{(sowMut.error as Error).message}</p>}

        <button onClick={() => sowMut.mutate()} disabled={!valid || sowMut.isPending} className="btn-primary w-full">
          {sowMut.isPending ? 'Sowing...' : 'Sow'}
        </button>
      </div>
    </div>
  )
}
