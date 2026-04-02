import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { api, type SeedInventoryResponse, type SpeciesResponse } from '../api/client'
import { PageHeader } from '../components/PageHeader'
import { ErrorDisplay } from '../components/ErrorDisplay'
import { Dialog } from '../components/Dialog'
import { Pagination } from '../components/Pagination'
import { SpeciesAutocomplete } from '../components/SpeciesAutocomplete'

const PAGE_SIZE = 50

export function SeedInventory() {
  const qc = useQueryClient()
  const { t } = useTranslation()
  const { data, error, isLoading, refetch } = useQuery({
    queryKey: ['seed-inventory'],
    queryFn: () => api.inventory.list(),
  })

  const { data: species } = useQuery({ queryKey: ['species'], queryFn: api.species.list })

  const [showAdd, setShowAdd] = useState(false)
  const [addSpecies, setAddSpecies] = useState<SpeciesResponse | null>(null)
  const [addQuantity, setAddQuantity] = useState('')
  const [addCollection, setAddCollection] = useState('')
  const [addExpiration, setAddExpiration] = useState('')
  const [addCost, setAddCost] = useState('')
  const [addUnitType, setAddUnitType] = useState('')
  const [deleteItem, setDeleteItem] = useState<SeedInventoryResponse | null>(null)
  const [page, setPage] = useState(0)
  const [editItem, setEditItem] = useState<SeedInventoryResponse | null>(null)
  const [editQuantity, setEditQuantity] = useState('')
  const [editCollection, setEditCollection] = useState('')
  const [editExpiration, setEditExpiration] = useState('')
  const [editCost, setEditCost] = useState('')
  const [editUnitType, setEditUnitType] = useState('')

  const createMut = useMutation({
    mutationFn: () => api.inventory.create({
      speciesId: addSpecies!.id,
      quantity: Number(addQuantity),
      collectionDate: addCollection || undefined,
      expirationDate: addExpiration || undefined,
      costPerUnitSek: addCost ? Number(addCost) : undefined,
      unitType: addUnitType || undefined,
    }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['seed-inventory'] })
      setShowAdd(false); setAddSpecies(null); setAddQuantity(''); setAddCollection(''); setAddExpiration('')
      setAddCost(''); setAddUnitType('')
    },
  })

  const [updateError, setUpdateError] = useState<string | null>(null)
  const [deleteError, setDeleteError] = useState<string | null>(null)

  const updateMut = useMutation({
    mutationFn: () => api.inventory.update(editItem!.id, {
      quantity: Number(editQuantity),
      collectionDate: editCollection || undefined,
      expirationDate: editExpiration || undefined,
      costPerUnitSek: editCost ? Number(editCost) : undefined,
      unitType: editUnitType || undefined,
    }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['seed-inventory'] })
      setEditItem(null); setUpdateError(null)
    },
    onError: (err) => { setUpdateError(err instanceof Error ? err.message : String(err)) },
  })

  const deleteMut = useMutation({
    mutationFn: (id: number) => api.inventory.delete(id),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['seed-inventory'] }); setDeleteItem(null); setDeleteError(null) },
    onError: (err) => { setDeleteError(err instanceof Error ? err.message : String(err)) },
  })

  if (isLoading) return <div className="flex justify-center p-16"><div className="animate-spin h-8 w-8 border-2 border-accent border-t-transparent rounded-full" /></div>
  if (error) return <ErrorDisplay error={error} onRetry={refetch} />

  return (
    <div>
      <PageHeader title={t('seeds.title')} action={{ label: t('seeds.addSeeds'), onClick: () => setShowAdd(true), 'data-onboarding': 'add-seed-btn' }} />
      <div className="px-4 py-4">
        {data && data.length === 0 && (
          <p className="text-text-secondary text-sm text-center py-4">{t('seeds.noBatches')}</p>
        )}

        {data && data.length > 0 && (<>
          <div className="border border-divider rounded-xl overflow-hidden bg-bg shadow-sm">
            <table className="w-full">
              <thead>
                <tr className="border-b border-divider bg-surface">
                  <th className="text-left px-4 py-2 text-xs font-medium text-text-secondary">{t('seeds.colName')}</th>
                  <th className="text-right px-4 py-2 text-xs font-medium text-text-secondary">{t('seeds.colSeeds')}</th>
                  <th className="text-right px-4 py-2 text-xs font-medium text-text-secondary">{t('seeds.colCost')}</th>
                  <th className="text-left px-4 py-2 text-xs font-medium text-text-secondary">{t('seeds.colExpires')}</th>
                </tr>
              </thead>
              <tbody>
                {data.slice(page * PAGE_SIZE, (page + 1) * PAGE_SIZE).map(item => {
                  const sp = species?.find(s => s.id === item.speciesId)
                  const name = sp?.commonNameSv ?? sp?.commonName ?? item.speciesName
                  const variant = sp?.variantNameSv ?? sp?.variantName
                  return (
                    <tr
                      key={item.id}
                      className="border-b border-divider last:border-0 hover:bg-surface cursor-pointer transition-colors"
                      onClick={() => {
                        setEditItem(item)
                        setEditQuantity(String(item.quantity))
                        setEditCollection(item.collectionDate ?? '')
                        setEditExpiration(item.expirationDate ?? '')
                        setEditCost(item.costPerUnitSek != null ? String(item.costPerUnitSek) : '')
                        setEditUnitType(item.unitType ?? '')
                      }}
                    >
                      <td className="px-4 py-2.5 text-sm">
                        {name}{variant ? <span className="text-text-secondary"> — {variant}</span> : ''}
                      </td>
                      <td className="px-4 py-2.5 text-sm text-right tabular-nums">{item.quantity}</td>
                      <td className="px-4 py-2.5 text-sm text-right tabular-nums text-text-secondary">
                        {item.costPerUnitSek != null ? `${item.costPerUnitSek} kr` : '—'}
                      </td>
                      <td className="px-4 py-2.5 text-sm text-text-secondary">{item.expirationDate ?? '—'}</td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
          <Pagination page={page} pageSize={PAGE_SIZE} total={data.length} onPageChange={setPage} />
        </>)}
      </div>

      <Dialog open={showAdd} onClose={() => { setShowAdd(false); setAddSpecies(null); setAddQuantity(''); setAddCollection(''); setAddExpiration(''); setAddCost(''); setAddUnitType('') }} title={t('seeds.addSeedsTitle')} actions={
        <>
          <button onClick={() => { setShowAdd(false); setAddSpecies(null); setAddQuantity(''); setAddCollection(''); setAddExpiration(''); setAddCost(''); setAddUnitType('') }} className="px-4 py-2 text-sm text-text-secondary">{t('common.cancel')}</button>
          <button
            onClick={() => createMut.mutate()}
            disabled={!addSpecies || !addQuantity || createMut.isPending}
            className="btn-primary text-sm"
          >
            {createMut.isPending ? t('species.adding') : t('common.add')}
          </button>
        </>
      }>
        <div data-onboarding="seed-form" className="space-y-4">
          <div>
            <label className="field-label">{t('common.speciesLabel')}</label>
            <SpeciesAutocomplete value={addSpecies} onChange={setAddSpecies} />
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
          <div>
            <label className="field-label">{t('seeds.costPerUnit')}</label>
            <input type="number" value={addCost} onChange={e => setAddCost(e.target.value)} placeholder="e.g. 35" className="input" />
          </div>
          <div>
            <label className="field-label">{t('seeds.unitType')}</label>
            <select value={addUnitType} onChange={e => setAddUnitType(e.target.value)} className="input">
              <option value="">{t('common.optional')}</option>
              {['SEED', 'PLUG', 'BULB', 'TUBER', 'PLANT'].map(ut => (
                <option key={ut} value={ut}>{t(`unitTypes.${ut}`)}</option>
              ))}
            </select>
          </div>
        </div>
      </Dialog>

      <Dialog open={editItem !== null} onClose={() => { setEditItem(null); setUpdateError(null) }} title={t('seeds.editSeedsTitle')} actions={
        <>
          <button onClick={() => { setEditItem(null); setUpdateError(null) }} className="px-4 py-2 text-sm text-text-secondary">{t('common.cancel')}</button>
          <button
            onClick={() => updateMut.mutate()}
            disabled={!editQuantity || updateMut.isPending}
            className="btn-primary text-sm"
          >
            {updateMut.isPending ? t('common.saving') : t('common.save')}
          </button>
        </>
      }>
        <div className="space-y-4">
          <div>
            <label className="field-label">{t('seeds.quantityLabel')}</label>
            <input type="number" value={editQuantity} onChange={e => setEditQuantity(e.target.value)} className="input w-full" />
          </div>
          <div>
            <label className="field-label">{t('seeds.collectionDate')}</label>
            <input type="date" value={editCollection} onChange={e => setEditCollection(e.target.value)} className="input w-full" />
          </div>
          <div>
            <label className="field-label">{t('seeds.expirationDate')}</label>
            <input type="date" value={editExpiration} onChange={e => setEditExpiration(e.target.value)} className="input w-full" />
          </div>
          <div>
            <label className="field-label">{t('seeds.costPerUnit')}</label>
            <input type="number" value={editCost} onChange={e => setEditCost(e.target.value)} placeholder="e.g. 35" className="input w-full" />
          </div>
          <div>
            <label className="field-label">{t('seeds.unitType')}</label>
            <select value={editUnitType} onChange={e => setEditUnitType(e.target.value)} className="input w-full">
              <option value="">{t('common.optional')}</option>
              {['SEED', 'PLUG', 'BULB', 'TUBER', 'PLANT'].map(ut => (
                <option key={ut} value={ut}>{t(`unitTypes.${ut}`)}</option>
              ))}
            </select>
          </div>
          <button
            onClick={() => { setEditItem(null); setUpdateError(null); setDeleteItem(editItem) }}
            className="text-sm text-error hover:underline"
          >
            {t('seeds.deleteBatch')}
          </button>
          {updateError && <p className="text-error text-sm mt-2">{updateError}</p>}
        </div>
      </Dialog>

      <Dialog open={deleteItem !== null} onClose={() => { setDeleteItem(null); setDeleteError(null) }} title={t('seeds.deleteBatchTitle')} actions={
        <>
          <button onClick={() => { setDeleteItem(null); setDeleteError(null) }} className="px-4 py-2 text-sm text-text-secondary">{t('common.cancel')}</button>
          <button onClick={() => deleteItem && deleteMut.mutate(deleteItem.id)} className="px-4 py-2 text-sm text-error font-semibold">{t('common.delete')}</button>
        </>
      }>
        <p className="text-text-secondary">{t('seeds.deleteBatchConfirm')}</p>
        {deleteError && <p className="text-error text-sm mt-2">{deleteError}</p>}
      </Dialog>

    </div>
  )
}
