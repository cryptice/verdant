package app.verdant.android.data.model

import java.util.Locale

/** The names typed for a new variety; blank ones are ignored. */
data class VarietyNames(val commonName: String, val variantName: String = "", val variantNameSv: String = "")

private val swedish = Locale("sv", "SE")
private fun norm(s: String?): String = s.orEmpty().trim().lowercase(swedish)

/** The text to search the server with: the variety name narrows best, so "Lilliput" also finds "Pompondahlia – Lilliput". */
fun duplicateSearchText(names: VarietyNames): String =
    names.variantNameSv.ifBlank { names.variantName }.ifBlank { names.commonName }.trim()

/**
 * Varieties that look like the one being entered, matched the same way as on the web. With a variety
 * name, it must match exactly and the species names must overlap ("Dahlia" in "Pompondahlia"); without
 * one, the species name must match a variety that has no variety name either.
 */
fun findDuplicateVarieties(names: VarietyNames, candidates: List<SpeciesResponse>): List<SpeciesResponse> {
    val common = norm(names.commonName).ifEmpty { return emptyList() }
    val variants = listOf(names.variantNameSv, names.variantName).map(::norm).filter { it.isNotEmpty() && !it.endsWith("(kopia)") }
    return candidates.filter { c ->
        val cCommons = listOf(c.commonNameSv, c.commonName).map(::norm).filter { it.isNotEmpty() }
        val cVariants = listOf(c.variantNameSv, c.variantName).map(::norm).filter { it.isNotEmpty() }
        if (variants.isEmpty()) cVariants.isEmpty() && common in cCommons
        else cVariants.any { it in variants } && cCommons.any { it.contains(common) || common.contains(it) }
    }
}
