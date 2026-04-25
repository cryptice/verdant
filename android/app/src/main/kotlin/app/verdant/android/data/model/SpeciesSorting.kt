package app.verdant.android.data.model

import java.text.Collator
import java.util.Locale

/**
 * Order species by Swedish common name (falling back to English when missing),
 * then by Swedish variant (falling back to English). Uses a Swedish-locale
 * collator so å/ä/ö sort after z.
 */
private val swedishCollator: Collator = Collator.getInstance(Locale("sv", "SE")).apply {
    strength = Collator.PRIMARY
}

private fun displayCommon(s: SpeciesResponse): String =
    s.commonNameSv ?: s.commonName

private fun displayVariant(s: SpeciesResponse): String =
    s.variantNameSv ?: s.variantName ?: ""

fun List<SpeciesResponse>.sortedBySwedishName(): List<SpeciesResponse> {
    val cmp = Comparator<SpeciesResponse> { a, b ->
        val byCommon = swedishCollator.compare(displayCommon(a), displayCommon(b))
        if (byCommon != 0) byCommon
        else swedishCollator.compare(displayVariant(a), displayVariant(b))
    }
    return sortedWith(cmp)
}
