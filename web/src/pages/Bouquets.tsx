import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { api, type BouquetResponse, type SpeciesResponse } from '../api/client'
import { ErrorDisplay } from '../components/ErrorDisplay'
import { Dialog } from '../components/Dialog'
import { SpeciesAutocomplete } from '../components/SpeciesAutocomplete'
import { Masthead, Chip } from '../components/faltet'

const ROLES = ['FLOWER', 'FOLIAGE', 'FILLER', 'ACCENT'] as const

interface FormItem {
  species: SpeciesResponse | null
  speciesName?: string  // for display when seeded from recipe (species not loaded as object)
  speciesId?: number
  stemCount: string
  role: string
}

export function Bouquets() {
  const qc = useQueryClient()
  const { t } = useTranslation()

  const { data: bouquets, error, isLoading, refetch } = useQuery({
    queryKey: ['bouquets'],
    queryFn: api.bouquets.list,
  })
  const { data: recipes } = useQuery({
    queryKey: ['bouquet-recipes'],
    queryFn: api.bouquetRecipes.list,
  })

  const [showAdd, setShowAdd] = useState(false)
  const [editItem, setEditItem] = useState<BouquetResponse | null>(null)
  const [deleteItem, setDeleteItem] = useState<BouquetResponse | null>(null)

  const [formRecipeId, setFormRecipeId] = useState<number | ''>('')
  const [formName, setFormName] = useState('')
  const [formPrice, setFormPrice] = useState('')
  const [formAssembled, setFormAssembled] = useState(() => new Date().toISOString().slice(0, 10))
  const [formNotes, setFormNotes] = useState('')
  const [formItems, setFormItems] = useState<FormItem[]>([])
  const [formError, setFormError] = useState<string | null>(null)

  const resetForm = () => {
    setFormRecipeId('')
    setFormName('')
    setFormPrice('')
    setFormAssembled(new Date().toISOString().slice(0, 10))
    setFormNotes('')
    setFormItems([])
    setFormError(null)
  }

  const openAdd = () => { resetForm(); setShowAdd(true) }

  const seedFromRecipe = (recipeId: number) => {
    const r = recipes?.find(x => x.id === recipeId)
    if (!r) return
    setFormRecipeId(recipeId)
    if (!formName) setFormName(r.name)
    if (!formPrice && r.priceSek != null) setFormPrice(String(r.priceSek))
    setFormItems(r.items.map(it => ({
      species: null,
      speciesName: it.speciesName,
      speciesId: it.speciesId,
      stemCount: String(it.stemCount),
      role: it.role,
    })))
  }

  const openEdit = (b: BouquetResponse) => {
    setFormRecipeId(b.sourceRecipeId ?? '')
    setFormName(b.name)
    setFormPrice(b.priceSek != null ? String(b.priceSek) : '')
    setFormAssembled(b.assembledAt.slice(0, 10))
    setFormNotes(b.notes ?? '')
    setFormItems(b.items.map(it => ({
      species: null,
      speciesName: it.speciesName,
      speciesId: it.speciesId,
      stemCount: String(it.stemCount),
      role: it.role,
    })))
    setFormError(null)
    setEditItem(b)
  }

  const updateItem = (i: number, patch: Partial<FormItem>) =>
    setFormItems(items => items.map((it, idx) => idx === i ? { ...it, ...patch } : it))

  const addItem = () => setFormItems(items => [...items, { species: null, stemCount: '1', role: 'FLOWER' }])
  const removeItem = (i: number) => setFormItems(items => items.filter((_, idx) => idx !== i))

  const buildPayload = () => ({
    sourceRecipeId: formRecipeId === '' ? null : Number(formRecipeId),
    name: formName.trim(),
    priceSek: formPrice ? Number(formPrice) : undefined,
    assembledAt: new Date(formAssembled).toISOString(),
    notes: formNotes || undefined,
    items: formItems
      .filter(it => (it.species?.id ?? it.speciesId) != null && Number(it.stemCount) > 0)
      .map(it => ({
        speciesId: it.species?.id ?? it.speciesId!,
        stemCount: Number(it.stemCount) || 1,
        role: it.role,
      })),
  })

  const createMut = useMutation({
    mutationFn: () => api.bouquets.create(buildPayload()),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['bouquets'] }); setShowAdd(false); resetForm() },
    onError: (err) => { setFormError(err instanceof Error ? err.message : String(err)) },
  })
  const updateMut = useMutation({
    mutationFn: () => api.bouquets.update(editItem!.id, buildPayload()),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['bouquets'] }); setEditItem(null); resetForm() },
    onError: (err) => { setFormError(err instanceof Error ? err.message : String(err)) },
  })
  const deleteMut = useMutation({
    mutationFn: (id: number) => api.bouquets.delete(id),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['bouquets'] }); setDeleteItem(null) },
  })

  if (isLoading) return <div className="flex justify-center p-16"><div className="animate-spin h-8 w-8 border-2 border-accent border-t-transparent rounded-full" /></div>
  if (error) return <ErrorDisplay error={error} onRetry={refetch} />

  const list = bouquets ?? []
  const formIsValid = formName.trim().length > 0 && formItems.some(it => (it.species || it.speciesId) && Number(it.stemCount) > 0)

  const formFields = (
    <div className="space-y-4">
      <div>
        <label className="field-label">{t('bouquets.sourceRecipe')}</label>
        <select
          className="input w-full"
          value={formRecipeId}
          onChange={e => {
            const v = e.target.value
            if (v) seedFromRecipe(Number(v))
            else { setFormRecipeId(''); setFormItems([]) }
          }}
        >
          <option value="">{t('bouquets.noRecipe')}</option>
          {recipes?.map(r => <option key={r.id} value={r.id}>{r.name}</option>)}
        </select>
      </div>
      <div>
        <label className="field-label">{t('bouquets.name')} *</label>
        <input className="input w-full" value={formName} onChange={e => setFormName(e.target.value)} />
      </div>
      <div className="grid grid-cols-2 gap-3">
        <div>
          <label className="field-label">{t('bouquets.assembledAt')}</label>
          <input type="date" className="input w-full" value={formAssembled} onChange={e => setFormAssembled(e.target.value)} />
        </div>
        <div>
          <label className="field-label">{t('bouquets.price')}</label>
          <input type="number" className="input w-full" value={formPrice} onChange={e => setFormPrice(e.target.value)} />
        </div>
      </div>

      <div className="space-y-3">
        <label className="field-label">{t('bouquets.items')}</label>
        {formItems.map((it, i) => (
          <div key={i} className="border border-divider rounded-xl p-3 space-y-2">
            <div className="flex items-center justify-between">
              <span className="text-xs font-medium text-text-secondary">#{i + 1}</span>
              <button onClick={() => removeItem(i)} className="text-xs text-error hover:underline">{t('common.delete')}</button>
            </div>
            <div>
              <label className="field-label">{t('common.speciesLabel')}</label>
              {it.species == null && it.speciesName ? (
                <p className="font-display italic">{it.speciesName} <button onClick={() => updateItem(i, { speciesName: undefined, speciesId: undefined })} className="text-xs underline ml-2">{t('common.change')}</button></p>
              ) : (
                <SpeciesAutocomplete value={it.species} onChange={s => updateItem(i, { species: s, speciesId: s?.id, speciesName: s ? undefined : it.speciesName })} />
              )}
            </div>
            <div className="flex gap-2">
              <div className="flex-1">
                <label className="field-label">{t('bouquets.stemCount')}</label>
                <input type="number" min="1" className="input" value={it.stemCount} onChange={e => updateItem(i, { stemCount: e.target.value })} />
              </div>
              <div className="flex-1">
                <label className="field-label">{t('bouquets.role')}</label>
                <select className="input" value={it.role} onChange={e => updateItem(i, { role: e.target.value })}>
                  {ROLES.map(r => <option key={r} value={r}>{t(`roles.${r}`)}</option>)}
                </select>
              </div>
            </div>
          </div>
        ))}
        <button onClick={addItem} className="btn-secondary text-sm">+ {t('bouquets.addItem')}</button>
      </div>

      <div>
        <label className="field-label">{t('common.notesLabel')}</label>
        <textarea rows={2} className="input w-full" value={formNotes} onChange={e => setFormNotes(e.target.value)} />
      </div>
      {formError && <p className="text-error text-sm">{formError}</p>}
    </div>
  )

  return (
    <div>
      <Masthead
        left={t('nav.bouquets')}
        center=""
        right={<button onClick={openAdd} className="btn-primary">{t('bouquets.build')}</button>}
      />
      <div className="page-body">
        {list.length === 0 ? (
          <div className="empty-state">
            <p className="text-sm text-text-secondary">{t('bouquets.empty')}</p>
          </div>
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
            {list.map(b => (
              <button key={b.id} onClick={() => openEdit(b)} className="list-tile text-left">
                <div className="flex items-baseline justify-between gap-2">
                  <span className="font-display text-xl leading-tight">{b.name}</span>
                  {b.priceSek != null && <span className="font-mono text-xs">{b.priceSek} kr</span>}
                </div>
                <div className="font-mono text-[10px] uppercase tracking-wider text-text-secondary">{b.assembledAt.slice(0, 10)}</div>
                {b.sourceRecipeName && <Chip tone="sage">{b.sourceRecipeName}</Chip>}
                <div className="font-mono text-[10px] uppercase tracking-wider text-text-secondary mt-auto">
                  {b.items.reduce((s, it) => s + it.stemCount, 0)} {t('bouquets.stems')}
                </div>
              </button>
            ))}
          </div>
        )}
      </div>

      <Dialog
        open={showAdd}
        onClose={() => { setShowAdd(false); resetForm() }}
        title={t('bouquets.buildTitle')}
        actions={<>
          <button onClick={() => { setShowAdd(false); resetForm() }} className="px-4 py-2 text-sm text-text-secondary">{t('common.cancel')}</button>
          <button onClick={() => createMut.mutate()} disabled={!formIsValid || createMut.isPending} className="btn-primary text-sm">{createMut.isPending ? t('common.saving') : t('common.save')}</button>
        </>}
      >{formFields}</Dialog>

      <Dialog
        open={editItem !== null}
        onClose={() => { setEditItem(null); resetForm() }}
        title={t('bouquets.editTitle')}
        actions={<>
          <button onClick={() => editItem && setDeleteItem(editItem)} className="px-4 py-2 text-sm text-error">{t('common.delete')}</button>
          <button onClick={() => { setEditItem(null); resetForm() }} className="px-4 py-2 text-sm text-text-secondary">{t('common.cancel')}</button>
          <button onClick={() => updateMut.mutate()} disabled={!formIsValid || updateMut.isPending} className="btn-primary text-sm">{updateMut.isPending ? t('common.saving') : t('common.save')}</button>
        </>}
      >{formFields}</Dialog>

      <Dialog
        open={deleteItem !== null}
        onClose={() => setDeleteItem(null)}
        title={t('common.delete')}
        actions={<>
          <button onClick={() => setDeleteItem(null)} className="px-4 py-2 text-sm text-text-secondary">{t('common.cancel')}</button>
          <button onClick={() => deleteItem && deleteMut.mutate(deleteItem.id)} className="px-4 py-2 text-sm text-error font-semibold">{t('common.delete')}</button>
        </>}
      >
        <p>{t('bouquets.deleteConfirm', { name: deleteItem?.name })}</p>
      </Dialog>
    </div>
  )
}
