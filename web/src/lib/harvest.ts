/**
 * Year-over-year harvest delta as a whole-number percent, or `null` when there
 * is no prior-year baseline to compare against (the dashboard hides the delta
 * line in that case rather than dividing by zero).
 */
export function harvestDeltaPct(totalStems: number, prevYearStems: number): number | null {
  if (prevYearStems <= 0) return null
  return Math.round(((totalStems - prevYearStems) / prevYearStems) * 100)
}
