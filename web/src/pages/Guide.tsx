import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { PageHeader } from '../components/PageHeader'
import { useAuth } from '../auth/AuthContext'

const basicSections = [
  { id: 'getting-started', icon: '🚀' },
  { id: 'concepts', icon: '💡' },
  { id: 'dashboard', icon: '🌍' },
  { id: 'seasons', icon: '📅' },
  { id: 'gardens', icon: '🏡' },
  { id: 'species', icon: '🌿' },
  { id: 'sowing', icon: '🌰' },
  { id: 'plants', icon: '🌱' },
  { id: 'tasks', icon: '📋' },
  { id: 'seeds', icon: '🫘' },
  { id: 'customers', icon: '👥' },
  { id: 'tutorial', icon: '📖' },
]

const advancedSections = [
  { id: 'successions', icon: '🔄' },
  { id: 'targets', icon: '🎯' },
  { id: 'calendar', icon: '📊' },
  { id: 'bouquets', icon: '💐' },
  { id: 'trials', icon: '🔬' },
  { id: 'pestDisease', icon: '🐛' },
  { id: 'analytics', icon: '📈' },
]

export function Guide() {
  const { t } = useTranslation()
  const { user } = useAuth()
  const [open, setOpen] = useState<string | null>(null)

  function renderSection(s: { id: string; icon: string }) {
    const isOpen = open === s.id
    return (
      <div key={s.id} className="border border-divider rounded-xl overflow-hidden bg-bg shadow-sm">
        <button
          aria-expanded={isOpen}
          onClick={() => setOpen(isOpen ? null : s.id)}
          className="w-full flex items-center gap-3 px-4 py-3 text-left hover:bg-surface transition-colors cursor-pointer"
        >
          <span className="text-lg">{s.icon}</span>
          <span className="font-medium text-sm flex-1">{t(`guide.sections.${s.id}.title`)}</span>
          <span className="text-text-muted text-sm">{isOpen ? '−' : '+'}</span>
        </button>
        {isOpen && (
          <div className="px-4 pb-4 pt-1 text-sm text-text-primary leading-relaxed space-y-3 border-t border-divider">
            {(t(`guide.sections.${s.id}.body`, { returnObjects: true }) as string[]).map((p, i) => {
              if (p.startsWith('##')) return <h3 key={i} className="font-semibold text-base mt-3">{p.replace(/^##\s*/, '')}</h3>
              if (p.startsWith('- ')) return <li key={i} className="ml-4 list-disc">{p.slice(2)}</li>
              if (p.startsWith('|')) return <p key={i} className="font-mono text-xs text-text-secondary">{p}</p>
              return <p key={i}>{p}</p>
            })}
          </div>
        )}
      </div>
    )
  }

  return (
    <div>
      <PageHeader title={t('guide.title')} />
      <div className="px-4 py-2 space-y-2">
        {basicSections.map(renderSection)}
      </div>

      {user?.advancedMode && (
        <>
          <h2 className="px-4 pt-6 pb-2 text-sm font-semibold text-text-secondary uppercase tracking-wide">{t('guide.advanced')}</h2>
          <div className="px-4 py-2 space-y-2">
            {advancedSections.map(renderSection)}
          </div>
        </>
      )}
    </div>
  )
}
