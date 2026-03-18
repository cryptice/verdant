import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useState, useMemo } from 'react'
import { useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { api, type SpeciesResponse } from '../api/client'
import { PageHeader } from '../components/PageHeader'
import { ErrorDisplay } from '../components/ErrorDisplay'
import { Dialog } from '../components/Dialog'
import { Pagination } from '../components/Pagination'

const PAGE_SIZE = 50

function displayName(s: SpeciesResponse, lang: string) {
  const name = lang === 'sv' ? (s.commonNameSv ?? s.commonName) : s.commonName
  const variant = lang === 'sv' ? (s.variantNameSv ?? s.variantName) : s.variantName
  return variant ? `${name} \u2013 ${variant}` : name
}

function matchesQuery(s: SpeciesResponse, q: string) {
  if (!q) return true
  const lower = q.toLowerCase()
  return [s.commonName, s.commonNameSv, s.variantName, s.variantNameSv, s.scientificName]
    .some(v => v?.toLowerCase().includes(lower))
}

export function SpeciesList() {
  const qc = useQueryClient()
  const navigate = useNavigate()
  const { t, i18n } = useTranslation()
  const { data, error, isLoading, refetch } = useQuery({
    queryKey: ['species'],
    queryFn: api.species.list,
  })

  const [search, setSearch] = useState('')
  const [page, setPage] = useState(0)
  const [showAdd, setShowAdd] = useState(false)
  const [addCommonName, setAddCommonName] = useState('')
  const [addVariantName, setAddVariantName] = useState('')
  const [addVariantNameSv, setAddVariantNameSv] = useState('')
  const [addScientificName, setAddScientificName] = useState('')
  const [deleteItem, setDeleteItem] = useState<SpeciesResponse | null>(null)

  const filtered = useMemo(
    () => (data ?? []).filter(s => matchesQuery(s, search)),
    [data, search]
  )

  const createMut = useMutation({
    mutationFn: () => api.species.create({
      commonName: addCommonName,
      variantName: addVariantName || undefined,
      variantNameSv: addVariantNameSv || undefined,
      scientificName: addScientificName || undefined,
    }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['species'] })
      setShowAdd(false)
      setAddCommonName(''); setAddVariantName(''); setAddVariantNameSv(''); setAddScientificName('')
    },
  })

  const deleteMut = useMutation({
    mutationFn: (id: number) => api.species.delete(id),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['species'] }); setDeleteItem(null) },
  })

  if (isLoading) return <div className="flex justify-center p-16"><div className="animate-spin h-8 w-8 border-2 border-accent border-t-transparent rounded-full" /></div>
  if (error) return <ErrorDisplay error={error} onRetry={refetch} />

  return (
    <div>
      <PageHeader title={t('species.title')} action={{ label: t('species.newSpecies'), onClick: () => setShowAdd(true) }} />

      <div className="px-4 py-3">
        <input
          type="search"
          value={search}
          onChange={e => { setSearch(e.target.value); setPage(0) }}
          placeholder={t('common.searchSpecies')}
          className="w-full px-3 py-2 rounded-lg border border-divider bg-bg text-sm outline-none focus:ring-2 focus:ring-accent"
        />
      </div>

      {filtered.length === 0 && (
        <p className="text-text-secondary text-sm text-center py-8">{t('species.noSpeciesFound')}</p>
      )}

      {filtered.length > 0 && (
        <div className="px-4 pb-24">
          <div className="border border-divider rounded-lg overflow-hidden bg-bg">
            <table className="w-full">
              <thead>
                <tr className="border-b border-divider bg-surface">
                  <th className="text-left px-4 py-2 text-xs font-medium text-text-secondary">{t('species.colName')}</th>
                  <th className="text-left px-4 py-2 text-xs font-medium text-text-secondary">{t('species.scientificName')}</th>
                </tr>
              </thead>
              <tbody>
                {filtered.slice(page * PAGE_SIZE, (page + 1) * PAGE_SIZE).map(s => {
                  const name = s.commonNameSv ?? s.commonName
                  const variant = s.variantNameSv ?? s.variantName
                  return (
                    <tr
                      key={s.id}
                      className="border-b border-divider last:border-0 hover:bg-surface cursor-pointer transition-colors"
                      onClick={() => navigate(`/sow?speciesId=${s.id}`)}
                    >
                      <td className="px-4 py-2.5 text-sm">
                        {name}{variant ? <span className="text-text-secondary"> — {variant}</span> : ''}
                      </td>
                      <td className="px-4 py-2.5 text-sm text-text-secondary italic">{s.scientificName ?? '—'}</td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
          <Pagination page={page} pageSize={PAGE_SIZE} total={filtered.length} onPageChange={setPage} />
        </div>
      )}

      {showAdd && (
        <Dialog open={showAdd} title={t('species.addSpeciesTitle')} onClose={() => setShowAdd(false)}>
          <div className="space-y-4">
            <div>
              <label className="field-label">{t('species.commonName')}</label>
              <input className="input" value={addCommonName} onChange={e => setAddCommonName(e.target.value)} placeholder={t('species.commonNamePlaceholder')} />
            </div>
            <div>
              <label className="field-label">{t('species.variantName')}</label>
              <input className="input" value={addVariantName} onChange={e => setAddVariantName(e.target.value)} placeholder={t('common.optional')} />
            </div>
            <div>
              <label className="field-label">{t('species.variantNameSv')}</label>
              <input className="input" value={addVariantNameSv} onChange={e => setAddVariantNameSv(e.target.value)} placeholder={t('common.optional')} />
            </div>
            <div>
              <label className="field-label">{t('species.scientificName')}</label>
              <input className="input" value={addScientificName} onChange={e => setAddScientificName(e.target.value)} placeholder={t('common.optional')} />
            </div>
            {createMut.error && <p className="text-error text-sm">{String(createMut.error)}</p>}
            <div className="flex justify-end pt-1">
              <button
                className="btn-primary"
                onClick={() => createMut.mutate()}
                disabled={!addCommonName.trim() || createMut.isPending}
              >
                {createMut.isPending ? t('species.adding') : t('species.addSpeciesBtn')}
              </button>
            </div>
          </div>
        </Dialog>
      )}

      {deleteItem && (
        <Dialog open={!!deleteItem} title={t('species.deleteSpeciesTitle')} onClose={() => setDeleteItem(null)}>
          <p className="text-sm mb-4">{t('common.delete')} &ldquo;{displayName(deleteItem, i18n.language)}&rdquo;?</p>
          <div className="flex gap-2">
            <button className="btn-secondary flex-1" onClick={() => setDeleteItem(null)}>{t('common.cancel')}</button>
            <button
              className="btn-primary flex-1 bg-error"
              onClick={() => deleteMut.mutate(deleteItem.id)}
              disabled={deleteMut.isPending}
            >
              {deleteMut.isPending ? t('common.deleting') : t('common.delete')}
            </button>
          </div>
        </Dialog>
      )}
    </div>
  )
}
