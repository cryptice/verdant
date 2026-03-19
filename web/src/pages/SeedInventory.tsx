import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { api, type SeedInventoryResponse } from '../api/client'
import { PageHeader } from '../components/PageHeader'
import { ErrorDisplay } from '../components/ErrorDisplay'
import { Dialog } from '../components/Dialog'
import { Pagination } from '../components/Pagination'

const PAGE_SIZE = 50

export function SeedInventory() {
  const qc = useQueryClient()
  const { t, i18n } = useTranslation()
  const { data, error, isLoading, refetch } = useQuery({
    queryKey: ['seed-inventory'],
    queryFn: () => api.inventory.list(),
  })

  const { data: species } = useQuery({ queryKey: ['species'], queryFn: api.species.list })

  const [showAdd, setShowAdd] = useState(false)
  const [addSpeciesId, setAddSpeciesId] = useState('')
  const [speciesSearch, setSpeciesSearch] = useState('')
  const [addQuantity, setAddQuantity] = useState('')
  const [addCollection, setAddCollection] = useState('')
  const [addExpiration, setAddExpiration] = useState('')
  const [deleteItem, setDeleteItem] = useState<SeedInventoryResponse | null>(null)
  const [page, setPage] = useState(0)
  const [editItem, setEditItem] = useState<SeedInventoryResponse | null>(null)
  const [editQuantity, setEditQuantity] = useState('')
  const [editCollection, setEditCollection] = useState('')
  const [editExpiration, setEditExpiration] = useState('')

  const createMut = useMutation({
    mutationFn: () => api.inventory.create({
      speciesId: Number(addSpeciesId),
      quantity: Number(addQuantity),
      collectionDate: addCollection || undefined,
      expirationDate: addExpiration || undefined,
    }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['seed-inventory'] })
      setShowAdd(false); setAddSpeciesId(''); setSpeciesSearch(''); setAddQuantity(''); setAddCollection(''); setAddExpiration('')
    },
  })

  const [updateError, setUpdateError] = useState<string | null>(null)
  const [deleteError, setDeleteError] = useState<string | null>(null)

  const updateMut = useMutation({
    mutationFn: () => api.inventory.update(editItem!.id, {
      quantity: Number(editQuantity),
      collectionDate: editCollection || undefined,
      expirationDate: editExpiration || undefined,
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
      <PageHeader title={t('seeds.title')} action={{ label: t('seeds.addSeeds'), onClick: () => setShowAdd(true) }} />
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
                      }}
                    >
                      <td className="px-4 py-2.5 text-sm">
                        {name}{variant ? <span className="text-text-secondary"> — {variant}</span> : ''}
                      </td>
                      <td className="px-4 py-2.5 text-sm text-right tabular-nums">{item.quantity}</td>
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

      <Dialog open={showAdd} onClose={() => { setShowAdd(false); setAddSpeciesId(''); setSpeciesSearch(''); setAddQuantity(''); setAddCollection(''); setAddExpiration('') }} title={t('seeds.addSeedsTitle')} actions={
        <>
          <button onClick={() => { setShowAdd(false); setAddSpeciesId(''); setSpeciesSearch(''); setAddQuantity(''); setAddCollection(''); setAddExpiration('') }} className="px-4 py-2 text-sm text-text-secondary">{t('common.cancel')}</button>
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
          <div className="relative">
            <label className="field-label">{t('common.speciesLabel')}</label>
            <input
              value={speciesSearch || (addSpeciesId ? (() => {
                const s = species?.find(sp => sp.id === Number(addSpeciesId))
                if (!s) return ''
                const name = i18n.language === 'sv' ? (s.commonNameSv ?? s.commonName) : s.commonName
                const variant = i18n.language === 'sv' ? (s.variantNameSv ?? s.variantName) : s.variantName
                return variant ? `${name} — ${variant}` : name
              })() : '')}
              onChange={e => { setSpeciesSearch(e.target.value); setAddSpeciesId('') }}
              placeholder={t('common.searchSpecies')}
              className="input w-full"
            />
            {speciesSearch && (
              <div className="absolute z-10 left-0 right-0 mt-1 border border-divider rounded-md bg-bg shadow-md max-h-48 overflow-y-auto">
                {species?.filter(s => {
                  const q = speciesSearch.toLowerCase()
                  return s.commonName.toLowerCase().includes(q) ||
                    s.commonNameSv?.toLowerCase().includes(q) ||
                    s.variantName?.toLowerCase().includes(q) ||
                    s.variantNameSv?.toLowerCase().includes(q)
                }).slice(0, 20).map(s => {
                  const name = i18n.language === 'sv' ? (s.commonNameSv ?? s.commonName) : s.commonName
                  const variant = i18n.language === 'sv' ? (s.variantNameSv ?? s.variantName) : s.variantName
                  return (
                    <button
                      key={s.id}
                      onClick={() => { setAddSpeciesId(String(s.id)); setSpeciesSearch('') }}
                      className="w-full text-left px-3 py-2 text-sm hover:bg-surface transition-colors"
                    >
                      {name}{variant ? ` — ${variant}` : ''}
                    </button>
                  )
                })}
                {species?.filter(s => {
                  const q = speciesSearch.toLowerCase()
                  return s.commonName.toLowerCase().includes(q) ||
                    s.commonNameSv?.toLowerCase().includes(q) ||
                    s.variantName?.toLowerCase().includes(q) ||
                    s.variantNameSv?.toLowerCase().includes(q)
                }).length === 0 && (
                  <p className="px-3 py-2 text-sm text-text-secondary">{t('species.noSpeciesFoundDropdown')}</p>
                )}
              </div>
            )}
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
