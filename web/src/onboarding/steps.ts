import type { OnboardingStep, OnboardingSection } from './types'

export const ONBOARDING_STEPS: OnboardingStep[] = [
  // Section 1: Getting Started
  { id: 'create_org', section: 'getting_started', route: '/org/settings', completionType: 'auto' },
  { id: 'create_season', section: 'getting_started', route: '/seasons', completionType: 'explicit' },
  { id: 'create_garden', section: 'getting_started', route: '/gardens', completionType: 'explicit' },
  { id: 'create_bed', section: 'getting_started', route: '/gardens', requires: 'create_garden', completionType: 'explicit' },

  // Section 2: Growing
  { id: 'browse_species', section: 'growing', route: '/species', completionType: 'visit' },
  { id: 'add_seeds', section: 'growing', route: '/seed-stock', completionType: 'explicit' },
  { id: 'sow_seeds', section: 'growing', route: '/sow', completionType: 'explicit' },

  // Section 3: Planning
  { id: 'create_task', section: 'planning', route: '/task/new', completionType: 'explicit' },
  { id: 'setup_succession', section: 'planning', route: '/successions', completionType: 'explicit' },
  { id: 'set_target', section: 'planning', route: '/targets', completionType: 'explicit' },
  { id: 'view_calendar', section: 'planning', route: '/calendar', completionType: 'visit' },

  // Section 4: Plant
  { id: 'pot_up', section: 'planting', route: '/plants', completionType: 'explicit' },
  { id: 'plant_out', section: 'planting', route: '/plants', completionType: 'explicit' },

  // Section 5: Harvest
  { id: 'record_harvest', section: 'harvest', route: '/plants', completionType: 'explicit' },

  // Section 6: Sell
  { id: 'add_customer', section: 'sell', route: '/customers', completionType: 'explicit' },
  { id: 'create_bouquet', section: 'sell', route: '/bouquets', completionType: 'explicit' },

  // Section 5: Advanced
  { id: 'start_trial', section: 'advanced', route: '/trials', completionType: 'explicit' },
  { id: 'log_pest', section: 'advanced', route: '/pest-disease', completionType: 'explicit' },
  { id: 'view_analytics', section: 'advanced', route: '/analytics', completionType: 'visit' },
]

export const SECTIONS: { id: OnboardingSection; titleKey: string; icon: string }[] = [
  { id: 'getting_started', titleKey: 'onboarding.sections.gettingStarted', icon: '🚀' },
  { id: 'growing', titleKey: 'onboarding.sections.growing', icon: '🌱' },
  { id: 'planning', titleKey: 'onboarding.sections.planning', icon: '📋' },
  { id: 'planting', titleKey: 'onboarding.sections.planting', icon: '🌿' },
  { id: 'harvest', titleKey: 'onboarding.sections.harvest', icon: '🌾' },
  { id: 'sell', titleKey: 'onboarding.sections.sell', icon: '💐' },
  { id: 'advanced', titleKey: 'onboarding.sections.advanced', icon: '⚡' },
]

export function getStepsForSection(section: OnboardingSection): OnboardingStep[] {
  return ONBOARDING_STEPS.filter(s => s.section === section)
}
