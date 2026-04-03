import { useLocation } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { useOnboarding } from './OnboardingContext'
import { ONBOARDING_STEPS } from './steps'

export function OnboardingHint() {
  const { getHintsForRoute, startStep } = useOnboarding()
  const { pathname } = useLocation()
  const { t } = useTranslation()

  const hints = getHintsForRoute(pathname)
  if (hints.length === 0) return null

  const step = hints[0]
  const stepDef = ONBOARDING_STEPS.find(s => s.id === step.id)
  const isVisit = stepDef?.completionType === 'visit'

  return (
    <div className="bg-accent-light/50 border border-accent/15 rounded-2xl px-6 py-5 flex items-center gap-4">
      <div className="w-10 h-10 rounded-full bg-accent/10 flex items-center justify-center shrink-0">
        <span className="text-xl">🌿</span>
      </div>
      <div className="flex-1 min-w-0">
        <p className="font-semibold text-text-primary">{t(`onboarding.steps.${step.id}`)}</p>
        <p className="text-sm text-text-secondary mt-1">{t(`onboarding.hints.${step.id}`)}</p>
      </div>
      {!isVisit && (
        <button
          onClick={() => startStep(step.id)}
          className="text-sm px-4 py-2 rounded-xl bg-accent text-white hover:bg-accent-hover transition-colors font-medium shrink-0"
        >
          {t('onboarding.hint.showMe')}
        </button>
      )}
    </div>
  )
}
