import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useState, useEffect } from 'react'
import { useTranslation } from 'react-i18next'
import { api, type BouquetRecipeResponse, type SpeciesResponse } from '../api/client'
import { ErrorDisplay } from '../components/ErrorDisplay'
import { Dialog } from '../components/Dialog'
import { SpeciesAutocomplete } from '../components/SpeciesAutocomplete'
import { useOnboarding } from '../onboarding/OnboardingContext'
import { Masthead, Chip, LedgerPagination } from '../components/faltet'

const ROLES = ['FLOWER', 'FOLIAGE', 'FILLER', 'ACCENT'] as const

interface FormItem {
  species: SpeciesResponse | null
  stemCount: string
  role: string
}

const TEMPLATE = '60px 1.5fr 140px 120px 40px'

const headerStyle: React.CSSProperties = {
  display: 'grid',
  gridTemplateColumns: TEMPLATE,
  gap: 18,
  padding: '10px 0',
  borderBottom: '1px solid var(--color-ink)',
  fontFamily: 'var(--font-mono)',
  fontSize: 9,
  letterSpacing: 1.4,
  textTransform: 'uppercase',
  color: 'var(--color-forest)',
  opacity: 0.7,
  alignItems: 'center',
}

export function BouquetRecipes() {
  const qc = useQueryClient()
  const { t } = useTranslation()
  const { completeStep, isActive, isStepComplete } = useOnboarding()
  const { data, error, isLoading, refetch } = useQuery({
    queryKey: ['bouquet-recipes'],
    queryFn: () => api.bouquetRecipes.list(),
  })

  const [expanded, setExpanded] = useState<Set<number>>(new Set())
  const toggle = (id: number) => setExpanded(prev => {
    const next = new Set(prev)
    next.has(id) ? next.delete(id) : next.add(id)
    return next
  })

  const [page, setPage] = useState(0)
  const pageSize = 50
  useEffect(() => { setPage(0) }, [data])

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
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['bouquet-recipes'] }); setShowAdd(false); resetForm(); completeStep('create_bouquet') },
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

  const recipes = data ?? []

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
      <Masthead
        left={t('nav.bouquets')}
        center="— Bukettliggaren —"
        right={
          <button onClick={openAdd} className="btn-primary" data-onboarding="add-bouquet-btn">
            {t('bouquets.new')}
          </button>
        }
      />

      <div style={{ padding: '28px 40px' }}>
        {recipes.length === 0 && (
          isActive && !isStepComplete('create_bouquet') ? (
            <div className="bg-accent-light/50 border border-accent/15 rounded-2xl px-6 py-6 text-center">
              <div className="w-10 h-10 rounded-full bg-accent/10 flex items-center justify-center mx-auto mb-3">
                <span className="text-xl">💐</span>
              </div>
              <p className="font-semibold text-text-primary">{t('onboarding.steps.create_bouquet')}</p>
              <p className="text-sm text-text-secondary mt-1 max-w-md mx-auto">{t('onboarding.hints.create_bouquet')}</p>
              <button onClick={openAdd} className="btn-primary mt-4">
                {t('bouquets.new')}
              </button>
            </div>
          ) : (
            <div style={{ padding: '40px 0', textAlign: 'center', borderBottom: '1px solid var(--color-ink)', borderTop: '1px solid var(--color-ink)' }}>
              <div style={{ fontFamily: 'var(--font-mono)', fontSize: 9, letterSpacing: 1.4, textTransform: 'uppercase', color: 'var(--color-forest)', opacity: 0.7 }}>
                {t('bouquets.noRecipes')}
              </div>
            </div>
          )
        )}

        {recipes.length > 0 && (
          <>
            {/* Header row */}
            <div style={headerStyle}>
              <span>№</span>
              <span>{t('bouquets.col.name')}</span>
              <span>{t('bouquets.col.price')}</span>
              <span>{t('bouquets.col.items')}</span>
              <span />
            </div>

            {/* Body rows */}
            {recipes.slice(page * pageSize, (page + 1) * pageSize).map((recipe, i) => {
              const globalIndex = page * pageSize + i
              return (
              <div key={recipe.id}>
                <button
                  onClick={() => toggle(recipe.id)}
                  style={{
                    display: 'grid',
                    gridTemplateColumns: TEMPLATE,
                    gap: 18,
                    padding: '14px 0',
                    borderBottom: '1px solid color-mix(in srgb, var(--color-ink) 20%, transparent)',
                    width: '100%',
                    background: 'transparent',
                    border: 'none',
                    textAlign: 'left',
                    cursor: 'pointer',
                    alignItems: 'center',
                  }}
                  className="ledger-row"
                >
                  <span style={{ fontFamily: 'var(--font-display)', fontStyle: 'italic', fontSize: 22, color: 'var(--color-accent)' }}>
                    {String(globalIndex + 1).padStart(2, '0')}
                  </span>
                  <div>
                    <div style={{ fontFamily: 'var(--font-display)', fontSize: 20 }}>{recipe.name}</div>
                    {recipe.description && (
                      <div style={{ fontFamily: 'var(--font-display)', fontStyle: 'italic', fontSize: 13, color: 'var(--color-forest)', marginTop: 2 }}>
                        {recipe.description}
                      </div>
                    )}
                  </div>
                  <span style={{ fontFamily: 'var(--font-display)', fontStyle: 'italic', color: 'var(--color-accent)' }}>
                    {formatPrice(recipe.priceSek)}
                  </span>
                  <span style={{ fontVariantNumeric: 'tabular-nums' }}>{recipe.items.length}</span>
                  <span style={{ fontFamily: 'var(--font-mono)', fontSize: 12 }}>
                    {expanded.has(recipe.id) ? '▼' : '▶'}
                  </span>
                </button>

                {expanded.has(recipe.id) && (
                  <div style={{
                    padding: '10px 0 10px 78px',
                    borderBottom: '1px solid color-mix(in srgb, var(--color-ink) 20%, transparent)',
                    background: 'color-mix(in srgb, var(--color-ink) 3%, transparent)',
                  }}>
                    {/* Mini-ledger header */}
                    <div style={{ display: 'grid', gridTemplateColumns: '1.5fr 80px 100px 40px', gap: 12, padding: '4px 0 8px', fontFamily: 'var(--font-mono)', fontSize: 9, letterSpacing: 1.4, textTransform: 'uppercase', color: 'var(--color-forest)', opacity: 0.7 }}>
                      <span>{t('common.speciesLabel')}</span>
                      <span>{t('bouquets.stemCount')}</span>
                      <span>{t('bouquets.role')}</span>
                      <span />
                    </div>
                    {recipe.items.map((item) => (
                      <div key={item.id} style={{ display: 'grid', gridTemplateColumns: '1.5fr 80px 100px 40px', gap: 12, padding: '6px 0', borderTop: '1px solid color-mix(in srgb, var(--color-ink) 10%, transparent)', alignItems: 'center' }}>
                        <span style={{ fontFamily: 'var(--font-display)', fontSize: 15 }}>{item.speciesName}</span>
                        <span style={{ fontVariantNumeric: 'tabular-nums', fontFamily: 'var(--font-display)', fontSize: 15 }}>{item.stemCount}</span>
                        <Chip tone={
                          item.role === 'FLOWER' ? 'clay' :
                          item.role === 'FOLIAGE' ? 'sage' :
                          item.role === 'FILLER' ? 'mustard' : 'berry'
                        }>
                          {t(`roles.${item.role}`)}
                        </Chip>
                        <span />
                      </div>
                    ))}
                    {recipe.items.length === 0 && (
                      <div style={{ fontFamily: 'var(--font-mono)', fontSize: 10, color: 'var(--color-forest)', opacity: 0.5, padding: '6px 0' }}>
                        —
                      </div>
                    )}
                    <div style={{ marginTop: 10 }}>
                      <button
                        onClick={() => openEdit(recipe)}
                        style={{ fontFamily: 'var(--font-mono)', fontSize: 10, letterSpacing: 1.4, textTransform: 'uppercase', color: 'var(--color-accent)', background: 'transparent', border: 'none', cursor: 'pointer' }}
                      >
                        {t('bouquets.edit')} →
                      </button>
                    </div>
                  </div>
                )}
              </div>
              )
            })}
            <LedgerPagination page={page} pageSize={pageSize} total={recipes.length} onChange={setPage} />
          </>
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
