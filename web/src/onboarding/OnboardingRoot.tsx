import { OnboardingFab } from './OnboardingFab'
import { OnboardingDrawer } from './OnboardingDrawer'
import { OnboardingOverlay } from './OnboardingOverlay'
import { OnboardingTooltip } from './OnboardingTooltip'
import { useOnboarding } from './OnboardingContext'
import { useTooltipTour } from './useTooltipTour'
import { ONBOARDING_STEPS } from './steps'

export function OnboardingRoot() {
  const { activeTour, clearActiveTour, completeStep, isActive, drawerOpen } = useOnboarding()

  const tour = useTooltipTour(activeTour, () => {
    if (activeTour) {
      // Only auto-complete visit-type steps via the tour.
      // Mutation steps complete when the user actually performs the action.
      const step = ONBOARDING_STEPS.find(s => s.id === activeTour.stepId)
      if (step?.completionType === 'visit') {
        completeStep(activeTour.stepId)
      }
    }
    clearActiveTour()
  })

  return (
    <>
      {isActive && <OnboardingFab />}
      {drawerOpen && <OnboardingDrawer />}
      {activeTour && tour.targetElement && (
        <>
          <OnboardingOverlay targetElement={tour.targetElement} onClick={clearActiveTour} />
          <OnboardingTooltip
            targetElement={tour.targetElement}
            titleKey={tour.currentTooltip!.titleKey}
            descriptionKey={tour.currentTooltip!.descriptionKey}
            currentIndex={tour.currentIndex}
            totalSteps={tour.totalSteps}
            onNext={tour.next}
            onBack={tour.back}
            onSkip={clearActiveTour}
          />
        </>
      )}
    </>
  )
}
