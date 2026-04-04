export interface OnboardingStep {
  id: string
  section: OnboardingSection
  route: string
  /** Step ID that must be completed before this step is clickable */
  requires?: string
  completionType: 'explicit' | 'visit' | 'auto'
}

export type OnboardingSection =
  | 'getting_started'
  | 'growing'
  | 'planning'
  | 'planting'
  | 'harvest'
  | 'sell'
  | 'advanced'

export interface OnboardingState {
  completedSteps: string[]
  dismissed: boolean
}

export interface TooltipStep {
  targetSelector: string
  titleKey: string
  descriptionKey: string
}

export interface PageTooltipConfig {
  stepId: string
  tooltips: TooltipStep[]
}
