import type { PageTooltipConfig } from './types'

const configs: PageTooltipConfig[] = [
  {
    stepId: 'create_garden',
    tooltips: [
      { targetSelector: '[data-onboarding="garden-form"]', titleKey: 'onboarding.tooltips.createGarden.title', descriptionKey: 'onboarding.tooltips.createGarden.description' },
    ],
  },
  {
    stepId: 'create_bed',
    tooltips: [
      { targetSelector: '[data-onboarding="garden-card"]', titleKey: 'onboarding.tooltips.createBed.step1.title', descriptionKey: 'onboarding.tooltips.createBed.step1.description' },
      { targetSelector: '[data-onboarding="add-bed-btn"]', titleKey: 'onboarding.tooltips.createBed.step2.title', descriptionKey: 'onboarding.tooltips.createBed.step2.description' },
    ],
  },
  {
    stepId: 'browse_species',
    tooltips: [
      { targetSelector: '[data-onboarding="species-list"]', titleKey: 'onboarding.tooltips.browseSpecies.title', descriptionKey: 'onboarding.tooltips.browseSpecies.description' },
    ],
  },
  {
    stepId: 'add_seeds',
    tooltips: [
      { targetSelector: '[data-onboarding="add-seed-btn"]', titleKey: 'onboarding.tooltips.addSeeds.step1.title', descriptionKey: 'onboarding.tooltips.addSeeds.step1.description' },
      { targetSelector: '[data-onboarding="seed-form"]', titleKey: 'onboarding.tooltips.addSeeds.step2.title', descriptionKey: 'onboarding.tooltips.addSeeds.step2.description' },
    ],
  },
  {
    stepId: 'sow_seeds',
    tooltips: [
      { targetSelector: '[data-onboarding="sow-species"]', titleKey: 'onboarding.tooltips.sowSeeds.step1.title', descriptionKey: 'onboarding.tooltips.sowSeeds.step1.description' },
      { targetSelector: '[data-onboarding="sow-location"]', titleKey: 'onboarding.tooltips.sowSeeds.step2.title', descriptionKey: 'onboarding.tooltips.sowSeeds.step2.description' },
      { targetSelector: '[data-onboarding="sow-submit"]', titleKey: 'onboarding.tooltips.sowSeeds.step3.title', descriptionKey: 'onboarding.tooltips.sowSeeds.step3.description' },
    ],
  },
  {
    stepId: 'pot_up',
    tooltips: [
      { targetSelector: '[data-onboarding="plant-actions"]', titleKey: 'onboarding.tooltips.potUp.title', descriptionKey: 'onboarding.tooltips.potUp.description' },
    ],
  },
  {
    stepId: 'plant_out',
    tooltips: [
      { targetSelector: '[data-onboarding="plant-actions"]', titleKey: 'onboarding.tooltips.plantOut.title', descriptionKey: 'onboarding.tooltips.plantOut.description' },
    ],
  },
  {
    stepId: 'record_harvest',
    tooltips: [
      { targetSelector: '[data-onboarding="plant-actions"]', titleKey: 'onboarding.tooltips.recordHarvest.title', descriptionKey: 'onboarding.tooltips.recordHarvest.description' },
    ],
  },
  {
    stepId: 'add_customer',
    tooltips: [
      { targetSelector: '[data-onboarding="add-customer-btn"]', titleKey: 'onboarding.tooltips.addCustomer.title', descriptionKey: 'onboarding.tooltips.addCustomer.description' },
    ],
  },
  {
    stepId: 'create_bouquet',
    tooltips: [
      { targetSelector: '[data-onboarding="add-bouquet-btn"]', titleKey: 'onboarding.tooltips.createBouquet.title', descriptionKey: 'onboarding.tooltips.createBouquet.description' },
    ],
  },
  {
    stepId: 'create_task',
    tooltips: [
      { targetSelector: '[data-onboarding="task-form"]', titleKey: 'onboarding.tooltips.createTask.title', descriptionKey: 'onboarding.tooltips.createTask.description' },
    ],
  },
  {
    stepId: 'setup_succession',
    tooltips: [
      { targetSelector: '[data-onboarding="add-succession-btn"]', titleKey: 'onboarding.tooltips.setupSuccession.title', descriptionKey: 'onboarding.tooltips.setupSuccession.description' },
    ],
  },
  {
    stepId: 'set_target',
    tooltips: [
      { targetSelector: '[data-onboarding="add-target-btn"]', titleKey: 'onboarding.tooltips.setTarget.title', descriptionKey: 'onboarding.tooltips.setTarget.description' },
    ],
  },
  {
    stepId: 'view_calendar',
    tooltips: [
      { targetSelector: '[data-onboarding="calendar-view"]', titleKey: 'onboarding.tooltips.viewCalendar.title', descriptionKey: 'onboarding.tooltips.viewCalendar.description' },
    ],
  },
  {
    stepId: 'start_trial',
    tooltips: [
      { targetSelector: '[data-onboarding="add-trial-btn"]', titleKey: 'onboarding.tooltips.startTrial.title', descriptionKey: 'onboarding.tooltips.startTrial.description' },
    ],
  },
  {
    stepId: 'log_pest',
    tooltips: [
      { targetSelector: '[data-onboarding="add-pest-btn"]', titleKey: 'onboarding.tooltips.logPest.title', descriptionKey: 'onboarding.tooltips.logPest.description' },
    ],
  },
  {
    stepId: 'view_analytics',
    tooltips: [
      { targetSelector: '[data-onboarding="analytics-view"]', titleKey: 'onboarding.tooltips.viewAnalytics.title', descriptionKey: 'onboarding.tooltips.viewAnalytics.description' },
    ],
  },
  {
    stepId: 'create_listing',
    tooltips: [
      { targetSelector: '[data-onboarding="add-listing-btn"]', titleKey: 'onboarding.tooltips.createListing.title', descriptionKey: 'onboarding.tooltips.createListing.description' },
    ],
  },
]

export function getTooltipConfig(stepId: string): PageTooltipConfig | null {
  return configs.find(c => c.stepId === stepId) ?? null
}
