import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { api, type ListingResponse, type SpeciesResponse } from '../api/client'
import { PageHeader } from '../components/PageHeader'
import { ErrorDisplay } from '../components/ErrorDisplay'
import { Dialog } from '../components/Dialog'
import { SpeciesAutocomplete } from '../components/SpeciesAutocomplete'
import { Pagination } from '../components/Pagination'

const PAGE_SIZE = 50

export function MyListings() {
  const qc = useQueryClient()
  const { t } = useTranslation()
  const { data, error, isLoading, refetch } = useQuery({
    queryKey: ['my-listings'],
    queryFn: () => api.market.myListings(),
  })

  const [page, setPage] = useState(0)
  const [showAdd, setShowAdd] = useState(false)
  const [editItem, setEditItem] = useState<ListingResponse | null>(null)
  const [deleteItem, setDeleteItem] = useState<ListingResponse | null>(null)

  // Form state
  const [formSpecies, setFormSpecies] = useState<SpeciesResponse | null>(null)
  const [formTitle, setFormTitle] = useState('')
  const [formDescription, setFormDescription] = useState('')
  const [formQuantity, setFormQuantity] = useState('')
  const [formPrice, setFormPrice] = useState('')
  const [formFrom, setFormFrom] = useState('')
  const [formUntil, setFormUntil] = useState('')

  const [formError, setFormError] = useState<string | null>(null)
  const [deleteError, setDeleteError] = useState<string | null>(null)

  const resetForm = () => {
    setFormSpecies(null); setFormTitle(''); setFormDescription('')
    setFormQuantity(''); setFormPrice(''); setFormFrom(''); setFormUntil('')
    setFormError(null)
  }

  const openAdd = () => { resetForm(); setShowAdd(true) }

  const openEdit = (item: ListingResponse) => {
    setFormSpecies({ id: item.speciesId, commonName: item.speciesName } as SpeciesResponse)
    setFormTitle(item.title)
    setFormDescription(item.description ?? '')
    setFormQuantity(String(item.quantityAvailable))
    setFormPrice(String(item.pricePerStemCents))
    setFormFrom(item.availableFrom)
    setFormUntil(item.availableUntil)
    setFormError(null)
    setEditItem(item)
  }

  const buildPayload = () => ({
    speciesId: formSpecies?.id,
    title: formTitle,
    description: formDescription || undefined,
    quantityAvailable: Number(formQuantity),
    pricePerStemCents: Number(formPrice),
    availableFrom: formFrom,
    availableUntil: formUntil,
  })

  const createMut = useMutation({
    mutationFn: () => api.market.createListing(buildPayload()),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['my-listings'] }); setShowAdd(false); resetForm() },
    onError: (err) => { setFormError(err instanceof Error ? err.message : String(err)) },
  })

  const updateMut = useMutation({
    mutationFn: () => api.market.updateListing(editItem!.id, buildPayload()),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['my-listings'] }); setEditItem(null); resetForm() },
    onError: (err) => { setFormError(err instanceof Error ? err.message : String(err)) },
  })

  const deleteMut = useMutation({
    mutationFn: (id: number) => api.market.deleteListing(id),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['my-listings'] }); setDeleteItem(null); setDeleteError(null) },
    onError: (err) => { setDeleteError(err instanceof Error ? err.message : String(err)) },
  })

  if (isLoading) return <div className="flex justify-center p-16"><div className="animate-spin h-8 w-8 border-2 border-accent border-t-transparent rounded-full" /></div>
  if (error) return <ErrorDisplay error={error} onRetry={refetch} />

  const formatPrice = (cents: number) => `${(cents / 100).toFixed(2)} kr`

  const formFields = (
    <div className="space-y-4">
      <div>
        <label className="field-label">{t('listings.speciesLabel')} *</label>
        <SpeciesAutocomplete value={formSpecies} onChange={setFormSpecies} />
      </div>
      <div>
        <label className="field-label">{t('listings.listingTitle')} *</label>
        <input type="text" value={formTitle} onChange={e => setFormTitle(e.target.value)} className="input" />
      </div>
      <div>
        <label className="field-label">{t('listings.description')}</label>
        <textarea value={formDescription} onChange={e => setFormDescription(e.target.value)} placeholder={t('common.optional')} rows={2} className="input" />
      </div>
      <div>
        <label className="field-label">{t('listings.quantity')} *</label>
        <input type="number" min="0" value={formQuantity} onChange={e => setFormQuantity(e.target.value)} className="input" />
      </div>
      <div>
        <label className="field-label">{t('listings.price')} *</label>
        <input type="number" min="0" value={formPrice} onChange={e => setFormPrice(e.target.value)} className="input" />
      </div>
      <div>
        <label className="field-label">{t('listings.availableFrom')} *</label>
        <input type="date" value={formFrom} onChange={e => setFormFrom(e.target.value)} className="input" />
      </div>
      <div>
        <label className="field-label">{t('listings.availableUntil')} *</label>
        <input type="date" value={formUntil} onChange={e => setFormUntil(e.target.value)} className="input" />
      </div>
      {formError && <p className="text-error text-sm">{formError}</p>}
    </div>
  )

  const canSubmit = formSpecies && formTitle && formQuantity && formPrice && formFrom && formUntil

  return (
    <div>
      <PageHeader title={t('listings.title')} action={{ label: t('listings.new'), onClick: openAdd }} />
      <div className="px-4 py-4">
        {data && data.length === 0 && (
          <p className="text-text-secondary text-sm text-center py-4">{t('listings.noListings')}</p>
        )}

        {data && data.length > 0 && (<>
          <div className="border border-divider rounded-xl overflow-hidden bg-bg shadow-sm">
            <table className="w-full">
              <thead>
                <tr className="border-b border-divider bg-surface">
                  <th className="text-left px-4 py-2 text-xs font-medium text-text-secondary">{t('listings.speciesLabel')}</th>
                  <th className="text-left px-4 py-2 text-xs font-medium text-text-secondary">{t('listings.listingTitle')}</th>
                  <th className="text-right px-4 py-2 text-xs font-medium text-text-secondary">{t('listings.quantity')}</th>
                  <th className="text-right px-4 py-2 text-xs font-medium text-text-secondary">{t('listings.price')}</th>
                  <th className="text-left px-4 py-2 text-xs font-medium text-text-secondary">{t('listings.availableFrom')}</th>
                  <th className="text-left px-4 py-2 text-xs font-medium text-text-secondary">{t('listings.availableUntil')}</th>
                  <th className="text-left px-4 py-2 text-xs font-medium text-text-secondary">{t('listings.active')}</th>
                </tr>
              </thead>
              <tbody>
                {data.slice(page * PAGE_SIZE, (page + 1) * PAGE_SIZE).map(item => (
                  <tr
                    key={item.id}
                    className="border-b border-divider last:border-0 hover:bg-surface cursor-pointer transition-colors"
                    onClick={() => openEdit(item)}
                  >
                    <td className="px-4 py-2.5 text-sm">{item.speciesName}</td>
                    <td className="px-4 py-2.5 text-sm text-text-secondary">{item.title}</td>
                    <td className="px-4 py-2.5 text-sm text-right tabular-nums">{item.quantityAvailable}</td>
                    <td className="px-4 py-2.5 text-sm text-right tabular-nums">{formatPrice(item.pricePerStemCents)}</td>
                    <td className="px-4 py-2.5 text-sm text-text-secondary">{item.availableFrom}</td>
                    <td className="px-4 py-2.5 text-sm text-text-secondary">{item.availableUntil}</td>
                    <td className="px-4 py-2.5 text-sm">
                      {item.isActive && (
                        <span className="inline-block px-2 py-0.5 rounded-full text-xs font-medium bg-green-100 text-green-700">
                          {t('listings.active')}
                        </span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <Pagination page={page} pageSize={PAGE_SIZE} total={data.length} onPageChange={setPage} />
        </>)}
      </div>

      <Dialog open={showAdd} onClose={() => { setShowAdd(false); resetForm() }} title={t('listings.new')} actions={
        <>
          <button onClick={() => { setShowAdd(false); resetForm() }} className="px-4 py-2 text-sm text-text-secondary">{t('common.cancel')}</button>
          <button
            onClick={() => createMut.mutate()}
            disabled={!canSubmit || createMut.isPending}
            className="btn-primary text-sm"
          >
            {createMut.isPending ? t('common.saving') : t('common.add')}
          </button>
        </>
      }>
        {formFields}
      </Dialog>

      <Dialog open={editItem !== null} onClose={() => { setEditItem(null); resetForm() }} title={t('listings.edit')} actions={
        <>
          <button onClick={() => { setEditItem(null); resetForm() }} className="px-4 py-2 text-sm text-text-secondary">{t('common.cancel')}</button>
          <button
            onClick={() => updateMut.mutate()}
            disabled={!canSubmit || updateMut.isPending}
            className="btn-primary text-sm"
          >
            {updateMut.isPending ? t('common.saving') : t('common.save')}
          </button>
        </>
      }>
        {formFields}
        <button
          onClick={() => { setEditItem(null); resetForm(); setDeleteItem(editItem) }}
          className="text-sm text-error hover:underline mt-4"
        >
          {t('listings.deleteListing')}
        </button>
      </Dialog>

      <Dialog open={deleteItem !== null} onClose={() => { setDeleteItem(null); setDeleteError(null) }} title={t('listings.deleteListing')} actions={
        <>
          <button onClick={() => { setDeleteItem(null); setDeleteError(null) }} className="px-4 py-2 text-sm text-text-secondary">{t('common.cancel')}</button>
          <button onClick={() => deleteItem && deleteMut.mutate(deleteItem.id)} className="px-4 py-2 text-sm text-error font-semibold">{t('common.delete')}</button>
        </>
      }>
        <p className="text-text-secondary">{t('listings.deleteConfirm')}</p>
        {deleteError && <p className="text-error text-sm mt-2">{deleteError}</p>}
      </Dialog>
    </div>
  )
}
