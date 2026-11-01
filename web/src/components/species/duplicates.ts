import type { SpeciesResponse } from '../../api/client'

export interface VarietyNames {
  commonName: string
  commonNameSv: string
  variantName: string
  variantNameSv: string
}

const norm = (s?: string | null) => (s ?? '').trim().toLocaleLowerCase('sv')

/** The text to search the server with: the variety name narrows best, so "Lilliput" also finds "Pompondahlia – Lilliput". */
export function duplicateSearchText(names: VarietyNames): string {
  return (names.variantNameSv || names.variantName || names.commonNameSv || names.commonName).trim()
}

/**
 * Varieties that look like the one being entered. With a variety name, it must match exactly and the
 * species names must overlap ("Dahlia" in "Pompondahlia"); without one, the species name must match a
 * variety that has no variety name either. A copy's "(kopia)" name never matches.
 */
export function findDuplicateVarieties(names: VarietyNames, candidates: SpeciesResponse[]): SpeciesResponse[] {
  const commons = [names.commonNameSv, names.commonName].map(norm).filter(Boolean)
  const variants = [names.variantNameSv, names.variantName].map(norm).filter(v => v && !v.endsWith('(kopia)'))
  if (commons.length === 0) return []
  return candidates.filter(c => {
    const cCommons = [c.commonNameSv, c.commonName].map(norm).filter(Boolean)
    const cVariants = [c.variantNameSv, c.variantName].map(norm).filter(Boolean)
    if (variants.length === 0) return cVariants.length === 0 && cCommons.some(n => commons.includes(n))
    return cVariants.some(v => variants.includes(v)) && cCommons.some(n => commons.some(e => n.includes(e) || e.includes(n)))
  })
}
