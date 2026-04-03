import { OnboardingFab } from './OnboardingFab'
import { OnboardingDrawer } from './OnboardingDrawer'
import { OnboardingOverlay } from './OnboardingOverlay'
import { OnboardingTooltip } from './OnboardingTooltip'
import { useOnboarding } from './OnboardingContext'
import { useTooltipTour } from './useTooltipTour'

export function OnboardingRoot() {
  const { activeTour, clearActiveTour, completeStep, isActive, drawerOpen } = useOnboarding()

  const tour = useTooltipTour(activeTour, () => {
    if (activeTour) {
      completeStep(activeTour.stepId)
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
