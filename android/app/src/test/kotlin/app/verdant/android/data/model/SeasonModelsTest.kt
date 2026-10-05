package app.verdant.android.data.model

import org.junit.Assert.*
import org.junit.Test

class SeasonModelsTest {
    private fun season(id: Long, year: Int, active: Boolean = false) =
        SeasonResponse(id, "Season $id", year, null, null, null, null, null, null, active, "", "")

    @Test fun `latest season uses year rather than id active flag or list order`() {
        val latest = season(2, 2028)
        val seasons = listOf(season(99, 2026, true), latest, season(7, 2027))
        assertEquals(latest, seasons.latestByYear())
        assertEquals(latest, seasons.reversed().latestByYear())
    }

    @Test fun `empty list has no default and single season is selected`() {
        assertNull(emptyList<SeasonResponse>().latestByYear())
        val only = season(5, 2025)
        assertEquals(only, listOf(only).latestByYear())
    }

    @Test fun `same-year ties retain the first season in list order`() {
        val first = season(2, 2028)
        assertEquals(first, listOf(first, season(8, 2028)).latestByYear())
    }
}
