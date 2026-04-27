import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api, type TrayLocationResponse } from '../api/client'
import { Masthead, Ledger } from '../components/faltet'
import { ErrorDisplay } from '../components/ErrorDisplay'
import { Dialog } from '../components/Dialog'

export function TrayLocations() {
  const qc = useQueryClient()
  const navigate = useNavigate()
  const { data: locations = [], error, isLoading, refetch } = useQuery({
    queryKey: ['tray-locations'],
    queryFn: () => api.trayLocations.list(),
  })

  const [showAdd, setShowAdd] = useState(false)
  const [editItem, setEditItem] = useState<TrayLocationResponse | null>(null)
  const [deleteItem, setDeleteItem] = useState<TrayLocationResponse | null>(null)

  const [name, setName] = useState('')
  const [formError, setFormError] = useState<string | null>(null)

  const reset = () => { setName(''); setFormError(null) }

  const openAdd = () => { reset(); setShowAdd(true) }
  const openEdit = (loc: TrayLocationResponse) => { setName(loc.name); setEditItem(loc) }

  const createMut = useMutation({
    mutationFn: () => api.trayLocations.create(name.trim()),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['tray-locations'] }); setShowAdd(false); reset() },
    onError: (e) => setFormError(e instanceof Error ? e.message : String(e)),
  })

  const updateMut = useMutation({
    mutationFn: () => api.trayLocations.update(editItem!.id, name.trim()),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['tray-locations'] }); setEditItem(null); reset() },
    onError: (e) => setFormError(e instanceof Error ? e.message : String(e)),
  })

  const deleteMut = useMutation({
    mutationFn: (id: number) => api.trayLocations.delete(id),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['tray-locations'] }); setDeleteItem(null) },
  })

  if (isLoading) return <div className="flex justify-center p-16"><div className="animate-spin h-8 w-8 border-2 border-accent border-t-transparent rounded-full" /></div>
  if (error) return <ErrorDisplay error={error} onRetry={refetch} />

  return (
    <div>
      <Masthead
        left="Platser"
        center="— Brättens platser —"
        right={<button onClick={openAdd} className="btn-primary">+ Ny plats</button>}
      />

      <div className="page-body">
        <Ledger
          columns={[
            {
              key: 'name', label: 'Namn', width: '1.5fr',
              render: (l) => <span style={{ fontFamily: 'var(--font-display)', fontSize: 20 }}>{l.name}</span>,
            },
            {
              key: 'count', label: 'Plantor', width: '120px', align: 'right',
              render: (l) => (
                <span style={{ fontVariantNumeric: 'tabular-nums' }}>{l.activePlantCount}</span>
              ),
            },
            {
              key: 'edit', label: '', width: '60px', align: 'right',
              render: (l) => (
                <button
                  onClick={(e) => { e.stopPropagation(); openEdit(l) }}
                  style={{ color: 'var(--color-accent)', fontFamily: 'var(--font-mono)', fontSize: 11, background: 'none', border: 'none', cursor: 'pointer' }}
                >
                  Redigera
                </button>
              ),
            },
            {
              key: 'goto', label: '', width: '40px', align: 'right',
              render: () => <span style={{ color: 'var(--color-accent)', fontFamily: 'var(--font-mono)' }}>→</span>,
            },
          ]}
          rows={locations}
          rowKey={(l) => l.id}
          onRowClick={(l) => navigate(`/tray-locations/${l.id}`)}
        />
        {locations.length === 0 && (
          <p style={{ marginTop: 24, fontFamily: 'var(--font-mono)', fontSize: 11, color: 'var(--color-forest)' }}>
            Inga platser ännu. Skapa en där dina brätten står.
          </p>
        )}
      </div>

      <Dialog
        open={showAdd}
        onClose={() => { setShowAdd(false); reset() }}
        title="Ny plats"
        actions={
          <>
            <button onClick={() => { setShowAdd(false); reset() }} className="px-4 py-2 text-sm text-text-secondary">Avbryt</button>
            <button
              onClick={() => createMut.mutate()}
              disabled={!name.trim() || createMut.isPending}
              className="btn-primary text-sm"
            >
              {createMut.isPending ? 'Sparar…' : 'Spara'}
            </button>
          </>
        }
      >
        <div className="space-y-3">
          <div>
            <label className="field-label">Namn *</label>
            <input
              type="text"
              autoFocus
              value={name}
              onChange={(e) => setName(e.target.value)}
              className="input w-full"
            />
          </div>
          {formError && <p className="text-error text-sm">{formError}</p>}
        </div>
      </Dialog>

      <Dialog
        open={editItem !== null}
        onClose={() => { setEditItem(null); reset() }}
        title="Redigera plats"
        actions={
          <>
            <button onClick={() => { setEditItem(null); reset() }} className="px-4 py-2 text-sm text-text-secondary">Avbryt</button>
            <button
              onClick={() => updateMut.mutate()}
              disabled={!name.trim() || updateMut.isPending || name.trim() === editItem?.name}
              className="btn-primary text-sm"
            >
              {updateMut.isPending ? 'Sparar…' : 'Spara'}
            </button>
          </>
        }
      >
        <div className="space-y-3">
          <div>
            <label className="field-label">Namn *</label>
            <input
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              className="input w-full"
            />
          </div>
          {formError && <p className="text-error text-sm">{formError}</p>}
        </div>
        <button
          onClick={() => { const item = editItem; setEditItem(null); reset(); setDeleteItem(item) }}
          className="text-sm text-error hover:underline mt-4"
        >
          Ta bort plats
        </button>
      </Dialog>

      <Dialog
        open={deleteItem !== null}
        onClose={() => setDeleteItem(null)}
        title="Ta bort plats"
        actions={
          <>
            <button onClick={() => setDeleteItem(null)} className="px-4 py-2 text-sm text-text-secondary">Avbryt</button>
            <button
              onClick={() => deleteItem && deleteMut.mutate(deleteItem.id)}
              className="px-4 py-2 text-sm text-error font-semibold"
            >
              Ta bort
            </button>
          </>
        }
      >
        <p className="text-text-secondary">
          {deleteItem && deleteItem.activePlantCount > 0
            ? `${deleteItem.activePlantCount} plantor i ${deleteItem.name} blir utan plats. Fortsätt?`
            : `Ta bort ${deleteItem?.name}?`}
        </p>
      </Dialog>
    </div>
  )
}
