import { useEffect, useRef, useState } from 'react'
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
        { to: '/sales',                 label: t('nav.sales') },
        { to: '/outlets',               label: t('nav.outlets') },
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

  const drawerRef = useRef<HTMLDialogElement>(null)

  useEffect(() => {
    const dialog = drawerRef.current
    if (!dialog) return
    if (!drawerOpen) {
      dialog.close()
      return
    }
    dialog.showModal()
    const previousOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    const desktop = window.matchMedia('(min-width: 768px)')
    const closeOnDesktop = () => { if (desktop.matches) setDrawerOpen(false) }
    desktop.addEventListener('change', closeOnDesktop)
    return () => {
      document.body.style.overflow = previousOverflow
      desktop.removeEventListener('change', closeOnDesktop)
    }
  }, [drawerOpen])

  const content = (
    <nav className="app-sidebar" aria-label={t('sidebar.navigation')}>
      <div className="sidebar-brand-row">
        <Link to="/" className="sidebar-brand" onClick={() => setDrawerOpen(false)}>
          Verdant<span> Planner</span><span className="sidebar-brand-dot">.</span>
        </Link>
        <button className="sidebar-close" onClick={() => setDrawerOpen(false)} aria-label={t('common.close')}>×</button>
      </div>
      <div className="sidebar-subtitle">{t('app.subtitle')}</div>

      <div className="sidebar-scroll">
        {topGroups.map((group) => (
          <GroupBlock key={group.header} group={group} currentPath={currentPath} onNavigate={() => setDrawerOpen(false)} />
        ))}
        {accountGroup && <GroupBlock group={accountGroup} currentPath={currentPath} onNavigate={() => setDrawerOpen(false)} />}
      </div>

      <div className="sidebar-footer">
        <span>{t('language.label')}</span>
        <div className="sidebar-languages">
          {['sv', 'en'].map((lng) => (
            <button
              key={lng}
              aria-label={t(`language.${lng}`)}
              aria-pressed={i18n.language.startsWith(lng)}
              onClick={() => {
                localStorage.setItem('verdant-lang', lng)
                i18n.changeLanguage(lng)
              }}
            >
              {lng.toUpperCase()}
            </button>
          ))}
        </div>
      </div>
    </nav>
  )

  return (
    <>
      <button
        onClick={() => setDrawerOpen(true)}
        className="sidebar-toggle"
        aria-label={t('sidebar.openMenu')}
        aria-expanded={drawerOpen}
        aria-controls="mobile-navigation"
      >
        <span aria-hidden="true">☰</span>
      </button>
      <aside className="sidebar-desktop">{content}</aside>
      <dialog
        ref={drawerRef}
        id="mobile-navigation"
        className="sidebar-drawer"
        aria-label={t('sidebar.navigation')}
        onClose={() => setDrawerOpen(false)}
        onClick={(event) => { if (event.target === event.currentTarget) setDrawerOpen(false) }}
      >
        {content}
      </dialog>
    </>
  )
}

function GroupBlock({ group, currentPath, onNavigate }: { group: NavGroup; currentPath: string; onNavigate: () => void }) {
  const pathname = currentPath.split('?')[0]
  return (
    <div className="sidebar-group">
      <div className="sidebar-group-title">{group.header}</div>
      {group.items.map((item) => {
        const hasQuery = item.to.includes('?')
        const active = hasQuery
          ? currentPath === item.to
          : pathname === item.to || (item.to !== '/' && pathname.startsWith(item.to + '/'))
        // A dedicated query-specific page takes precedence over its base route.
        const selected = active && !(item.to === '/analytics' && currentPath.includes('tab=harvest'))
        return (
          <Link
            key={item.to}
            to={item.to}
            onClick={onNavigate}
            className="sidebar-link"
            aria-current={selected ? 'page' : undefined}
          >
            <span>{item.label}</span>
            {selected && <span className="sidebar-current" aria-hidden="true">↗</span>}
          </Link>
        )
      })}
    </div>
  )
}
