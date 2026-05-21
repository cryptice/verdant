// web/src/pages/Dashboard.tsx
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Link, useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import type { TFunction } from 'i18next'
import { api } from '../api/client'
import type { ScheduledTaskResponse, TraySummaryEntry } from '../api/client'

// Activity types whose subject is a bed, not a species. Mirrors
// BED_ACTIVITY_TYPES in backend ScheduledTaskService.
const BED_ACTIONS = new Set(['WATER', 'WEED', 'FERTILIZE'])

function activityLabel(type: string, t: TFunction): string {
  return t(`activityType.${type}`, type.replace(/_/g, ' '))
}

/** Headline for a dashboard task row — the *subject* of the action. */
function taskTitle(task: ScheduledTaskResponse, t: TFunction): string {
  if (task.activityType === 'TODO') {
    return task.notes?.trim() || t('activityType.TODO', 'Att göra')
  }
  if (BED_ACTIONS.has(task.activityType) && task.bedName) return task.bedName
  if (task.speciesName) return task.speciesName
  return activityLabel(task.activityType, t)
}

/** Secondary line — the action verb, plus the other side of the relation
 *  (bed for species-tasks, garden for bed-tasks) when present. */
function taskSubject(task: ScheduledTaskResponse, t: TFunction): string | null {
  if (task.activityType === 'TODO') return null
  const action = activityLabel(task.activityType, t)
  if (BED_ACTIONS.has(task.activityType)) {
    return [action, task.gardenName].filter(Boolean).join(' · ')
  }
  return [action, task.bedName].filter(Boolean).join(' · ')
}
import { Masthead } from '../components/faltet'
import { Snackbar, useSnackbar } from '../components/Snackbar'
import { useOnboarding } from '../onboarding/OnboardingContext'

export function Dashboard() {
  const { t } = useTranslation()
  const qc = useQueryClient()
  const navigate = useNavigate()
  const { isActive, completedCount, totalCount, setDrawerOpen } = useOnboarding()

  const { message: toast, show: showToast } = useSnackbar()

  const waterLocationMut = useMutation({
    mutationFn: (vars: { locId: number; locName: string | null }) =>
      api.trayLocations.water(vars.locId).then((r) => ({ ...r, locName: vars.locName })),
    onSuccess: (r) => {
      qc.invalidateQueries({ queryKey: ['tray-summary'] })
      showToast(r.locName ? `Vattnade · ${r.plantsAffected} plantor i ${r.locName}` : `Vattnade · ${r.plantsAffected} plantor`)
    },
    onError: () => showToast('Kunde inte vattna'),
  })


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

  // Tasks with a future earliestDate are "kommande" — they live on the
  // dedicated Tasks list, not on the dashboard's at-a-glance feed.
  const dashboardTasks = tasks?.filter(t => {
    const earliest = t.earliestDate ? new Date(t.earliestDate) : null
    if (earliest == null) return true
    const today = new Date()
    today.setHours(0, 0, 0, 0)
    return earliest <= today
  })

  const { data: harvests } = useQuery({
    queryKey: ['harvest-stats'],
    queryFn: api.stats.harvests,
  })

  const { data: seasons } = useQuery({
    queryKey: ['seasons'],
    queryFn: () => api.seasons.list(),
  })

  // Active season (or most recent) — drives the revenue card.
  const activeSeason = seasons?.find((s) => s.isActive) ?? seasons?.[0]
  const { data: seasonLedger } = useQuery({
    queryKey: ['sale-ledger', activeSeason?.id ?? null],
    queryFn: () => api.sales.list({ seasonId: activeSeason!.id }),
    enabled: !!activeSeason,
  })
  const seasonRevenueKr = Math.round((seasonLedger?.reduce((acc, e) => acc + e.totalCents, 0) ?? 0) / 100)
  const seasonSalesCount = seasonLedger?.length ?? 0

  const activeBedCount = dashboard?.stats.totalBeds ?? beds?.length ?? 0
  const activePlantCount = dashboard?.stats.totalActivePlants ?? 0
  const activeSpeciesCount = dashboard?.stats.totalActiveSpecies ?? 0

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
        <div
          className="stats-band dashboard-hero"
          style={{
            margin: '0 0 28px',
            display: 'flex',
            alignItems: 'stretch',
            gap: 12,
            flexWrap: 'wrap',
          }}
        >
          <CenteredHeroStat value={activeBedCount} label="Bäddar" />
          <CenteredHeroStat value={activePlantCount} label="Plantor" />
          <CenteredHeroStat value={activeSpeciesCount} label="Arter" />
          {activeSeason && (
            <RevenueHeroStat
              valueKr={seasonRevenueKr}
              salesCount={seasonSalesCount}
              seasonName={activeSeason.name}
              onClick={() => navigate('/sales')}
            />
          )}
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
                        navigate(`/species/${row.speciesId}/plants`)
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
            {dashboardTasks?.slice(0, 6).map((task) => (
              <div
                key={task.id}
                style={{
                  display: 'grid',
                  gridTemplateColumns: '1fr 80px',
                  gap: 10,
                  padding: '10px 0',
                  borderBottom: '1px solid color-mix(in srgb, var(--color-ink) 20%, transparent)',
                  alignItems: 'center',
                }}
              >
                <div style={{ display: 'flex', flexDirection: 'column', minWidth: 0 }}>
                  <span
                    style={{
                      fontFamily: 'var(--font-display)',
                      fontSize: 16,
                      overflow: 'hidden',
                      textOverflow: 'ellipsis',
                      whiteSpace: 'nowrap',
                    }}
                  >
                    {taskTitle(task, t)}
                  </span>
                  {taskSubject(task, t) && (
                    <span
                      style={{
                        fontFamily: 'var(--font-mono)',
                        fontSize: 9,
                        letterSpacing: 1.4,
                        textTransform: 'uppercase',
                        color: 'var(--color-forest)',
                        opacity: 0.7,
                        overflow: 'hidden',
                        textOverflow: 'ellipsis',
                        whiteSpace: 'nowrap',
                      }}
                    >
                      {taskSubject(task, t)}
                    </span>
                  )}
                </div>
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
            {(!dashboardTasks || dashboardTasks.length === 0) && (
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
            {beds?.map((b) => (
              <Link
                key={b.id}
                to={`/bed/${b.id}`}
                style={{
                  display: 'grid',
                  gridTemplateColumns: '40px 1fr 20px',
                  gap: 10,
                  padding: '10px 0',
                  borderBottom: '1px solid color-mix(in srgb, var(--color-ink) 20%, transparent)',
                  alignItems: 'center',
                  color: 'inherit',
                  textDecoration: 'none',
                }}
              >
                <span
                  style={{
                    fontFamily: 'var(--font-display)',
                    fontStyle: 'italic',
                    fontSize: 20,
                    color: 'var(--color-sage)',
                    fontVariationSettings: '"SOFT" 100, "opsz" 144',
                  }}
                >
                  №
                </span>
                <div style={{ display: 'flex', flexDirection: 'column', minWidth: 0 }}>
                  <span
                    style={{
                      fontFamily: 'var(--font-display)',
                      fontSize: 16,
                      overflow: 'hidden',
                      textOverflow: 'ellipsis',
                      whiteSpace: 'nowrap',
                    }}
                  >
                    {b.name}
                  </span>
                  {b.gardenName && (
                    <span
                      style={{
                        fontFamily: 'var(--font-mono)',
                        fontSize: 9,
                        letterSpacing: 1.4,
                        textTransform: 'uppercase',
                        color: 'var(--color-forest)',
                        opacity: 0.7,
                        overflow: 'hidden',
                        textOverflow: 'ellipsis',
                        whiteSpace: 'nowrap',
                      }}
                    >
                      {b.gardenName}
                    </span>
                  )}
                </div>
                <span
                  style={{
                    fontFamily: 'var(--font-mono)',
                    fontSize: 12,
                    color: 'var(--color-forest)',
                    textAlign: 'right',
                  }}
                >
                  →
                </span>
              </Link>
            ))}
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

      <Snackbar message={toast} />
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

function RevenueHeroStat({
  valueKr, salesCount, seasonName, onClick,
}: { valueKr: number; salesCount: number; seasonName: string; onClick: () => void }) {
  return (
    <button
      onClick={onClick}
      style={{
        flex: 1,
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        gap: 4,
        padding: '20px 16px',
        border: '1px solid var(--color-ink)',
        borderRadius: 14,
        background: 'var(--color-paper)',
        cursor: 'pointer',
        textAlign: 'center',
      }}
    >
      <div style={{ display: 'flex', alignItems: 'baseline', gap: 6 }}>
        <span
          style={{
            fontFamily: 'var(--font-display)',
            fontStyle: 'italic',
            fontSize: 48,
            lineHeight: 1,
            fontWeight: 300,
            letterSpacing: -1,
            color: 'var(--color-accent)',
            fontVariationSettings: '"SOFT" 100, "opsz" 144',
          }}
        >
          {valueKr.toLocaleString('sv-SE')}
        </span>
        <span
          style={{
            fontFamily: 'var(--font-mono)',
            fontSize: 12,
            letterSpacing: 1.4,
            color: 'var(--color-forest)',
          }}
        >
          KR
        </span>
      </div>
      <div
        style={{
          fontFamily: 'var(--font-mono)',
          fontSize: 10,
          letterSpacing: 1.6,
          textTransform: 'uppercase',
          color: 'var(--color-forest)',
        }}
      >
        {salesCount} försäljningar · {seasonName}
      </div>
    </button>
  )
}

function CenteredHeroStat({ value, label }: { value: number; label: string }) {
  return (
    <div
      style={{
        flex: 1,
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        gap: 6,
        padding: '20px 16px',
        border: '1px solid var(--color-ink)',
        borderRadius: 14,
        background: 'var(--color-paper)',
      }}
    >
      <div
        style={{
          fontFamily: 'var(--font-display)',
          fontStyle: 'italic',
          fontSize: 48,
          lineHeight: 1,
          fontWeight: 300,
          letterSpacing: -1,
          color: 'var(--color-accent)',
          fontVariationSettings: '"SOFT" 100, "opsz" 144',
        }}
      >
        {value}
      </div>
      <div
        style={{
          fontFamily: 'var(--font-mono)',
          fontSize: 10,
          letterSpacing: 1.6,
          textTransform: 'uppercase',
          color: 'var(--color-forest)',
          textAlign: 'center',
        }}
      >
        {label}
      </div>
    </div>
  )
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
