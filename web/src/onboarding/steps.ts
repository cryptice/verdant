import type { OnboardingStep, OnboardingSection } from './types'

export const ONBOARDING_STEPS: OnboardingStep[] = [
  // Section 1: Getting Started
  { id: 'create_season', section: 'getting_started', route: '/seasons', completionType: 'mutation', queryKey: ['seasons'], mutationQueryKeys: [['seasons']] },
  { id: 'create_garden', section: 'getting_started', route: '/', completionType: 'mutation', mutationQueryKeys: [['dashboard']] },
  { id: 'create_bed', section: 'getting_started', route: '/', extraRoutePrefixes: ['/garden/'], completionType: 'mutation', mutationQueryKeys: [['garden-beds']] },

  // Section 2: Growing
  { id: 'browse_species', section: 'growing', route: '/species', completionType: 'visit' },
  { id: 'add_seeds', section: 'growing', route: '/seeds', completionType: 'mutation', queryKey: ['seed-inventory'], mutationQueryKeys: [['seed-inventory']] },
  { id: 'sow_seeds', section: 'growing', route: '/sow', completionType: 'mutation', mutationQueryKeys: [['plants'], ['tray-summary']] },
  { id: 'pot_up', section: 'growing', route: '/plants', completionType: 'mutation', mutationQueryKeys: [['plants']] },
  { id: 'plant_out', section: 'growing', route: '/plants', completionType: 'mutation', mutationQueryKeys: [['plants']] },

  // Section 3: Harvesting & Sales
  { id: 'record_harvest', section: 'harvesting_sales', route: '/plants', completionType: 'mutation', mutationQueryKeys: [['harvest-stats']] },
  { id: 'add_customer', section: 'harvesting_sales', route: '/customers', completionType: 'mutation', queryKey: ['customers'], mutationQueryKeys: [['customers']] },
  { id: 'create_bouquet', section: 'harvesting_sales', route: '/bouquets', completionType: 'mutation', queryKey: ['bouquet-recipes'], mutationQueryKeys: [['bouquet-recipes']] },

  // Section 4: Planning
  { id: 'create_task', section: 'planning', route: '/task/new', completionType: 'mutation', queryKey: ['tasks'], mutationQueryKeys: [['tasks']] },
  { id: 'setup_succession', section: 'planning', route: '/successions', completionType: 'mutation', queryKey: ['succession-schedules'], mutationQueryKeys: [['succession-schedules']] },
  { id: 'set_target', section: 'planning', route: '/targets', completionType: 'mutation', queryKey: ['production-targets'], mutationQueryKeys: [['production-targets']] },
  { id: 'view_calendar', section: 'planning', route: '/calendar', completionType: 'visit' },

  // Section 5: Advanced
  { id: 'start_trial', section: 'advanced', route: '/trials', completionType: 'mutation', queryKey: ['variety-trials'], mutationQueryKeys: [['variety-trials']] },
  { id: 'log_pest', section: 'advanced', route: '/pest-disease', completionType: 'mutation', queryKey: ['pest-disease-logs'], mutationQueryKeys: [['pest-disease-logs']] },
  { id: 'view_analytics', section: 'advanced', route: '/analytics', completionType: 'visit' },
  { id: 'create_listing', section: 'advanced', route: '/market/listings', completionType: 'mutation', queryKey: ['my-listings'], mutationQueryKeys: [['my-listings']] },
]

export const SECTIONS: { id: OnboardingSection; titleKey: string; icon: string }[] = [
  { id: 'getting_started', titleKey: 'onboarding.sections.gettingStarted', icon: '🚀' },
  { id: 'growing', titleKey: 'onboarding.sections.growing', icon: '🌱' },
  { id: 'harvesting_sales', titleKey: 'onboarding.sections.harvestingSales', icon: '🌾' },
  { id: 'planning', titleKey: 'onboarding.sections.planning', icon: '📋' },
  { id: 'advanced', titleKey: 'onboarding.sections.advanced', icon: '⚡' },
]

export function getStepsForSection(section: OnboardingSection): OnboardingStep[] {
  return ONBOARDING_STEPS.filter(s => s.section === section)
}
