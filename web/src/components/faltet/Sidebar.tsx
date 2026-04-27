import { useState } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { useQuery } from '@tanstack/react-query'
import { useAuth } from '../../auth/AuthContext'
import { api } from '../../api/client'

type NavItem = { to: string; label: string; advanced?: boolean }
type NavGroup = { header: string; items: NavItem[] }

function useGroups(): NavGroup[] {
  const { t } = useTranslation()
  const { user } = useAuth()
  const showAdvanced = user?.advancedMode ?? false

  // Substitute the generic "Gardens" link with a direct link to the user's
  // sole garden (by name) when they only have one.
  const { data: gardens } = useQuery({
    queryKey: ['gardens'],
    queryFn: api.gardens.list,
    enabled: !!user,
    staleTime: 60_000,
  })
  const gardensItem: NavItem =
    gardens && gardens.length === 1
      ? { to: `/garden/${gardens[0].id}`, label: gardens[0].name }
      : { to: '/gardens', label: t('nav.gardens') }

  const allGroups: NavGroup[] = [
    {
      header: t('sidebar.groups.odling'),
      items: [
        { to: '/',               label: t('nav.dashboard') },
        gardensItem,
        { to: '/species',        label: t('nav.species') },
        { to: '/species-groups', label: t('nav.speciesGroups') },
        { to: '/plants',         label: t('nav.plants') },
        { to: '/workflows',      label: t('nav.workflows') },
        { to: '/successions',    label: t('nav.successions'), advanced: true },
        { to: '/seasons',        label: t('nav.seasons') },
        { to: '/tray-locations', label: t('nav.trayLocations') },
      ],
    },
    {
      header: t('sidebar.groups.uppgifter'),
      items: [
        { to: '/tasks',      label: t('nav.tasks') },
        { to: '/calendar',   label: t('nav.calendar'),  advanced: true },
        { to: '/targets',    label: t('nav.targets'),   advanced: true },
        { to: '/seed-stock', label: t('nav.seeds') },
        { to: '/supplies',   label: t('nav.supplies') },
      ],
    },
    {
      header: t('sidebar.groups.sales'),
      items: [
        { to: '/analytics?tab=harvest', label: t('nav.harvest'),  advanced: true },
        { to: '/customers',             label: t('nav.customers') },
        { to: '/bouquets',              label: t('nav.bouquets') },
        { to: '/bouquet-recipes',       label: t('nav.bouquetRecipes'), advanced: true },
      ],
    },
    {
      header: t('sidebar.groups.analysis'),
      items: [
        { to: '/trials',       label: t('nav.trials'),      advanced: true },
        { to: '/pest-disease', label: t('nav.pestDisease'), advanced: true },
        { to: '/analytics',    label: t('nav.analytics'),   advanced: true },
      ],
    },
    {
      header: t('sidebar.groups.account'),
      items: [
        { to: '/guide',        label: t('nav.guide') },
        { to: '/org/settings', label: t('nav.orgSettings') },
        { to: '/account',      label: t('nav.account') },
      ],
    },
  ]

  return allGroups
    .map((g) => ({
      ...g,
      items: showAdvanced ? g.items : g.items.filter((i) => !i.advanced),
    }))
    .filter((g) => g.items.length > 0)
}

export function Sidebar() {
  const { t, i18n } = useTranslation()
  const location = useLocation()
  const groups = useGroups()
  const accountHeader = t('sidebar.groups.account')
  const topGroups = groups.filter((g) => g.header !== accountHeader)
  const accountGroup = groups.find((g) => g.header === accountHeader)
  const [drawerOpen, setDrawerOpen] = useState(false)
  const currentPath = location.pathname + location.search

  const content = (
    <nav
      style={{
        display: 'flex',
        flexDirection: 'column',
        height: '100%',
        width: 220,
        background: 'var(--color-cream)',
        borderRight: '1px solid var(--color-ink)',
        padding: '24px 16px',
        boxSizing: 'border-box',
      }}
    >
      <div>
        <div
          style={{
            fontFamily: 'var(--font-display)',
            fontStyle: 'italic',
            fontSize: 26,
            fontWeight: 300,
            color: 'var(--color-ink)',
            lineHeight: 1,
          }}
        >
          Verdant<span style={{ color: 'var(--color-accent)' }}>.</span>
        </div>
        <div
          style={{
            fontFamily: 'var(--font-mono)',
            fontSize: 10,
            letterSpacing: '0.08em',
            textTransform: 'uppercase',
            color: 'var(--color-forest)',
            marginTop: 6,
          }}
        >
          {t('app.subtitle')}
        </div>
      </div>

      <div style={{ height: 1, background: 'var(--color-ink)', margin: '16px 0' }} />

      <div style={{ flex: 1, display: 'flex', flexDirection: 'column', overflowY: 'auto' }}>
        {topGroups.map((group) => (
          <GroupBlock key={group.header} group={group} currentPath={currentPath} onNavigate={() => setDrawerOpen(false)} />
        ))}

        {accountGroup && (
          <div style={{ marginTop: 'auto' }}>
            <GroupBlock group={accountGroup} currentPath={currentPath} onNavigate={() => setDrawerOpen(false)} />
          </div>
        )}
      </div>

      <div
        style={{
          display: 'flex',
          gap: 12,
          marginTop: 16,
          paddingTop: 12,
          borderTop: '1px solid color-mix(in srgb, var(--color-ink) 20%, transparent)',
          fontFamily: 'var(--font-mono)',
          fontSize: 10,
          letterSpacing: 1.4,
          textTransform: 'uppercase',
        }}
      >
        {['sv', 'en'].map((lng) => (
          <button
            key={lng}
            onClick={() => {
              localStorage.setItem('verdant-lang', lng)
              i18n.changeLanguage(lng)
            }}
            style={{
              background: 'transparent',
              border: 'none',
              cursor: 'pointer',
              color: i18n.language === lng ? 'var(--color-accent)' : 'var(--color-forest)',
              padding: 0,
              fontFamily: 'inherit',
              fontSize: 'inherit',
              letterSpacing: 'inherit',
              textTransform: 'inherit',
            }}
          >
            {lng.toUpperCase()}
          </button>
        ))}
      </div>
    </nav>
  )

  return (
    <>
      {/* Mobile hamburger — visible <768px */}
      <button
        onClick={() => setDrawerOpen(true)}
        className="md:hidden"
        style={{
          position: 'fixed',
          top: 'calc(10px + env(safe-area-inset-top))',
          left: 'calc(10px + env(safe-area-inset-left))',
          zIndex: 30,
          background: 'var(--color-cream)',
          border: '1px solid var(--color-ink)',
          padding: '6px 10px',
          cursor: 'pointer',
          fontFamily: 'var(--font-mono)',
          fontSize: 16,
        }}
        aria-label="Menu"
      >
        &#8801;
      </button>

      {/* Desktop sidebar */}
      <div className="hidden md:block" style={{ height: '100vh', position: 'sticky', top: 0 }}>
        {content}
      </div>

      {/* Mobile drawer */}
      {drawerOpen && (
        <div
          onClick={() => setDrawerOpen(false)}
          style={{
            position: 'fixed',
            inset: 0,
            background: 'rgba(30,36,29,0.55)',
            zIndex: 40,
          }}
        >
          <div
            onClick={(e) => e.stopPropagation()}
            style={{ position: 'absolute', top: 0, left: 0, height: '100vh' }}
          >
            {content}
          </div>
        </div>
      )}
    </>
  )
}

function GroupBlock({ group, currentPath, onNavigate }: { group: NavGroup; currentPath: string; onNavigate: () => void }) {
  return (
    <div style={{ marginBottom: 18 }}>
      <div
        style={{
          fontFamily: 'var(--font-mono)',
          fontSize: 9,
          letterSpacing: 1.4,
          textTransform: 'uppercase',
          color: 'var(--color-forest)',
          opacity: 0.7,
          padding: '12px 0 6px',
        }}
      >
        § {group.header}
      </div>
      {group.items.map((item) => {
        // Items with a query string must match exactly (e.g. /analytics?tab=harvest
        // should not collide with plain /analytics). Items without a query match on
        // prefix so /gardens/123 highlights the Gardens entry.
        const hasQuery = item.to.includes('?')
        const active = hasQuery
          ? currentPath === item.to
          : currentPath === item.to
            || (item.to !== '/' && (currentPath === item.to || currentPath.startsWith(item.to + '/')))
        return (
          <Link
            key={item.to}
            to={item.to}
            onClick={onNavigate}
            style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              padding: '10px 0',
              borderBottom: '1px solid color-mix(in srgb, var(--color-ink) 20%, transparent)',
              fontFamily: 'var(--font-display)',
              fontStyle: 'italic',
              fontSize: 16,
              color: active ? 'var(--color-accent)' : 'var(--color-ink)',
              textDecoration: 'none',
              fontVariationSettings: '"SOFT" 100, "opsz" 144',
            }}
          >
            <span>{item.label}</span>
            {active && <span style={{ color: 'var(--color-accent)' }}>&#9679;</span>}
          </Link>
        )
      })}
    </div>
  )
}
