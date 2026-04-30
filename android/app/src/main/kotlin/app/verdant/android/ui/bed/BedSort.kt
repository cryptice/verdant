package app.verdant.android.ui.bed

/**
 * Natural string comparison: digit runs are compared as integers, so
 * "Bed #2" sorts before "Bed #10". Falls back to case-insensitive
 * lexicographic comparison for non-digit chunks.
 */
internal val NaturalNameComparator: Comparator<String> = Comparator { a, b ->
    val ac = NATURAL_CHUNKS.findAll(a).map { it.value }.toList()
    val bc = NATURAL_CHUNKS.findAll(b).map { it.value }.toList()
    val n = minOf(ac.size, bc.size)
    for (i in 0 until n) {
        val ax = ac[i]
        val bx = bc[i]
        val cmp = if (ax.first().isDigit() && bx.first().isDigit()) {
            (ax.toLongOrNull() ?: 0L).compareTo(bx.toLongOrNull() ?: 0L)
        } else {
            ax.compareTo(bx, ignoreCase = true)
        }
        if (cmp != 0) return@Comparator cmp
    }
    ac.size.compareTo(bc.size)
}

private val NATURAL_CHUNKS = Regex("(\\d+|\\D+)")

/** Sort beds by name with natural numeric ordering (Bed #10 after Bed #9). */
@JvmName("sortBedsByNaturalName")
internal fun List<app.verdant.android.data.model.BedResponse>.sortedByNaturalName():
    List<app.verdant.android.data.model.BedResponse> =
    sortedWith(compareBy(NaturalNameComparator) { it.name })

/**
 * Sort cross-garden beds: primary by garden name, secondary by bed name
 * (natural). Keeps the existing per-garden grouping that the backend
 * applied via ORDER BY g.name, b.name.
 */
@JvmName("sortBedsWithGardenByNaturalName")
internal fun List<app.verdant.android.data.model.BedWithGardenResponse>.sortedByNaturalName():
    List<app.verdant.android.data.model.BedWithGardenResponse> =
    sortedWith(Comparator { a, b ->
        val g = NaturalNameComparator.compare(a.gardenName ?: "", b.gardenName ?: "")
        if (g != 0) g else NaturalNameComparator.compare(a.name, b.name)
    })
