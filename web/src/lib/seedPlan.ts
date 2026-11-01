import type { SpeciesWorkflowStepResponse } from '../api/client'

/** Why a variety's workflow can't be planned from, mirroring the server's checks. */
export type WorkflowProblem = { kind: 'catalogue' } | { kind: 'none' } | { kind: 'missingDays'; step: string }

/**
 * Planning counts back through the workflow's main line, so it needs at least one step and a number
 * of days on every step after the first. Side branches don't take part.
 */
export function workflowProblem(steps: Pick<SpeciesWorkflowStepResponse, 'name' | 'daysAfterPrevious' | 'isSideBranch' | 'sortOrder'>[]): WorkflowProblem | null {
  const main = steps.filter(s => !s.isSideBranch).sort((a, b) => a.sortOrder - b.sortOrder)
  if (main.length === 0) return { kind: 'none' }
  const gap = main.slice(1).find(s => s.daysAfterPrevious == null)
  return gap ? { kind: 'missingDays', step: gap.name } : null
}
