import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { api, type SeedInventoryResponse } from '../api/client'
import { PageHeader } from '../components/PageHeader'
import { ErrorDisplay } from '../components/ErrorDisplay'
import { Fab } from '../components/Fab'
import { Dialog } from '../components/Dialog'

export function SeedInventory() {
  const qc = useQueryClient()
  const { data, error, isLoading, refetch } = useQuery({
    queryKey: ['seed-inventory'],
    queryFn: () => api.inventory.list(),
  })

  const { data: species } = useQuery({ queryKey: ['species'], queryFn: api.species.list })

  const [showAdd, setShowAdd] = useState(false)
  const [addSpeciesId, setAddSpeciesId] = useState('')
  const [addQuantity, setAddQuantity] = useState('')
  const [addCollection, setAddCollection] = useState('')
  const [addExpiration, setAddExpiration] = useState('')
  const [deleteItem, setDeleteItem] = useState<SeedInventoryResponse | null>(null)

  const createMut = useMutation({
    mutationFn: () => api.inventory.create({
      speciesId: Number(addSpeciesId),
      quantity: Number(addQuantity),
      collectionDate: addCollection || undefined,
      expirationDate: addExpiration || undefined,
    }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['seed-inventory'] })
      setShowAdd(false); setAddSpeciesId(''); setAddQuantity(''); setAddCollection(''); setAddExpiration('')
    },
  })

  const deleteMut = useMutation({
    mutationFn: (id: number) => api.inventory.delete(id),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['seed-inventory'] }); setDeleteItem(null) },
  })

  if (isLoading) return <div className="flex justify-center p-16"><div className="animate-spin h-8 w-8 border-4 border-green-primary border-t-transparent rounded-full" /></div>
  if (error) return <ErrorDisplay error={error} onRetry={refetch} />

  return (
    <div>
      <PageHeader title="Seed Inventory" />
      <div className="px-4 py-4 space-y-3">
        {data && data.length === 0 && (
          <p className="text-text-secondary text-sm text-center py-4">No seed batches. Tap + to add one.</p>
        )}

        {data?.map(item => (
          <div key={item.id} className="card flex items-center justify-between">
            <div>
              <p className="font-semibold text-sm">{item.speciesName}</p>
              <p className="text-xs text-text-secondary">
                {item.quantity} seeds
                {item.collectionDate && ` · Collected ${item.collectionDate}`}
                {item.expirationDate && ` · Expires ${item.expirationDate}`}
              </p>
            </div>
            <button onClick={() => setDeleteItem(item)} className="text-error text-xs">Delete</button>
          </div>
        ))}
      </div>

      <Dialog open={showAdd} onClose={() => setShowAdd(false)} title="Add Seeds" actions={
        <>
          <button onClick={() => setShowAdd(false)} className="px-4 py-2 text-sm text-text-secondary">Cancel</button>
          <button
            onClick={() => createMut.mutate()}
            disabled={!addSpeciesId || !addQuantity || createMut.isPending}
            className="btn-primary text-sm"
          >
            {createMut.isPending ? 'Adding...' : 'Add'}
          </button>
        </>
      }>
        <div className="space-y-3">
          <select value={addSpeciesId} onChange={e => setAddSpeciesId(e.target.value)} className="w-full border border-cream-dark rounded-xl px-3 py-2 bg-cream">
            <option value="">Select species...</option>
            {species?.map(s => (
              <option key={s.id} value={s.id}>{s.commonName}{s.variantName ? ` — ${s.variantName}` : ''}</option>
            ))}
          </select>
          <input type="number" value={addQuantity} onChange={e => setAddQuantity(e.target.value)} placeholder="Quantity" className="w-full border border-cream-dark rounded-xl px-3 py-2 bg-cream" />
          <input type="date" value={addCollection} onChange={e => setAddCollection(e.target.value)} placeholder="Collection date" className="w-full border border-cream-dark rounded-xl px-3 py-2 bg-cream" />
          <input type="date" value={addExpiration} onChange={e => setAddExpiration(e.target.value)} placeholder="Expiration date" className="w-full border border-cream-dark rounded-xl px-3 py-2 bg-cream" />
        </div>
      </Dialog>

      <Dialog open={deleteItem !== null} onClose={() => setDeleteItem(null)} title="Delete Batch" actions={
        <>
          <button onClick={() => setDeleteItem(null)} className="px-4 py-2 text-sm text-text-secondary">Cancel</button>
          <button onClick={() => deleteItem && deleteMut.mutate(deleteItem.id)} className="px-4 py-2 text-sm text-error font-semibold">Delete</button>
        </>
      }>
        <p className="text-text-secondary">Delete this seed batch?</p>
      </Dialog>

      <Fab onClick={() => setShowAdd(true)} />
    </div>
  )
}
