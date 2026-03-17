import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useParams, useNavigate, Link } from 'react-router-dom'
import { useState } from 'react'
import { api } from '../api/client'
import { PageHeader } from '../components/PageHeader'
import { ErrorDisplay } from '../components/ErrorDisplay'
import { StatusBadge } from '../components/StatusBadge'
import { Dialog } from '../components/Dialog'
import { Fab } from '../components/Fab'

export function BedDetail() {
  const { id } = useParams<{ id: string }>()
  const bedId = Number(id)
  const navigate = useNavigate()
  const qc = useQueryClient()

  const { data: bed, error, isLoading, refetch } = useQuery({
    queryKey: ['bed', bedId],
    queryFn: () => api.beds.get(bedId),
  })

  const { data: plants } = useQuery({
    queryKey: ['bed-plants', bedId],
    queryFn: () => api.beds.plants(bedId),
  })

  const [editing, setEditing] = useState(false)
  const [editName, setEditName] = useState('')
  const [editDesc, setEditDesc] = useState('')
  const [showDelete, setShowDelete] = useState(false)
  const [showAdd, setShowAdd] = useState(false)
  const [expandedGroups, setExpandedGroups] = useState<Set<string>>(new Set())

  const updateMut = useMutation({
    mutationFn: () => api.beds.update(bedId, { name: editName, description: editDesc || undefined }),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['bed', bedId] }); setEditing(false) },
  })

  const deleteMut = useMutation({
    mutationFn: () => api.beds.delete(bedId),
    onSuccess: () => { navigate(-1); qc.invalidateQueries({ queryKey: ['garden-beds'] }) },
  })

  if (isLoading) return <div className="flex justify-center p-16"><div className="animate-spin h-8 w-8 border-4 border-green-primary border-t-transparent rounded-full" /></div>
  if (error) return <ErrorDisplay error={error} onRetry={refetch} />
  if (!bed) return null

  const grouped = new Map<string, typeof plants>()
  plants?.forEach(p => {
    const key = p.speciesName ?? p.name
    if (!grouped.has(key)) grouped.set(key, [])
    grouped.get(key)!.push(p)
  })

  const toggleGroup = (key: string) => {
    setExpandedGroups(prev => {
      const next = new Set(prev)
      if (next.has(key)) next.delete(key); else next.add(key)
      return next
    })
  }

  return (
    <div>
      <PageHeader
        title={bed.name}
        back
        action={{ label: 'Edit', onClick: () => { setEditName(bed.name); setEditDesc(bed.description ?? ''); setEditing(true) } }}
      />

      <div className="px-4 py-4 space-y-3">
        {bed.description && <p className="text-text-secondary">{bed.description}</p>}

        <div className="flex items-center justify-between">
          <h2 className="font-bold text-lg">Plants</h2>
          <button onClick={() => setShowDelete(true)} className="text-error text-sm font-medium">Delete bed</button>
        </div>

        {plants && plants.length === 0 && (
          <p className="text-text-secondary text-sm">No plants yet. Tap + to add one.</p>
        )}

        {Array.from(grouped.entries()).map(([species, group]) => {
          const expanded = expandedGroups.has(species)
          return (
            <div key={species} className="card p-0 overflow-hidden">
              <button onClick={() => toggleGroup(species)} className="w-full flex items-center justify-between px-4 py-3 text-left">
                <div>
                  <p className="font-semibold">{species}</p>
                  <p className="text-xs text-text-secondary">{group!.length} {group!.length === 1 ? 'plant' : 'plants'}</p>
                </div>
                <span className="text-text-secondary text-lg">{expanded ? '▲' : '▼'}</span>
              </button>
              {expanded && (
                <div className="border-t border-cream">
                  {group!.map((p, i) => (
                    <div key={p.id}>
                      <Link to={`/plant/${p.id}`} className="flex items-center justify-between px-4 py-2.5 text-sm no-underline text-inherit hover:bg-cream/50">
                        <span>{p.name}</span>
                        <StatusBadge status={p.status} />
                      </Link>
                      {i < group!.length - 1 && <hr className="mx-4 border-cream" />}
                    </div>
                  ))}
                </div>
              )}
            </div>
          )
        })}
      </div>

      <Dialog open={editing} onClose={() => setEditing(false)} title="Edit Bed" actions={
        <>
          <button onClick={() => setEditing(false)} className="px-4 py-2 text-sm text-text-secondary">Cancel</button>
          <button onClick={() => updateMut.mutate()} disabled={!editName.trim()} className="btn-primary text-sm">Save</button>
        </>
      }>
        <div className="space-y-3">
          <input value={editName} onChange={e => setEditName(e.target.value)} placeholder="Bed name" className="w-full border border-cream-dark rounded-xl px-3 py-2 bg-cream" />
          <textarea value={editDesc} onChange={e => setEditDesc(e.target.value)} placeholder="Description (optional)" rows={2} className="w-full border border-cream-dark rounded-xl px-3 py-2 bg-cream" />
        </div>
      </Dialog>

      <Dialog open={showDelete} onClose={() => setShowDelete(false)} title="Delete Bed" actions={
        <>
          <button onClick={() => setShowDelete(false)} className="px-4 py-2 text-sm text-text-secondary">Cancel</button>
          <button onClick={() => deleteMut.mutate()} className="px-4 py-2 text-sm text-error font-semibold">Delete</button>
        </>
      }>
        <p className="text-text-secondary">This will delete the bed and all its plants. Are you sure?</p>
      </Dialog>

      <Dialog open={showAdd} onClose={() => setShowAdd(false)} title="Add Plant" actions={
        <button onClick={() => setShowAdd(false)} className="px-4 py-2 text-sm text-text-secondary">Cancel</button>
      }>
        <div className="space-y-2">
          <button onClick={() => { setShowAdd(false); navigate(`/sow?bedId=${bedId}`) }} className="btn-primary w-full text-sm">Sow seeds in bed</button>
        </div>
      </Dialog>

      <Fab onClick={() => setShowAdd(true)} />
    </div>
  )
}
