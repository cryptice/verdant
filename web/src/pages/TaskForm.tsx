import { useState, useEffect } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { api } from '../api/client'
import { PageHeader } from '../components/PageHeader'

const activityTypes = ['SOW', 'POT_UP', 'PLANT', 'HARVEST', 'RECOVER', 'DISCARD']

export function TaskForm() {
  const { taskId } = useParams<{ taskId: string }>()
  const isEdit = !!taskId
  const navigate = useNavigate()
  const qc = useQueryClient()

  const { data: species } = useQuery({ queryKey: ['species'], queryFn: api.species.list })
  const { data: existing } = useQuery({
    queryKey: ['task', Number(taskId)],
    queryFn: () => api.tasks.get(Number(taskId)),
    enabled: isEdit,
  })

  const [speciesId, setSpeciesId] = useState('')
  const [activityType, setActivityType] = useState('SOW')
  const [deadline, setDeadline] = useState('')
  const [targetCount, setTargetCount] = useState('')
  const [notes, setNotes] = useState('')

  useEffect(() => {
    if (existing) {
      setSpeciesId(String(existing.speciesId))
      setActivityType(existing.activityType)
      setDeadline(existing.deadline)
      setTargetCount(String(existing.targetCount))
      setNotes(existing.notes ?? '')
    }
  }, [existing])

  const createMut = useMutation({
    mutationFn: () => api.tasks.create({
      speciesId: Number(speciesId), activityType, deadline, targetCount: Number(targetCount), notes: notes || undefined,
    }),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['tasks'] }); navigate('/tasks', { replace: true }) },
  })

  const updateMut = useMutation({
    mutationFn: () => api.tasks.update(Number(taskId), {
      speciesId: Number(speciesId), activityType, deadline, targetCount: Number(targetCount), notes: notes || undefined,
    }),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['tasks'] }); navigate('/tasks', { replace: true }) },
  })

  const mutation = isEdit ? updateMut : createMut
  const valid = speciesId && deadline && Number(targetCount) > 0

  return (
    <div>
      <PageHeader title={isEdit ? 'Edit Task' : 'New Task'} back />
      <div className="px-4 py-4 space-y-4">
        <select value={speciesId} onChange={e => setSpeciesId(e.target.value)} className="w-full border border-cream-dark rounded-xl px-3 py-2 bg-cream">
          <option value="">Select species...</option>
          {species?.map(s => (
            <option key={s.id} value={s.id}>{s.commonName}{s.variantName ? ` — ${s.variantName}` : ''}</option>
          ))}
        </select>

        <select value={activityType} onChange={e => setActivityType(e.target.value)} className="w-full border border-cream-dark rounded-xl px-3 py-2 bg-cream">
          {activityTypes.map(t => <option key={t} value={t}>{t.replace('_', ' ')}</option>)}
        </select>

        <input type="date" value={deadline} onChange={e => setDeadline(e.target.value)} className="w-full border border-cream-dark rounded-xl px-3 py-2 bg-cream" />
        <input type="number" value={targetCount} onChange={e => setTargetCount(e.target.value)} placeholder="Target count" className="w-full border border-cream-dark rounded-xl px-3 py-2 bg-cream" />
        <textarea value={notes} onChange={e => setNotes(e.target.value)} placeholder="Notes (optional)" rows={2} className="w-full border border-cream-dark rounded-xl px-3 py-2 bg-cream" />

        {mutation.error && <p className="text-error text-sm">{(mutation.error as Error).message}</p>}

        <button onClick={() => mutation.mutate()} disabled={!valid || mutation.isPending} className="btn-primary w-full">
          {mutation.isPending ? 'Saving...' : isEdit ? 'Update Task' : 'Create Task'}
        </button>
      </div>
    </div>
  )
}
