import { useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { api } from '../api/client'
import { PageHeader } from '../components/PageHeader'

export function BedForm() {
  const { gardenId } = useParams<{ gardenId: string }>()
  const navigate = useNavigate()
  const qc = useQueryClient()
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')

  const mutation = useMutation({
    mutationFn: () => api.beds.create(Number(gardenId), { name, description: description || undefined }),
    onSuccess: (b) => { qc.invalidateQueries({ queryKey: ['garden-beds', Number(gardenId)] }); navigate(`/bed/${b.id}`, { replace: true }) },
  })

  return (
    <div>
      <PageHeader title="New Bed" back />
      <div className="px-4 py-4 space-y-4">
        <input value={name} onChange={e => setName(e.target.value)} placeholder="Bed name" className="w-full border border-cream-dark rounded-xl px-3 py-2 bg-cream" />
        <textarea value={description} onChange={e => setDescription(e.target.value)} placeholder="Description (optional)" rows={2} className="w-full border border-cream-dark rounded-xl px-3 py-2 bg-cream" />
        {mutation.error && <p className="text-error text-sm">{(mutation.error as Error).message}</p>}
        <button onClick={() => mutation.mutate()} disabled={!name.trim() || mutation.isPending} className="btn-primary w-full">
          {mutation.isPending ? 'Creating...' : 'Create Bed'}
        </button>
      </div>
    </div>
  )
}
