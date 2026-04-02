import { useTranslation } from 'react-i18next'
import { useOnboarding } from './OnboardingContext'

export function OnboardingFab() {
  const { isActive, completedCount, totalCount, drawerOpen, setDrawerOpen, minimized } = useOnboarding()
  const { t } = useTranslation()

  if (!isActive || minimized) return null

  const progress = completedCount / totalCount
  const radius = 20
  const circumference = 2 * Math.PI * radius
  const offset = circumference - progress * circumference

  return (
    <button
      onClick={() => setDrawerOpen(!drawerOpen)}
      className="fixed bottom-6 right-6 z-40 w-14 h-14 rounded-full bg-white border border-divider shadow-lg hover:shadow-xl transition-shadow duration-200 flex items-center justify-center group"
      aria-label={t('onboarding.fab.label')}
      title={t('onboarding.fab.title', { completed: completedCount, total: totalCount })}
    >
      <svg className="absolute inset-0 w-14 h-14 -rotate-90" viewBox="0 0 56 56">
        <circle
          cx="28" cy="28" r={radius}
          fill="none"
          stroke="currentColor"
          strokeWidth="3"
          className="text-divider"
        />
        <circle
          cx="28" cy="28" r={radius}
          fill="none"
          stroke="currentColor"
          strokeWidth="3"
          strokeLinecap="round"
          strokeDasharray={circumference}
          strokeDashoffset={offset}
          className="text-accent transition-all duration-500"
        />
      </svg>
      <span className="text-lg relative z-10 group-hover:scale-110 transition-transform duration-150">🌿</span>
    </button>
  )
}
