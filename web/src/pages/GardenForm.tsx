import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { api } from '../api/client'
import { PageHeader } from '../components/PageHeader'

export function GardenForm() {
  const navigate = useNavigate()
  const qc = useQueryClient()
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [emoji, setEmoji] = useState('')

  const mutation = useMutation({
    mutationFn: () => api.gardens.create({ name, description: description || undefined, emoji: emoji || undefined }),
    onSuccess: (g) => { qc.invalidateQueries({ queryKey: ['dashboard'] }); navigate(`/garden/${g.id}`, { replace: true }) },
  })

  return (
    <div>
      <PageHeader title="New Garden" back />
      <div className="px-4 py-4 space-y-4">
        <input value={emoji} onChange={e => setEmoji(e.target.value)} placeholder="Emoji (e.g. 🌻)" className="w-full border border-cream-dark rounded-xl px-3 py-2 bg-cream" />
        <input value={name} onChange={e => setName(e.target.value)} placeholder="Garden name" className="w-full border border-cream-dark rounded-xl px-3 py-2 bg-cream" />
        <textarea value={description} onChange={e => setDescription(e.target.value)} placeholder="Description (optional)" rows={3} className="w-full border border-cream-dark rounded-xl px-3 py-2 bg-cream" />
        {mutation.error && <p className="text-error text-sm">{(mutation.error as Error).message}</p>}
        <button onClick={() => mutation.mutate()} disabled={!name.trim() || mutation.isPending} className="btn-primary w-full">
          {mutation.isPending ? 'Creating...' : 'Create Garden'}
        </button>
      </div>
    </div>
  )
}
