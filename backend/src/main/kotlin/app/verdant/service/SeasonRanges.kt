package app.verdant.service

import app.verdant.entity.Season
import java.time.LocalDate

/**
 * Seasons may overlap; a date belongs to the overlapping season that starts latest
 * (lowest id on equal starts).
 */
object SeasonRanges {

    fun rangeOf(season: Season): ClosedRange<LocalDate> =
        (season.startDate ?: LocalDate.of(season.year, 1, 1))..(season.endDate ?: LocalDate.of(season.year, 12, 31))

    fun seasonFor(date: LocalDate, seasons: List<Season>): Season? =
        seasons.filter { date in rangeOf(it) }
            .minWithOrNull(compareByDescending<Season> { rangeOf(it).start }.thenBy { it.id })

    fun effectiveRanges(season: Season, seasons: List<Season>): List<ClosedRange<LocalDate>> {
        var remaining = listOf(rangeOf(season))
        seasons.filter { it.id != season.id && claimsBefore(it, season) }.forEach { other ->
            val cut = rangeOf(other)
            remaining = remaining.flatMap { r -> subtract(r, cut) }
        }
        return remaining.sortedBy { it.start }
    }

    private fun claimsBefore(other: Season, season: Season): Boolean {
        val otherStart = rangeOf(other).start
        val start = rangeOf(season).start
        return otherStart > start || (otherStart == start && (other.id ?: Long.MAX_VALUE) < (season.id ?: Long.MAX_VALUE))
    }

    private fun subtract(r: ClosedRange<LocalDate>, cut: ClosedRange<LocalDate>): List<ClosedRange<LocalDate>> {
        if (cut.endInclusive < r.start || cut.start > r.endInclusive) return listOf(r)
        return buildList {
            if (cut.start > r.start) add(r.start..cut.start.minusDays(1))
            if (cut.endInclusive < r.endInclusive) add(cut.endInclusive.plusDays(1)..r.endInclusive)
        }
    }
}
