import { useLocation } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { useOnboarding } from './OnboardingContext'

export function OnboardingHint() {
  const { getHintsForRoute, startStep } = useOnboarding()
  const { pathname } = useLocation()
  const { t } = useTranslation()

  const hints = getHintsForRoute(pathname)
  if (hints.length === 0) return null

  // Show the first incomplete step for this route
  const step = hints[0]

  return (
    <div className="bg-accent-light border border-accent/20 rounded-xl px-4 py-3 mb-4 flex items-center gap-3">
      <span className="text-xl">🌿</span>
      <div className="flex-1 min-w-0">
        <p className="text-sm font-medium text-text-primary">{t(`onboarding.steps.${step.id}`)}</p>
        <p className="text-xs text-text-secondary mt-0.5">{t(`onboarding.hints.${step.id}`)}</p>
      </div>
      <button
        onClick={() => startStep(step.id)}
        className="text-xs px-3 py-1.5 rounded-lg bg-accent text-white hover:bg-accent-hover transition-colors font-medium shrink-0"
      >
        {t('onboarding.hint.showMe')}
      </button>
    </div>
  )
}
