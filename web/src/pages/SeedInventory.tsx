import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { api, type SeedInventoryResponse } from '../api/client'
import { PageHeader } from '../components/PageHeader'
import { ErrorDisplay } from '../components/ErrorDisplay'
import { Dialog } from '../components/Dialog'

export function SeedInventory() {
  const qc = useQueryClient()
  const { t } = useTranslation()
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

  if (isLoading) return <div className="flex justify-center p-16"><div className="animate-spin h-8 w-8 border-2 border-accent border-t-transparent rounded-full" /></div>
  if (error) return <ErrorDisplay error={error} onRetry={refetch} />

  return (
    <div>
      <PageHeader title={t('seeds.title')} action={{ label: t('seeds.addSeeds'), onClick: () => setShowAdd(true) }} />
      <div className="px-4 py-4 space-y-3">
        {data && data.length === 0 && (
          <p className="text-text-secondary text-sm text-center py-4">{t('seeds.noBatches')}</p>
        )}

        {data?.map(item => (
          <div key={item.id} className="card flex items-center justify-between">
            <div>
              <p className="font-semibold text-sm">{item.speciesName}</p>
              <p className="text-xs text-text-secondary">
                {t('seeds.seedCount', { count: item.quantity })}
                {item.collectionDate && ` · ${t('seeds.collected', { date: item.collectionDate })}`}
                {item.expirationDate && ` · ${t('seeds.expires', { date: item.expirationDate })}`}
              </p>
            </div>
            <button onClick={() => setDeleteItem(item)} className="text-error text-xs">{t('common.delete')}</button>
          </div>
        ))}
      </div>

      <Dialog open={showAdd} onClose={() => setShowAdd(false)} title={t('seeds.addSeedsTitle')} actions={
        <>
          <button onClick={() => setShowAdd(false)} className="px-4 py-2 text-sm text-text-secondary">{t('common.cancel')}</button>
          <button
            onClick={() => createMut.mutate()}
            disabled={!addSpeciesId || !addQuantity || createMut.isPending}
            className="btn-primary text-sm"
          >
            {createMut.isPending ? t('species.adding') : t('common.add')}
          </button>
        </>
      }>
        <div className="space-y-4">
          <div>
            <label className="field-label">{t('common.speciesLabel')}</label>
            <select value={addSpeciesId} onChange={e => setAddSpeciesId(e.target.value)} className="input">
              <option value="">{t('common.selectSpecies')}</option>
              {species?.map(s => (
                <option key={s.id} value={s.id}>{s.commonName}{s.variantName ? ` — ${s.variantName}` : ''}</option>
              ))}
            </select>
          </div>
          <div>
            <label className="field-label">{t('seeds.quantityLabel')}</label>
            <input type="number" value={addQuantity} onChange={e => setAddQuantity(e.target.value)} placeholder="e.g. 50" className="input" />
          </div>
          <div>
            <label className="field-label">{t('seeds.collectionDate')}</label>
            <input type="date" value={addCollection} onChange={e => setAddCollection(e.target.value)} className="input" />
          </div>
          <div>
            <label className="field-label">{t('seeds.expirationDate')}</label>
            <input type="date" value={addExpiration} onChange={e => setAddExpiration(e.target.value)} className="input" />
          </div>
        </div>
      </Dialog>

      <Dialog open={deleteItem !== null} onClose={() => setDeleteItem(null)} title={t('seeds.deleteBatchTitle')} actions={
        <>
          <button onClick={() => setDeleteItem(null)} className="px-4 py-2 text-sm text-text-secondary">{t('common.cancel')}</button>
          <button onClick={() => deleteItem && deleteMut.mutate(deleteItem.id)} className="px-4 py-2 text-sm text-error font-semibold">{t('common.delete')}</button>
        </>
      }>
        <p className="text-text-secondary">{t('seeds.deleteBatchConfirm')}</p>
      </Dialog>

    </div>
  )
}
