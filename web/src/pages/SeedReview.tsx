import { useEffect, useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link, useSearchParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { api, type SeasonResponse, type SeedReviewResponse, type VarietyReview } from '../api/client'
import { Masthead } from '../components/faltet'
import { ErrorDisplay } from '../components/ErrorDisplay'

const LABEL_STYLE: React.CSSProperties = {
  fontFamily: 'var(--font-mono)',
  fontSize: 9,
  letterSpacing: 1.4,
  textTransform: 'uppercase',
  color: 'var(--color-forest)',
  opacity: 0.7,
}

const FIGURE_STYLE: React.CSSProperties = {
  fontFamily: 'var(--font-display)',
  fontSize: 20,
  fontVariantNumeric: 'tabular-nums',
}

const todayIso = () => {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

const seasonEnd = (s: SeasonResponse) => s.endDate ?? `${s.year}-12-31`

export function defaultSeasonId(seasons: SeasonResponse[], today = todayIso()): number | undefined {
  if (seasons.length === 0) return undefined
  const byEnd = [...seasons].sort((a, b) => seasonEnd(b).localeCompare(seasonEnd(a)))
  return (byEnd.find(s => seasonEnd(s) < today) ?? byEnd[0]).id
}

const kr = (cents: number) =>
  `${(cents / 100).toLocaleString('sv-SE', { minimumFractionDigits: 2, maximumFractionDigits: 2 })} kr`

function VarietyCard({ variety, value, onChange }: {
  variety: VarietyReview
  value: string
  onChange: (value: string) => void
}) {
  const { t } = useTranslation()
  const max = Math.max(1, ...variety.steps.map(s => s.count))

  return (
    <div data-testid="variety-card" style={{ borderBottom: '1px solid var(--color-ink)', padding: '18px 0' }}>
      <div style={{ fontFamily: 'var(--font-display)', fontSize: 22, marginBottom: 12 }}>{variety.speciesName}</div>
      <div className="stack-mobile" style={{ display: 'grid', gridTemplateColumns: '1.3fr 1fr', gap: 28 }}>
        <div>
          {variety.steps.map(step => (
            <div key={step.eventType} style={{ display: 'grid', gridTemplateColumns: '110px 1fr 40px', gap: 10, alignItems: 'center', padding: '3px 0' }}>
              <span style={{ fontSize: 13 }}>{step.name}</span>
              <div style={{ background: 'color-mix(in srgb, var(--color-ink) 8%, transparent)', height: 10 }}>
                <div
                  data-testid="step-bar"
                  style={{ width: `${Math.min(100, (step.count / max) * 100)}%`, height: '100%', background: 'var(--color-forest)' }}
                />
              </div>
              <span style={{ ...FIGURE_STYLE, fontSize: 15, textAlign: 'right' }}>{step.count}</span>
            </div>
          ))}
        </div>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px 18px', alignContent: 'start' }}>
          <div>
            <div style={LABEL_STYLE}>{t('seedReview.harvested')}</div>
            <div data-testid="harvested" style={FIGURE_STYLE}>{variety.harvestedStems}</div>
          </div>
          <div>
            <div style={LABEL_STYLE}>{t('seedReview.sold')}</div>
            <div data-testid="sold" style={FIGURE_STYLE}>{variety.soldStems}</div>
          </div>
          <div>
            <div style={LABEL_STYLE}>{t('seedReview.cost')}</div>
            <div data-testid="cost" style={FIGURE_STYLE}>{kr(variety.seedCostCents)}</div>
          </div>
          <div>
            <div style={LABEL_STYLE}>{t('seedReview.revenue')}</div>
            <div data-testid="revenue" style={FIGURE_STYLE}>{kr(variety.revenueCents)}</div>
          </div>
          <div>
            <div style={LABEL_STYLE}>{t('seedReview.leftInStock')}</div>
            <div data-testid="stock" style={FIGURE_STYLE}>{variety.leftInStock}</div>
          </div>
          <div>
            <label style={LABEL_STYLE} htmlFor={`order-${variety.speciesId}`}>{t('seedReview.order')}</label>
            <input
              id={`order-${variety.speciesId}`}
              aria-label={`${t('seedReview.order')} ${variety.speciesName}`}
              type="number"
              min={0}
              step={1}
              className="input"
              value={value}
              onChange={e => onChange(e.target.value)}
            />
            <div style={{ ...LABEL_STYLE, marginTop: 4 }}>{t('seedReview.suggested', { count: variety.suggestedOrder })}</div>
          </div>
        </div>
      </div>
    </div>
  )
}

export function SeedReview() {
  const { t } = useTranslation()
  const qc = useQueryClient()
  const [searchParams, setSearchParams] = useSearchParams()

  const seasonsQuery = useQuery({ queryKey: ['seasons'], queryFn: api.seasons.list })
  const seasons = seasonsQuery.data
  const paramId = Number(searchParams.get('seasonId'))
  const seasonId = Number.isFinite(paramId) && paramId > 0
    ? paramId
    : seasons ? defaultSeasonId(seasons) : undefined

  const reviewQuery = useQuery({
    queryKey: ['seed-review', seasonId],
    queryFn: () => api.seedReview.get(seasonId as number),
    enabled: seasonId !== undefined,
  })

  // Edited order fields by species; untouched varieties keep their saved or suggested value.
  const [edits, setEdits] = useState<Record<number, string>>({})
  const [savedNote, setSavedNote] = useState(false)
  useEffect(() => { setEdits({}); setSavedNote(false) }, [seasonId])

  const review = reviewQuery.data
  const valueOf = (v: VarietyReview) => edits[v.speciesId] ?? String(v.savedOrder ?? v.suggestedOrder)
  const lines = useMemo(() => (review?.varieties ?? []).map(v => ({
    speciesId: v.speciesId,
    quantity: Math.max(0, Math.floor(Number(edits[v.speciesId] ?? v.savedOrder ?? v.suggestedOrder)) || 0),
  })), [review, edits])

  const saveMut = useMutation({
    mutationFn: () => api.seedReview.saveOrderList(seasonId as number, lines),
    onSuccess: (saved: SeedReviewResponse) => {
      qc.setQueryData(['seed-review', seasonId], saved)
      setEdits({})
      setSavedNote(true)
    },
  })

  if (seasonsQuery.isLoading || reviewQuery.isLoading) {
    return <div className="flex justify-center p-16"><div className="animate-spin h-8 w-8 border-2 border-accent border-t-transparent rounded-full" /></div>
  }
  if (seasonsQuery.error) return <ErrorDisplay error={seasonsQuery.error} onRetry={seasonsQuery.refetch} />
  if (reviewQuery.error) return <ErrorDisplay error={reviewQuery.error} onRetry={reviewQuery.refetch} />

  return (
    <div>
      <Masthead
        left={t('seedReview.title')}
        center={t('seedReview.masthead')}
        right={<Link to="/seed-stock" className="btn-secondary">{t('seedReview.back')}</Link>}
      />
      <div className="page-body">
        {!seasons?.length ? (
          <p>{t('seedReview.noSeasons')}</p>
        ) : (
          <>
            <div style={{ marginBottom: 16, maxWidth: 280 }}>
              <label style={LABEL_STYLE} htmlFor="review-season">{t('seedReview.season')}</label>
              <select
                id="review-season"
                className="input"
                value={seasonId ?? ''}
                onChange={e => setSearchParams({ seasonId: e.target.value }, { replace: true })}
              >
                {seasons.map(s => <option key={s.id} value={s.id}>{s.name}</option>)}
              </select>
            </div>

            {review?.estimated && (
              <p style={{ fontStyle: 'italic', color: 'var(--color-forest)', margin: '0 0 16px' }}>{t('seedReview.estimated')}</p>
            )}

            {review && review.varieties.length === 0 && <p>{t('seedReview.empty')}</p>}

            {review?.varieties.map(v => (
              <VarietyCard
                key={v.speciesId}
                variety={v}
                value={valueOf(v)}
                onChange={value => { setEdits(prev => ({ ...prev, [v.speciesId]: value })); setSavedNote(false) }}
              />
            ))}

            {review && review.varieties.length > 0 && (
              <div style={{ marginTop: 20, display: 'flex', alignItems: 'center', gap: 14 }}>
                <button className="btn-primary" disabled={saveMut.isPending} onClick={() => saveMut.mutate()}>
                  {saveMut.isPending ? t('seedReview.saving') : t('seedReview.save')}
                </button>
                {savedNote && <span role="status">{t('seedReview.saved')}</span>}
                {saveMut.error && <span role="alert" style={{ color: 'var(--color-clay)' }}>{saveMut.error instanceof Error ? saveMut.error.message : String(saveMut.error)}</span>}
              </div>
            )}
          </>
        )}
      </div>
    </div>
  )
}
