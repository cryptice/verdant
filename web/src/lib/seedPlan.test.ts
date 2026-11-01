import { describe, expect, it } from 'vitest'
import { workflowProblem } from './seedPlan'

const step = (name: string, sortOrder: number, daysAfterPrevious?: number, isSideBranch = false) => ({ name, sortOrder, daysAfterPrevious, isSideBranch })

describe('workflowProblem', () => {
  it('finds no problem when every step after the first has days', () => {
    expect(workflowProblem([step('Säljklar', 2, 10), step('Sådd', 0), step('Omskolning', 1, 20)])).toBeNull()
  })

  it('reports a variety without a workflow, side branches not counting', () => {
    expect(workflowProblem([])).toEqual({ kind: 'none' })
    expect(workflowProblem([step('Sticklingar', 0, 5, true)])).toEqual({ kind: 'none' })
  })

  it('names the first main step missing its days', () => {
    expect(workflowProblem([step('Sådd', 0), step('Omskolning', 1), step('Toppning', 2), step('Gren', 3, undefined, true)]))
      .toEqual({ kind: 'missingDays', step: 'Omskolning' })
  })
})
