import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useOnboarding } from './OnboardingContext'
import { SECTIONS, getStepsForSection } from './steps'

export function OnboardingDrawer() {
  const { t } = useTranslation()
  const {
    drawerOpen, setDrawerOpen, isStepComplete, sectionProgress,
    startStep, completedCount, totalCount,
    minimizeForSession, dismissPermanently,
  } = useOnboarding()
  const [expandedSection, setExpandedSection] = useState<string>(SECTIONS[0].id)
  const [showDismissMenu, setShowDismissMenu] = useState(false)

  const allComplete = completedCount >= totalCount

  if (!drawerOpen) return null

  return (
    <>
      {/* Backdrop */}
      <div className="fixed inset-0 z-40 bg-black/10" onClick={() => setDrawerOpen(false)} />

      {/* Drawer */}
      <div className="fixed right-0 top-0 bottom-0 z-50 w-80 max-w-[90vw] bg-white border-l border-divider shadow-xl flex flex-col">
        {/* Header */}
        <div className="px-4 py-4 border-b border-divider flex items-center justify-between">
          <div>
            <h2 className="font-semibold text-base">{t('onboarding.drawer.title')}</h2>
            <p className="text-xs text-text-secondary mt-0.5">
              {t('onboarding.drawer.progress', { completed: completedCount, total: totalCount })}
            </p>
          </div>
          <div className="flex items-center gap-1">
            <div className="relative">
              <button
                onClick={() => setShowDismissMenu(!showDismissMenu)}
                className="text-text-muted hover:text-text-secondary p-1 rounded-lg hover:bg-surface transition-colors text-sm"
                aria-label={t('onboarding.drawer.options')}
              >
                ···
              </button>
              {showDismissMenu && (
                <div className="absolute right-0 top-8 bg-white border border-divider rounded-xl shadow-lg py-1 w-48 z-10">
                  <button
                    onClick={() => { minimizeForSession(); setShowDismissMenu(false) }}
                    className="w-full text-left px-3 py-2 text-sm text-text-secondary hover:bg-surface transition-colors"
                  >
                    {t('onboarding.drawer.hideSession')}
                  </button>
                  <button
                    onClick={() => { dismissPermanently(); setShowDismissMenu(false) }}
                    className="w-full text-left px-3 py-2 text-sm text-error hover:bg-surface transition-colors"
                  >
                    {t('onboarding.drawer.dismissForever')}
                  </button>
                </div>
              )}
            </div>
            <button
              onClick={() => setDrawerOpen(false)}
              className="text-text-muted hover:text-text-secondary p-1 rounded-lg hover:bg-surface transition-colors"
              aria-label={t('common.close')}
            >
              ✕
            </button>
          </div>
        </div>

        {/* Progress bar */}
        <div className="px-4 pt-3">
          <div className="h-1.5 bg-surface rounded-full overflow-hidden">
            <div
              className="h-full bg-accent rounded-full transition-all duration-500"
              style={{ width: `${(completedCount / totalCount) * 100}%` }}
            />
          </div>
        </div>

        {/* Sections */}
        <div className="flex-1 overflow-y-auto px-4 py-3 space-y-2">
          {allComplete ? (
            <div className="text-center py-8">
              <p className="text-4xl mb-3">🎉</p>
              <p className="font-semibold text-lg">{t('onboarding.drawer.allComplete')}</p>
              <p className="text-sm text-text-secondary mt-1">{t('onboarding.drawer.allCompleteHint')}</p>
              <button
                onClick={dismissPermanently}
                className="btn-primary mt-4"
              >
                {t('onboarding.drawer.finish')}
              </button>
            </div>
          ) : (
            SECTIONS.map(section => {
              const { completed, total } = sectionProgress(section.id)
              const steps = getStepsForSection(section.id)
              const isExpanded = expandedSection === section.id

              return (
                <div key={section.id} className="border border-divider rounded-xl overflow-hidden">
                  <button
                    onClick={() => setExpandedSection(isExpanded ? '' : section.id)}
                    className="w-full flex items-center gap-2.5 px-3 py-2.5 hover:bg-surface/50 transition-colors"
                  >
                    <span className="text-base">{section.icon}</span>
                    <span className="flex-1 text-left text-sm font-medium">{t(section.titleKey)}</span>
                    <span className="text-xs text-text-secondary">{completed}/{total}</span>
                    <span className={`text-xs text-text-muted transition-transform duration-200 ${isExpanded ? 'rotate-180' : ''}`}>
                      ▾
                    </span>
                  </button>
                  {isExpanded && (
                    <div className="border-t border-divider/50 px-3 py-1.5">
                      {steps.map(step => {
                        const complete = isStepComplete(step.id)
                        return (
                          <button
                            key={step.id}
                            onClick={() => !complete && startStep(step.id)}
                            disabled={complete}
                            className={`w-full flex items-center gap-2.5 px-2 py-2 rounded-lg text-sm transition-colors ${
                              complete
                                ? 'text-text-muted'
                                : 'text-text-primary hover:bg-accent-light cursor-pointer'
                            }`}
                          >
                            <span className={`w-5 h-5 rounded-full border-2 flex items-center justify-center shrink-0 text-xs ${
                              complete
                                ? 'border-accent bg-accent text-white'
                                : 'border-divider'
                            }`}>
                              {complete && '✓'}
                            </span>
                            <span className={complete ? 'line-through' : ''}>{t(`onboarding.steps.${step.id}`)}</span>
                          </button>
                        )
                      })}
                    </div>
                  )}
                </div>
              )
            })
          )}
        </div>
      </div>
    </>
  )
}
