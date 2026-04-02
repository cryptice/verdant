export interface OnboardingStep {
  id: string
  section: OnboardingSection
  route: string
  completionType: 'mutation' | 'visit' | 'query'
  queryKey?: string[]
  mutationQueryKeys?: string[][]
}

export type OnboardingSection =
  | 'getting_started'
  | 'growing'
  | 'harvesting_sales'
  | 'planning'
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
