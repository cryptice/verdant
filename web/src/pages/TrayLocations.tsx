import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../api/client'
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
  const [name, setName] = useState('')
  const [formError, setFormError] = useState<string | null>(null)

  const reset = () => { setName(''); setFormError(null) }

  const createMut = useMutation({
    mutationFn: () => api.trayLocations.create(name.trim()),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['tray-locations'] }); setShowAdd(false); reset() },
    onError: (e) => setFormError(e instanceof Error ? e.message : String(e)),
  })

  if (isLoading) return <div className="flex justify-center p-16"><div className="animate-spin h-8 w-8 border-2 border-accent border-t-transparent rounded-full" /></div>
  if (error) return <ErrorDisplay error={error} onRetry={refetch} />

  return (
    <div>
      <Masthead
        left="Platser"
        center="— Brättens platser —"
        right={
          <button
            onClick={() => { reset(); setShowAdd(true) }}
            aria-label="Ny plats"
            title="Ny plats"
            style={{
              background: 'transparent',
              border: '1px solid var(--color-accent)',
              color: 'var(--color-accent)',
              width: 32,
              height: 32,
              borderRadius: 999,
              fontSize: 18,
              lineHeight: 1,
              cursor: 'pointer',
              display: 'inline-flex',
              alignItems: 'center',
              justifyContent: 'center',
            }}
          >
            +
          </button>
        }
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
    </div>
  )
}
