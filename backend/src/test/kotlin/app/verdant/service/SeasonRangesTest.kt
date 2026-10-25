package app.verdant.service

import app.verdant.entity.Season
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SeasonRangesTest {

    private fun season(id: Long, year: Int, start: String? = null, end: String? = null) = Season(
        orgId = 1,
        name = "Season $id",
        year = year,
        startDate = start?.let(LocalDate::parse),
        endDate = end?.let(LocalDate::parse),
        id = id,
    )

    private val wholeYear = season(id = 1, year = 2026)
    private val autumn = season(id = 2, year = 2026, start = "2026-09-01", end = "2026-12-31")

    @Test
    fun `a season without dates covers its whole year`() {
        assertEquals(LocalDate.of(2026, 1, 1)..LocalDate.of(2026, 12, 31), SeasonRanges.rangeOf(wholeYear))
    }

    @Test
    fun `overlapping seasons give a date to the latest start`() {
        assertEquals(2L, SeasonRanges.seasonFor(LocalDate.of(2026, 10, 1), listOf(wholeYear, autumn))?.id)
        assertEquals(1L, SeasonRanges.seasonFor(LocalDate.of(2026, 5, 1), listOf(wholeYear, autumn))?.id)
    }

    @Test
    fun `equal starts go to the lowest id`() {
        val seven = season(id = 7, year = 2026)
        val three = season(id = 3, year = 2026)
        assertEquals(3L, SeasonRanges.seasonFor(LocalDate.of(2026, 6, 1), listOf(seven, three))?.id)
    }

    @Test
    fun `a date outside every season has none`() {
        assertNull(SeasonRanges.seasonFor(LocalDate.of(2027, 6, 1), listOf(wholeYear, autumn)))
    }

    @Test
    fun `effective ranges cut out what later seasons claim`() {
        assertEquals(
            listOf(LocalDate.of(2026, 1, 1)..LocalDate.of(2026, 8, 31)),
            SeasonRanges.effectiveRanges(wholeYear, listOf(wholeYear, autumn)),
        )
        assertEquals(
            listOf(LocalDate.of(2026, 9, 1)..LocalDate.of(2026, 12, 31)),
            SeasonRanges.effectiveRanges(autumn, listOf(wholeYear, autumn)),
        )
    }

    @Test
    fun `a season in the middle of another splits its effective ranges`() {
        val summer = season(id = 2, year = 2026, start = "2026-06-01", end = "2026-06-30")
        assertEquals(
            listOf(
                LocalDate.of(2026, 1, 1)..LocalDate.of(2026, 5, 31),
                LocalDate.of(2026, 7, 1)..LocalDate.of(2026, 12, 31),
            ),
            SeasonRanges.effectiveRanges(wholeYear, listOf(wholeYear, summer)),
        )
    }

    @Test
    fun `a season fully claimed by a later one has no effective range`() {
        val inner = season(id = 9, year = 2026, start = "2026-10-01", end = "2026-10-31")
        val october = season(id = 2, year = 2026, start = "2026-10-01", end = "2026-12-31")
        assertEquals(emptyList<ClosedRange<LocalDate>>(), SeasonRanges.effectiveRanges(inner, listOf(inner, october)))
    }
}
