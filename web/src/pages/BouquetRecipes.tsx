import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { api, type BouquetRecipeResponse, type SpeciesResponse } from '../api/client'
import { PageHeader } from '../components/PageHeader'
import { ErrorDisplay } from '../components/ErrorDisplay'
import { Dialog } from '../components/Dialog'
import { SpeciesAutocomplete } from '../components/SpeciesAutocomplete'

const ROLES = ['FLOWER', 'FOLIAGE', 'FILLER', 'ACCENT'] as const

interface FormItem {
  species: SpeciesResponse | null
  stemCount: string
  role: string
}

export function BouquetRecipes() {
  const qc = useQueryClient()
  const { t } = useTranslation()
  const { data, error, isLoading, refetch } = useQuery({
    queryKey: ['bouquet-recipes'],
    queryFn: () => api.bouquetRecipes.list(),
  })

  const [showAdd, setShowAdd] = useState(false)
  const [editItem, setEditItem] = useState<BouquetRecipeResponse | null>(null)
  const [deleteItem, setDeleteItem] = useState<BouquetRecipeResponse | null>(null)

  // Form state
  const [formName, setFormName] = useState('')
  const [formDescription, setFormDescription] = useState('')
  const [formPrice, setFormPrice] = useState('')
  const [formItems, setFormItems] = useState<FormItem[]>([])

  const [formError, setFormError] = useState<string | null>(null)
  const [deleteError, setDeleteError] = useState<string | null>(null)

  const resetForm = () => {
    setFormName(''); setFormDescription(''); setFormPrice(''); setFormItems([]); setFormError(null)
  }

  const openAdd = () => { resetForm(); setShowAdd(true) }

  const openEdit = (recipe: BouquetRecipeResponse) => {
    setFormName(recipe.name)
    setFormDescription(recipe.description ?? '')
    setFormPrice(recipe.priceSek != null ? String(recipe.priceSek) : '')
    setFormItems(recipe.items.map(item => ({
      species: { id: item.speciesId, commonName: item.speciesName } as SpeciesResponse,
      stemCount: String(item.stemCount),
      role: item.role,
    })))
    setFormError(null)
    setEditItem(recipe)
  }

  const addItem = () => {
    setFormItems([...formItems, { species: null, stemCount: '1', role: 'FLOWER' }])
  }

  const removeItem = (index: number) => {
    setFormItems(formItems.filter((_, i) => i !== index))
  }

  const updateItem = (index: number, updates: Partial<FormItem>) => {
    setFormItems(formItems.map((item, i) => i === index ? { ...item, ...updates } : item))
  }

  const buildPayload = () => ({
    name: formName,
    description: formDescription || undefined,
    priceSek: formPrice ? Number(formPrice) : undefined,
    items: formItems
      .filter(item => item.species)
      .map(item => ({
        speciesId: item.species!.id,
        stemCount: Number(item.stemCount) || 1,
        role: item.role,
      })),
  })

  const createMut = useMutation({
    mutationFn: () => api.bouquetRecipes.create(buildPayload()),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['bouquet-recipes'] }); setShowAdd(false); resetForm() },
    onError: (err) => { setFormError(err instanceof Error ? err.message : String(err)) },
  })

  const updateMut = useMutation({
    mutationFn: () => api.bouquetRecipes.update(editItem!.id, buildPayload()),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['bouquet-recipes'] }); setEditItem(null); resetForm() },
    onError: (err) => { setFormError(err instanceof Error ? err.message : String(err)) },
  })

  const deleteMut = useMutation({
    mutationFn: (id: number) => api.bouquetRecipes.delete(id),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['bouquet-recipes'] }); setDeleteItem(null); setDeleteError(null) },
    onError: (err) => { setDeleteError(err instanceof Error ? err.message : String(err)) },
  })

  if (isLoading) return <div className="flex justify-center p-16"><div className="animate-spin h-8 w-8 border-2 border-accent border-t-transparent rounded-full" /></div>
  if (error) return <ErrorDisplay error={error} onRetry={refetch} />

  const formatPrice = (cents?: number) => {
    if (cents == null) return '—'
    return `${(cents / 100).toFixed(2)} kr`
  }

  const totalStems = (recipe: BouquetRecipeResponse) =>
    recipe.items.reduce((sum, item) => sum + item.stemCount, 0)

  const formFields = (
    <div className="space-y-4">
      <div>
        <label className="field-label">{t('bouquets.name')} *</label>
        <input type="text" value={formName} onChange={e => setFormName(e.target.value)} className="input" />
      </div>
      <div>
        <label className="field-label">{t('bouquets.description')}</label>
        <textarea value={formDescription} onChange={e => setFormDescription(e.target.value)} placeholder={t('common.optional')} rows={2} className="input" />
      </div>
      <div>
        <label className="field-label">{t('bouquets.price')}</label>
        <input type="number" value={formPrice} onChange={e => setFormPrice(e.target.value)} className="input" />
      </div>

      {/* Dynamic item list */}
      <div className="space-y-3">
        {formItems.map((item, index) => (
          <div key={index} className="border border-divider rounded-xl p-3 space-y-2">
            <div className="flex items-center justify-between">
              <span className="text-xs font-medium text-text-secondary">#{index + 1}</span>
              <button
                onClick={() => removeItem(index)}
                className="text-xs text-error hover:underline"
              >
                {t('common.delete')}
              </button>
            </div>
            <div>
              <label className="field-label">{t('common.speciesLabel')}</label>
              <SpeciesAutocomplete value={item.species} onChange={species => updateItem(index, { species })} />
            </div>
            <div className="flex gap-2">
              <div className="flex-1">
                <label className="field-label">{t('bouquets.stemCount')}</label>
                <input type="number" min="1" value={item.stemCount} onChange={e => updateItem(index, { stemCount: e.target.value })} className="input" />
              </div>
              <div className="flex-1">
                <label className="field-label">{t('bouquets.role')}</label>
                <select value={item.role} onChange={e => updateItem(index, { role: e.target.value })} className="input">
                  {ROLES.map(r => (
                    <option key={r} value={r}>{t(`roles.${r}`)}</option>
                  ))}
                </select>
              </div>
            </div>
          </div>
        ))}
        <button onClick={addItem} className="text-sm text-accent hover:underline">
          {t('bouquets.addItem')}
        </button>
      </div>

      {formError && <p className="text-error text-sm">{formError}</p>}
    </div>
  )

  return (
    <div>
      <PageHeader title={t('bouquets.title')} action={{ label: t('bouquets.new'), onClick: openAdd, 'data-onboarding': 'add-bouquet-btn' }} />
      <div className="px-4 py-4">
        {data && data.length === 0 && (
          <p className="text-text-secondary text-sm text-center py-4">{t('bouquets.noRecipes')}</p>
        )}

        {data && data.length > 0 && (
          <div className="grid gap-3 grid-cols-1 sm:grid-cols-2">
            {data.map(recipe => (
              <button
                key={recipe.id}
                onClick={() => openEdit(recipe)}
                className="text-left border border-divider rounded-xl p-4 bg-bg shadow-sm hover:bg-surface transition-colors cursor-pointer"
              >
                <h3 className="font-medium text-sm text-text-primary">{recipe.name}</h3>
                {recipe.description && (
                  <p className="text-xs text-text-secondary mt-1 line-clamp-2">{recipe.description}</p>
                )}
                <div className="flex items-center gap-3 mt-2">
                  <span className="text-xs text-text-secondary">
                    {t('bouquets.stems', { count: totalStems(recipe) })}
                  </span>
                  <span className="text-xs font-medium text-accent">{formatPrice(recipe.priceSek)}</span>
                </div>
              </button>
            ))}
          </div>
        )}
      </div>

      <Dialog open={showAdd} onClose={() => { setShowAdd(false); resetForm() }} title={t('bouquets.new')} actions={
        <>
          <button onClick={() => { setShowAdd(false); resetForm() }} className="px-4 py-2 text-sm text-text-secondary">{t('common.cancel')}</button>
          <button
            onClick={() => createMut.mutate()}
            disabled={!formName || createMut.isPending}
            className="btn-primary text-sm"
          >
            {createMut.isPending ? t('common.saving') : t('common.add')}
          </button>
        </>
      }>
        {formFields}
      </Dialog>

      <Dialog open={editItem !== null} onClose={() => { setEditItem(null); resetForm() }} title={t('bouquets.edit')} actions={
        <>
          <button onClick={() => { setEditItem(null); resetForm() }} className="px-4 py-2 text-sm text-text-secondary">{t('common.cancel')}</button>
          <button
            onClick={() => updateMut.mutate()}
            disabled={!formName || updateMut.isPending}
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
          {t('bouquets.deleteRecipe')}
        </button>
      </Dialog>

      <Dialog open={deleteItem !== null} onClose={() => { setDeleteItem(null); setDeleteError(null) }} title={t('bouquets.deleteRecipe')} actions={
        <>
          <button onClick={() => { setDeleteItem(null); setDeleteError(null) }} className="px-4 py-2 text-sm text-text-secondary">{t('common.cancel')}</button>
          <button onClick={() => deleteItem && deleteMut.mutate(deleteItem.id)} className="px-4 py-2 text-sm text-error font-semibold">{t('common.delete')}</button>
        </>
      }>
        <p className="text-text-secondary">{t('bouquets.deleteConfirm')}</p>
        {deleteError && <p className="text-error text-sm mt-2">{deleteError}</p>}
      </Dialog>
    </div>
  )
}
