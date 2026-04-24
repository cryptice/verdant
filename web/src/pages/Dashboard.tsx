// web/src/pages/Dashboard.tsx
import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { api } from '../api/client'
import { Masthead, Stat, Rule, Chip } from '../components/faltet'
import { useOnboarding } from '../onboarding/OnboardingContext'

export function Dashboard() {
  const { t } = useTranslation()
  const { isActive, completedCount, totalCount, setDrawerOpen } = useOnboarding()

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
        {/* Hero */}
        <div className="dashboard-hero">
          <Stat
            size="large"
            value={activeBedCount}
            unit="×"
            label={t('dashboard.hero.label')}
            hue="sage"
          />
        </div>

        <div style={{ margin: '28px 0' }}>
          <Rule variant="ink" />
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
            {trays?.slice(0, 6).map((row, i) => (
              <div
                key={i}
                style={{
                  display: 'grid',
                  gridTemplateColumns: '1.5fr 60px 80px',
                  gap: 10,
                  padding: '10px 0',
                  borderBottom: '1px solid color-mix(in srgb, var(--color-ink) 20%, transparent)',
                  fontFamily: 'var(--font-display)',
                  fontSize: 16,
                }}
              >
                <span>{row.speciesName}</span>
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
            ))}
            {(!trays || trays.length === 0) && (
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

        <div style={{ margin: '28px 0 16px' }}>
          <Rule variant="ink" />
        </div>

        {/* Harvest totals band — dark ink bg + cream text + butter decorative circle */}
        <div
          style={{
            background: 'var(--color-ink)',
            color: 'var(--color-cream)',
            padding: '22px 28px',
            position: 'relative',
            overflow: 'hidden',
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
    </div>
  )
}

function ColumnHeader({ title, right }: { title: string; right?: React.ReactNode }) {
  return (
    <div
      style={{
        display: 'flex',
        alignItems: 'center',
        gap: 10,
        padding: '0 0 10px',
        borderBottom: '1px solid var(--color-ink)',
        marginBottom: 6,
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
      <Rule inline variant="soft" />
      {right}
    </div>
  )
}
