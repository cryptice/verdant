import { useState, useEffect, useRef } from 'react'
import { useTranslation } from 'react-i18next'
import { useOnboarding } from './OnboardingContext'
import { SECTIONS, getStepsForSection, ONBOARDING_STEPS } from './steps'

export function OnboardingDrawer() {
  const { t } = useTranslation()
  const {
    drawerOpen, setDrawerOpen, isStepComplete, isStepBlocked, sectionProgress,
    startStep, completedCount, totalCount,
    minimizeForSession, dismissPermanently, lastCompletedStepId,
  } = useOnboarding()
  const [expandedSection, setExpandedSection] = useState<string>(SECTIONS[0].id)
  const [showDismissMenu, setShowDismissMenu] = useState(false)
  const [visible, setVisible] = useState(false)
  const [animatingIn, setAnimatingIn] = useState(false)
  const nextStepRef = useRef<HTMLButtonElement>(null)

  const allComplete = completedCount >= totalCount

  // Auto-expand the first section with incomplete steps when drawer opens
  useEffect(() => {
    if (drawerOpen) {
      const firstIncomplete = SECTIONS.find(s => {
        const { completed, total } = sectionProgress(s.id)
        return completed < total
      })
      if (firstIncomplete) setExpandedSection(firstIncomplete.id)
    }
  }, [drawerOpen, sectionProgress])

  // Slide in/out animation
  useEffect(() => {
    if (drawerOpen) {
      setVisible(true)
      requestAnimationFrame(() => requestAnimationFrame(() => setAnimatingIn(true)))
    } else {
      setAnimatingIn(false)
      const timer = setTimeout(() => setVisible(false), 300)
      return () => clearTimeout(timer)
    }
  }, [drawerOpen])

  // Auto-expand the section containing the completed step, then the next incomplete section
  useEffect(() => {
    if (!lastCompletedStepId) return
    const completedStep = ONBOARDING_STEPS.find(s => s.id === lastCompletedStepId)
    if (!completedStep) return

    // Check if this was the last step in its section
    const sectionSteps = getStepsForSection(completedStep.section)
    const allSectionComplete = sectionSteps.every(s =>
      s.id === lastCompletedStepId || isStepComplete(s.id)
    )

    if (allSectionComplete) {
      // Find the next section with incomplete steps
      const currentIdx = SECTIONS.findIndex(s => s.id === completedStep.section)
      for (let i = currentIdx + 1; i < SECTIONS.length; i++) {
        const nextSteps = getStepsForSection(SECTIONS[i].id)
        if (nextSteps.some(s => !isStepComplete(s.id))) {
          setTimeout(() => setExpandedSection(SECTIONS[i].id), 800)
          return
        }
      }
    } else {
      // Keep current section expanded
      setExpandedSection(completedStep.section)
    }
  }, [lastCompletedStepId, isStepComplete])

  // Scroll next step into view after celebration
  useEffect(() => {
    if (lastCompletedStepId && nextStepRef.current) {
      const timer = setTimeout(() => {
        nextStepRef.current?.scrollIntoView({ behavior: 'smooth', block: 'nearest' })
      }, 1200)
      return () => clearTimeout(timer)
    }
  }, [lastCompletedStepId])

  if (!visible) return null

  // Find the next incomplete step (for highlighting)
  const nextIncompleteId = lastCompletedStepId
    ? ONBOARDING_STEPS.find(s => !isStepComplete(s.id) && s.id !== lastCompletedStepId)?.id ?? null
    : null

  return (
    <>
      {/* Backdrop */}
      <div
        className={`fixed inset-0 z-40 transition-opacity duration-300 ${animatingIn ? 'bg-black/10' : 'bg-black/0'}`}
        onClick={() => setDrawerOpen(false)}
      />

      {/* Drawer */}
      <div
        className={`fixed right-0 top-0 bottom-0 z-50 w-80 max-w-[90vw] bg-white border-l border-divider shadow-xl flex flex-col transition-transform duration-300 ease-out ${
          animatingIn ? 'translate-x-0' : 'translate-x-full'
        }`}
      >
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
              className="h-full bg-accent rounded-full transition-all duration-700 ease-out"
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
              const sectionComplete = completed === total

              return (
                <div
                  key={section.id}
                  className={`border rounded-xl overflow-hidden transition-colors duration-500 ${
                    sectionComplete ? 'border-accent/30 bg-accent-light/20' : 'border-divider'
                  }`}
                >
                  <button
                    onClick={() => setExpandedSection(isExpanded ? '' : section.id)}
                    className="w-full flex items-center gap-2.5 px-3 py-2.5 hover:bg-surface/50 transition-colors"
                  >
                    <span className="text-base">{section.icon}</span>
                    <span className="flex-1 text-left text-sm font-medium">{t(section.titleKey)}</span>
                    {sectionComplete ? (
                      <span className="text-xs font-medium text-accent">✓</span>
                    ) : (
                      <span className="text-xs text-text-secondary">{completed}/{total}</span>
                    )}
                    <span className={`text-xs text-text-muted transition-transform duration-200 ${isExpanded ? 'rotate-180' : ''}`}>
                      ▾
                    </span>
                  </button>
                  <div
                    className={`grid transition-all duration-300 ease-out ${
                      isExpanded ? 'grid-rows-[1fr]' : 'grid-rows-[0fr]'
                    }`}
                  >
                    <div className="overflow-hidden">
                      <div className="border-t border-divider/50 px-3 py-1.5">
                        {steps.map(step => {
                          const complete = isStepComplete(step.id)
                          const justCompleted = step.id === lastCompletedStepId
                          const isNext = step.id === nextIncompleteId
                          const blocked = isStepBlocked(step.id)

                          return (
                            <button
                              key={step.id}
                              ref={isNext ? nextStepRef : undefined}
                              onClick={() => !complete && !blocked && startStep(step.id)}
                              disabled={complete || blocked}
                              className={`relative w-full flex items-center gap-2.5 px-2 py-2 rounded-lg text-sm transition-all duration-300 ${
                                justCompleted
                                  ? 'bg-accent-light animate-zoom-bounce'
                                  : blocked
                                    ? 'text-text-muted/70 cursor-not-allowed'
                                  : isNext
                                    ? 'bg-accent-light/50 ring-1 ring-accent/30'
                                    : complete
                                      ? 'text-text-muted'
                                      : 'text-text-primary hover:bg-accent-light cursor-pointer'
                              }`}
                            >
                              <span
                                className={`relative w-5 h-5 rounded-full border-2 flex items-center justify-center shrink-0 text-xs transition-all duration-500 ${
                                  justCompleted
                                    ? 'border-accent bg-accent text-white animate-glitter'
                                    : complete
                                      ? 'border-accent bg-accent text-white'
                                      : blocked
                                        ? 'border-divider/50'
                                        : isNext
                                          ? 'border-accent animate-gentle-bounce'
                                          : 'border-divider'
                                }`}
                              >
                                {complete && '✓'}
                                {justCompleted && (
                                  <>
                                    <span className="absolute -top-1 -right-1 w-1.5 h-1.5 rounded-full bg-accent" style={{ animation: 'sparkle 0.8s ease-out 0.1s both' }} />
                                    <span className="absolute -top-0.5 -left-1.5 w-1 h-1 rounded-full bg-accent/70" style={{ animation: 'sparkle 0.8s ease-out 0.25s both' }} />
                                    <span className="absolute -bottom-1 -right-0.5 w-1 h-1 rounded-full bg-accent/60" style={{ animation: 'sparkle 0.8s ease-out 0.4s both' }} />
                                    <span className="absolute -bottom-0.5 -left-1 w-1.5 h-1.5 rounded-full bg-accent/50" style={{ animation: 'sparkle 0.8s ease-out 0.55s both' }} />
                                  </>
                                )}
                              </span>
                              <span className={`transition-all duration-300 ${
                                justCompleted
                                  ? 'font-medium text-accent'
                                  : complete
                                    ? 'line-through'
                                    : isNext
                                      ? 'font-medium text-accent'
                                      : ''
                              }`}>
                                {t(`onboarding.steps.${step.id}`)}
                              </span>
                              {justCompleted && (
                                <span className="ml-auto text-accent text-sm font-bold animate-zoom-bounce">✓</span>
                              )}
                              {isNext && !complete && (
                                <span className="ml-auto text-accent text-xs animate-gentle-bounce">→</span>
                              )}
                            </button>
                          )
                        })}
                      </div>
                    </div>
                  </div>
                </div>
              )
            })
          )}
        </div>

        {!allComplete && (
          <div className="px-4 py-3 border-t border-divider/50">
            <button
              onClick={dismissPermanently}
              className="w-full text-xs text-text-muted hover:text-text-secondary transition-colors text-center py-1"
            >
              {t('onboarding.drawer.skipAll')}
            </button>
          </div>
        )}
      </div>
    </>
  )
}
