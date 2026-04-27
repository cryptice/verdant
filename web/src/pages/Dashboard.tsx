// web/src/pages/Dashboard.tsx
import { useEffect, useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Link, useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { api } from '../api/client'
import type { TraySummaryEntry } from '../api/client'
import { Masthead, Stat, Chip } from '../components/faltet'
import { TrayActionDialog, type TrayActionEntry } from '../components/TrayActionDialog'
import { useOnboarding } from '../onboarding/OnboardingContext'

export function Dashboard() {
  const { t } = useTranslation()
  const qc = useQueryClient()
  const navigate = useNavigate()
  const { isActive, completedCount, totalCount, setDrawerOpen } = useOnboarding()

  const [toast, setToast] = useState<string | null>(null)
  useEffect(() => {
    if (!toast) return
    const id = window.setTimeout(() => setToast(null), 2500)
    return () => window.clearTimeout(id)
  }, [toast])

  const waterLocationMut = useMutation({
    mutationFn: (vars: { locId: number; locName: string | null }) =>
      api.trayLocations.water(vars.locId).then((r) => ({ ...r, locName: vars.locName })),
    onSuccess: (r) => {
      qc.invalidateQueries({ queryKey: ['tray-summary'] })
      setToast(r.locName ? `Vattnade · ${r.plantsAffected} plantor i ${r.locName}` : `Vattnade · ${r.plantsAffected} plantor`)
    },
    onError: () => setToast('Kunde inte vattna'),
  })

  const [trayDialogOpen, setTrayDialogOpen] = useState(false)
  const [activeTrayEntry, setActiveTrayEntry] = useState<TrayActionEntry | null>(null)

  const { data: dashboard } = useQuery({
    queryKey: ['dashboard'],
    queryFn: api.dashboard,
  })

  const { data: beds } = useQuery({
    queryKey: ['beds'],
    queryFn: () => api.beds.list(),
  })

  const { data: trays } = useQuery({
    queryKey: ['tray-summary'],
    queryFn: () => api.plants.traySummary(),
  })

  const { data: tasks } = useQuery({
    queryKey: ['tasks'],
    queryFn: () => api.tasks.list(),
  })

  const { data: harvests } = useQuery({
    queryKey: ['harvest-stats'],
    queryFn: api.stats.harvests,
  })

  const activeBedCount = dashboard?.stats.totalBeds ?? beds?.length ?? 0

  // Harvest totals: sum totalStems across all species (TODO: wire to season-scoped data)
  const totalStems = harvests?.reduce((acc, h) => acc + h.totalStems, 0) ?? 142

  return (
    <div>
      <Masthead left={t('nav.dashboard')} center={t('dashboard.masthead.center')} />

      {isActive && (
        <div
          className="dashboard-onboarding"
          style={{
            margin: '16px 40px 0',
            padding: '14px 18px',
            background: 'var(--color-paper)',
            border: '1px solid var(--color-ink)',
            display: 'flex',
            alignItems: 'center',
            gap: 18,
          }}
        >
          <div style={{ flex: 1 }}>
            <div style={{ fontFamily: 'var(--font-display)', fontSize: 16 }}>
              {t('dashboard.onboardingWidget.title')}
            </div>
            <div
              style={{
                fontFamily: 'var(--font-mono)',
                fontSize: 10,
                letterSpacing: 1.4,
                textTransform: 'uppercase',
                color: 'var(--color-forest)',
                marginTop: 4,
              }}
            >
              {t('dashboard.onboardingWidget.progress', { completed: completedCount, total: totalCount })}
            </div>
          </div>
          <button className="btn-secondary" onClick={() => setDrawerOpen(true)}>
            {t('dashboard.onboardingWidget.button')}
          </button>
        </div>
      )}

      <div className="dashboard-body page-body">
        {/* Hero — wrapped as a stats-band so the accent stripe + paper surface
            replaces the old full-width ink rule that used to sit below it. */}
        <div className="stats-band dashboard-hero" style={{ margin: '0 0 28px' }}>
          <Stat
            size="large"
            value={activeBedCount}
            unit="×"
            label={t('dashboard.hero.label')}
            hue="sage"
          />
        </div>

        {/* Three content columns */}
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(3, 1fr)',
            gap: 0,
          }}
          className="dashboard-columns"
        >
          {/* Column 1 — Tray summary */}
          <section style={{ padding: '0 22px 0 0', borderRight: '1px solid var(--color-ink)' }}>
            <ColumnHeader title={t('dashboard.trays.title')} />
            {(() => {
              if (!trays || trays.length === 0) {
                return (
                  <p
                    style={{
                      fontFamily: 'var(--font-mono)',
                      fontSize: 10,
                      letterSpacing: 1.4,
                      textTransform: 'uppercase',
                      color: 'var(--color-forest)',
                      opacity: 0.6,
                      marginTop: 12,
                    }}
                  >
                    —
                  </p>
                )
              }
              const groups = groupByLocation(trays)
              return groups.map(([key, entries]) => {
                const [locId, locName] = key
                const total = entries.reduce((acc, e) => acc + e.count, 0)
                return (
                  <div key={`loc_${locId ?? 'none'}`} style={{ marginTop: 16 }}>
                    <div
                      style={{
                        display: 'flex',
                        alignItems: 'center',
                        gap: 8,
                        marginBottom: 4,
                        fontFamily: 'var(--font-mono)',
                        fontSize: 10,
                        letterSpacing: 1.4,
                        textTransform: 'uppercase',
                        color: 'var(--color-forest)',
                      }}
                    >
                      <span style={{ flex: 1 }}>{locName ?? 'Utan plats'}</span>
                      <span>{total} ST</span>
                      {locId !== null && (
                        <>
                          <button
                            onClick={() => waterLocationMut.mutate({ locId, locName: locName ?? null })}
                            style={{
                              background: 'none',
                              border: 'none',
                              color: 'var(--color-accent)',
                              fontFamily: 'var(--font-mono)',
                              fontSize: 11,
                              cursor: 'pointer',
                              padding: '0 4px',
                            }}
                          >
                            Vattna
                          </button>
                          <button
                            onClick={() => navigate(`/tray-locations/${locId}`)}
                            style={{
                              background: 'none',
                              border: 'none',
                              color: 'var(--color-accent)',
                              fontFamily: 'var(--font-mono)',
                              fontSize: 11,
                              cursor: 'pointer',
                              padding: '0 4px',
                            }}
                          >
                            Öppna
                          </button>
                        </>
                      )}
                    </div>
                    {entries.slice(0, 6).map((row, i) => {
                      const clickable = row.speciesId != null
                      const handleClick = () => {
                        if (!clickable) return
                        setActiveTrayEntry({
                          speciesId: row.speciesId!,
                          speciesName: row.speciesName,
                          variantName: row.variantName,
                          status: row.status,
                          count: row.count,
                        })
                        setTrayDialogOpen(true)
                      }
                      return (
                        <div
                          key={`${locId ?? 'none'}_${i}`}
                          role={clickable ? 'button' : undefined}
                          tabIndex={clickable ? 0 : undefined}
                          onClick={handleClick}
                          onKeyDown={(e) => {
                            if (clickable && (e.key === 'Enter' || e.key === ' ')) {
                              e.preventDefault()
                              handleClick()
                            }
                          }}
                          style={{
                            display: 'grid',
                            gridTemplateColumns: '1.5fr 60px 80px',
                            gap: 10,
                            padding: '10px 0',
                            borderBottom: '1px solid color-mix(in srgb, var(--color-ink) 20%, transparent)',
                            fontFamily: 'var(--font-display)',
                            fontSize: 16,
                            cursor: clickable ? 'pointer' : 'default',
                            opacity: clickable ? 1 : 0.6,
                            transition: 'background 120ms',
                          }}
                          onMouseEnter={(e) => {
                            if (clickable) e.currentTarget.style.background =
                              'color-mix(in srgb, var(--color-ink) 4%, transparent)'
                          }}
                          onMouseLeave={(e) => {
                            e.currentTarget.style.background = 'transparent'
                          }}
                        >
                          <span>{row.variantName ? `${row.speciesName} – ${row.variantName}` : row.speciesName}</span>
                          <span style={{ textAlign: 'right', fontVariantNumeric: 'tabular-nums' }}>
                            {row.count}
                          </span>
                          <span
                            style={{
                              fontFamily: 'var(--font-mono)',
                              fontSize: 10,
                              textAlign: 'right',
                              textTransform: 'uppercase',
                              letterSpacing: 1.2,
                              color: 'var(--color-forest)',
                            }}
                          >
                            {row.status}
                          </span>
                        </div>
                      )
                    })}
                  </div>
                )
              })
            })()}
          </section>

          {/* Column 2 — Tasks */}
          <section style={{ padding: '0 22px', borderRight: '1px solid var(--color-ink)' }}>
            <ColumnHeader
              title={t('dashboard.tasks.title')}
              right={
                <Link to="/tasks" style={{ color: 'var(--color-accent)', textDecoration: 'none' }}>
                  →
                </Link>
              }
            />
            {tasks?.slice(0, 6).map((task) => (
              <div
                key={task.id}
                style={{
                  display: 'grid',
                  gridTemplateColumns: '40px 1fr 80px',
                  gap: 10,
                  padding: '10px 0',
                  borderBottom: '1px solid color-mix(in srgb, var(--color-ink) 20%, transparent)',
                  alignItems: 'center',
                }}
              >
                <span
                  style={{
                    fontFamily: 'var(--font-display)',
                    fontStyle: 'italic',
                    fontSize: 20,
                    color: 'var(--color-accent)',
                    fontVariationSettings: '"SOFT" 100, "opsz" 144',
                  }}
                >
                  №
                </span>
                <span style={{ fontFamily: 'var(--font-display)', fontSize: 16 }}>
                  {task.speciesName ?? task.activityType}
                </span>
                <span
                  style={{
                    fontFamily: 'var(--font-mono)',
                    fontSize: 10,
                    textAlign: 'right',
                    letterSpacing: 1.2,
                  }}
                >
                  {task.deadline?.slice(0, 10) ?? '—'}
                </span>
              </div>
            ))}
            {(!tasks || tasks.length === 0) && (
              <p
                style={{
                  fontFamily: 'var(--font-mono)',
                  fontSize: 10,
                  letterSpacing: 1.4,
                  textTransform: 'uppercase',
                  color: 'var(--color-forest)',
                  opacity: 0.6,
                  marginTop: 12,
                }}
              >
                —
              </p>
            )}
          </section>

          {/* Column 3 — Beds */}
          <section style={{ padding: '0 0 0 22px' }}>
            <ColumnHeader title={t('dashboard.beds.title')} />
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6 }}>
              {beds?.map((b) => (
                <Chip key={b.id} tone="sage">
                  № {b.id} · {b.name}
                </Chip>
              ))}
            </div>
            {(!beds || beds.length === 0) && (
              <p
                style={{
                  fontFamily: 'var(--font-mono)',
                  fontSize: 10,
                  letterSpacing: 1.4,
                  textTransform: 'uppercase',
                  color: 'var(--color-forest)',
                  opacity: 0.6,
                  marginTop: 12,
                }}
              >
                —
              </p>
            )}
          </section>
        </div>

        {/* Harvest totals band — dark ink bg + cream text + butter decorative circle */}
        <div
          style={{
            background: 'var(--color-ink)',
            color: 'var(--color-cream)',
            padding: '22px 28px',
            position: 'relative',
            overflow: 'hidden',
            marginTop: 28,
          }}
        >
          {/* Decorative butter circle top-right */}
          <div
            style={{
              position: 'absolute',
              top: -40,
              right: -40,
              width: 140,
              height: 140,
              borderRadius: '50%',
              background: 'var(--color-butter)',
              opacity: 0.2,
            }}
          />
          <div
            style={{
              fontFamily: 'var(--font-display)',
              fontStyle: 'italic',
              fontSize: 26,
              fontVariationSettings: '"SOFT" 100, "opsz" 144',
            }}
          >
            {t('dashboard.harvest.headline', { stems: totalStems, year: 2025 })}{' '}
            <span style={{ color: 'var(--color-blush)' }}>
              {t('dashboard.harvest.season', { year: 2025 })}
            </span>
            .
          </div>
          <div
            style={{
              marginTop: 12,
              fontFamily: 'var(--font-mono)',
              fontSize: 10,
              letterSpacing: 1.4,
              textTransform: 'uppercase',
              display: 'flex',
              gap: 18,
            }}
          >
            {/* TODO: wire bestWeek and delta to real harvest analytics */}
            <span style={{ color: 'var(--color-sage)' }}>
              {t('dashboard.harvest.bestWeek', { week: 32 })}
            </span>
            <span style={{ color: 'var(--color-blush)' }}>+24 % vs 2024 ▲</span>
          </div>
        </div>
      </div>

      <TrayActionDialog
        open={trayDialogOpen}
        entry={activeTrayEntry}
        onClose={() => setTrayDialogOpen(false)}
      />

      {toast && (
        <div
          role="status"
          aria-live="polite"
          style={{
            position: 'fixed',
            bottom: 24,
            left: '50%',
            transform: 'translateX(-50%)',
            background: 'var(--color-ink)',
            color: 'var(--color-cream)',
            padding: '10px 18px',
            borderRadius: 8,
            fontFamily: 'var(--font-mono)',
            fontSize: 11,
            letterSpacing: 1.2,
            boxShadow: '0 6px 24px rgba(0,0,0,0.18)',
            zIndex: 1000,
          }}
        >
          {toast}
        </div>
      )}
    </div>
  )
}

type LocationKey = readonly [number | null, string | null]

function groupByLocation(entries: TraySummaryEntry[]): [LocationKey, TraySummaryEntry[]][] {
  const map = new Map<string, { key: LocationKey; entries: TraySummaryEntry[] }>()
  for (const e of entries) {
    const id = e.trayLocationId ?? null
    const name = e.trayLocationName ?? null
    const k = `${id ?? 'null'}`
    const existing = map.get(k)
    if (existing) existing.entries.push(e)
    else map.set(k, { key: [id, name] as const, entries: [e] })
  }
  return Array.from(map.values())
    .sort((a, b) => {
      const an = a.key[1] ?? '￿'
      const bn = b.key[1] ?? '￿'
      return an.localeCompare(bn, 'sv')
    })
    .map((v) => [v.key, v.entries] as [LocationKey, TraySummaryEntry[]])
}

function ColumnHeader({ title, right }: { title: string; right?: React.ReactNode }) {
  return (
    <div
      style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        gap: 10,
        marginBottom: 12,
      }}
    >
      <span
        style={{
          fontFamily: 'var(--font-display)',
          fontStyle: 'italic',
          fontSize: 22,
          fontVariationSettings: '"SOFT" 100, "opsz" 144',
        }}
      >
        {title}
        <span style={{ color: 'var(--color-accent)' }}>.</span>
      </span>
      {right}
    </div>
  )
}
