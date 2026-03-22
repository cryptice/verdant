import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { PageHeader } from '../components/PageHeader'

const sections = [
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
  { id: 'successions', icon: '🔄' },
  { id: 'targets', icon: '🎯' },
  { id: 'calendar', icon: '📊' },
  { id: 'customers', icon: '👥' },
  { id: 'bouquets', icon: '💐' },
  { id: 'trials', icon: '🔬' },
  { id: 'pestDisease', icon: '🐛' },
  { id: 'analytics', icon: '📈' },
  { id: 'tutorial', icon: '📖' },
] as const

export function Guide() {
  const { t } = useTranslation()
  const [open, setOpen] = useState<string | null>(null)

  return (
    <div>
      <PageHeader title={t('guide.title')} />
      <div className="px-4 py-2 space-y-2">
        {sections.map(s => {
          const isOpen = open === s.id
          return (
            <div key={s.id} className="border border-divider rounded-xl overflow-hidden bg-bg shadow-sm">
              <button
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
        })}
      </div>
    </div>
  )
}
