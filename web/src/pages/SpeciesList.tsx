import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useState, useMemo } from 'react'
import { useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { api, type SpeciesResponse } from '../api/client'
import { Masthead, Ledger, LedgerFilters, Chip } from '../components/faltet'
import { ErrorDisplay } from '../components/ErrorDisplay'
import { Dialog } from '../components/Dialog'
import { OnboardingHint } from '../onboarding/OnboardingHint'
import type { LedgerFilterOption } from '../components/faltet'
import { matchesAllTokens } from '../utils/search'

const ALL_TYPES = ['ANNUAL', 'BIENNIAL', 'PERENNIAL', 'BULB', 'TUBER'] as const
type PlantType = typeof ALL_TYPES[number]

const PLANT_TYPE_TONE: Record<PlantType, LedgerFilterOption<PlantType>['tone']> = {
  ANNUAL: 'sage',
  BIENNIAL: 'berry',
  PERENNIAL: 'berry',
  BULB: 'mustard',
  TUBER: 'clay',
}

function matchesQuery(s: SpeciesResponse, q: string) {
  return matchesAllTokens(
    [s.commonName, s.commonNameSv, s.variantName, s.variantNameSv, s.scientificName],
    q,
  )
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
  const [types, setTypes] = useState<Set<PlantType>>(new Set(ALL_TYPES))
  const [deleteItem, setDeleteItem] = useState<SpeciesResponse | null>(null)

  const sorted = useMemo(
    () => (data ?? []).filter(s => matchesQuery(s, search)).sort((a, b) => {
      const nameA = (a.commonNameSv ?? a.commonName).toLowerCase()
      const nameB = (b.commonNameSv ?? b.commonName).toLowerCase()
      if (nameA !== nameB) return nameA.localeCompare(nameB, 'sv')
      const varA = (a.variantNameSv ?? a.variantName ?? '').toLowerCase()
      const varB = (b.variantNameSv ?? b.variantName ?? '').toLowerCase()
      return varA.localeCompare(varB, 'sv')
    }),
    [data, search]
  )

  const filtered = useMemo(
    () => sorted.filter(s => types.has((s.plantType as PlantType) ?? 'ANNUAL')),
    [sorted, types]
  )

  const deleteMut = useMutation({
    mutationFn: (id: number) => api.species.delete(id),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['species'] }); setDeleteItem(null) },
  })

  if (isLoading) return <div className="flex justify-center p-16"><div className="animate-spin h-8 w-8 border-2 border-accent border-t-transparent rounded-full" /></div>
  if (error) return <ErrorDisplay error={error} onRetry={refetch} />

  const deleteDisplayName = (s: SpeciesResponse) => {
    const name = i18n.language === 'sv' ? (s.commonNameSv ?? s.commonName) : s.commonName
    const variant = i18n.language === 'sv' ? (s.variantNameSv ?? s.variantName) : s.variantName
    return variant ? `${name} – ${variant}` : name
  }

  return (
    <div>
      <Masthead
        left={t('nav.species')}
        center="— Artliggaren —"
        right={
          <button onClick={() => navigate('/species/new')} className="btn-primary">
            {t('species.newSpecies')}
          </button>
        }
      />
      <OnboardingHint />

      <div className="page-body">
        <div style={{ marginBottom: 16 }}>
          <input
            type="search"
            aria-label={t('common.searchSpecies')}
            value={search}
            onChange={e => setSearch(e.target.value)}
            placeholder={t('common.searchSpecies')}
            style={{
              width: '100%',
              padding: '8px 14px',
              fontFamily: 'var(--font-mono)',
              fontSize: 12,
              letterSpacing: 0.8,
              border: '1px solid var(--color-ink)',
              background: 'transparent',
              color: 'var(--color-ink)',
              outline: 'none',
            }}
          />
        </div>

        <LedgerFilters
          options={ALL_TYPES.map(pt => ({
            id: pt,
            label: t(`plantType.${pt}`),
            tone: PLANT_TYPE_TONE[pt],
          }))}
          value={types}
          onChange={setTypes}
          storageKey="verdant-species-filters"
        />

        <Ledger
          paginated
          pageSize={50}
          columns={[
            {
              key: 'id',
              label: '№',
              hideOnMobile: true,
              width: '60px',
              render: (_s, i) => (
                <span style={{ fontFamily: 'var(--font-display)', fontStyle: 'italic', fontSize: 22, color: 'var(--color-accent)' }}>
                  {String(i + 1).padStart(2, '0')}
                </span>
              ),
            },
            {
              key: 'name',
              label: t('species.col.species'),
              width: '1.5fr',
              render: (s: SpeciesResponse) => {
                const baseName = i18n.language === 'sv' ? (s.commonNameSv ?? s.commonName) : s.commonName
                const variant = i18n.language === 'sv' ? (s.variantNameSv ?? s.variantName) : s.variantName
                return (
                  <div>
                    <div style={{ fontFamily: 'var(--font-display)', fontSize: 20 }}>
                      {variant ? `${baseName} – ${variant}` : baseName}
                    </div>
                    {s.scientificName && (
                      <div style={{ fontFamily: 'var(--font-display)', fontStyle: 'italic', fontSize: 9, color: 'var(--color-sage)' }}>
                        {s.scientificName}
                      </div>
                    )}
                  </div>
                )
              },
            },
            {
              key: 'variant',
              hideOnMobile: true,
              label: t('species.col.variant'),
              width: '1fr',
              render: (s: SpeciesResponse) => (
                <span style={{ fontFamily: 'var(--font-display)', fontStyle: 'italic', color: 'var(--color-accent)' }}>
                  {s.variantNameSv ?? s.variantName ?? ''}
                </span>
              ),
            },
            {
              key: 'plantType',
              label: t('species.col.type'),
              width: '120px',
              render: (s: SpeciesResponse) => {
                const pt = s.plantType as PlantType | undefined
                if (!pt) return null
                return <Chip tone={PLANT_TYPE_TONE[pt]}>{t(`plantType.${pt}`)}</Chip>
              },
            },
            {
              key: 'edit',
              hideOnMobile: true,
              label: '',
              width: '40px',
              align: 'right',
              render: (s: SpeciesResponse) => (
                <button
                  type="button"
                  aria-label={t('common.edit')}
                  title={t('common.edit')}
                  onClick={(e) => { e.stopPropagation(); navigate(`/species/${s.id}`) }}
                  style={{
                    background: 'transparent',
                    border: 'none',
                    color: 'var(--color-accent)',
                    cursor: 'pointer',
                    fontFamily: 'var(--font-mono)',
                    fontSize: 14,
                    padding: 4,
                  }}
                >
                  ✎
                </button>
              ),
            },
            {
              key: 'copy',
              hideOnMobile: true,
              label: '',
              width: '40px',
              align: 'right',
              render: (s: SpeciesResponse) => (
                <button
                  type="button"
                  aria-label={t('species.copy')}
                  title={t('species.copy')}
                  onClick={(e) => { e.stopPropagation(); navigate(`/species/new?from=${s.id}`) }}
                  style={{
                    background: 'transparent',
                    border: 'none',
                    color: 'var(--color-accent)',
                    cursor: 'pointer',
                    fontFamily: 'var(--font-mono)',
                    fontSize: 14,
                    padding: 4,
                  }}
                >
                  ⎘
                </button>
              ),
            },
            {
              key: 'goto',
              label: '',
              width: '40px',
              align: 'right',
              render: () => (
                <span style={{ color: 'var(--color-accent)', fontFamily: 'var(--font-mono)' }}>→</span>
              ),
            },
          ]}
          rows={filtered}
          rowKey={(s: SpeciesResponse) => s.id}
          onRowClick={(s: SpeciesResponse) => navigate(`/species/${s.id}`)}
        />
      </div>


      {deleteItem && (
        <Dialog open={!!deleteItem} title={t('species.deleteSpeciesTitle')} onClose={() => setDeleteItem(null)}>
          <p className="text-sm mb-4">{t('common.delete')} &ldquo;{deleteDisplayName(deleteItem)}&rdquo;?</p>
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
