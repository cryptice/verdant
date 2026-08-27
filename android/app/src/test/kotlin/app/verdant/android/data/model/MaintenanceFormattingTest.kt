package app.verdant.android.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MaintenanceFormattingTest {

    private fun rule(
        activityType: String = "WEED",
        intervalDays: Int = 21,
        active: Boolean = true,
        nextDueDate: String = "2026-06-25",
        seasonStartMonth: Int? = null,
        seasonStartDay: Int? = null,
        seasonEndMonth: Int? = null,
        seasonEndDay: Int? = null,
    ) = MaintenanceRuleResponse(
        id = 1, bedId = null, bedName = null,
        gardenAreaId = 5, gardenAreaName = "Gången",
        activityType = activityType, intervalDays = intervalDays, anchorDate = null,
        seasonStartMonth = seasonStartMonth, seasonStartDay = seasonStartDay,
        seasonEndMonth = seasonEndMonth, seasonEndDay = seasonEndDay,
        active = active, notes = null,
        lastDoneDate = null, nextDueDate = nextDueDate,
        createdAt = "2026-01-01T00:00:00Z", updatedAt = "2026-01-01T00:00:00Z",
    )

    @Test
    fun `beds accept exactly the three activities the backend allows`() {
        assertEquals(
            listOf("WATER", "WEED", "FERTILIZE"),
            activitiesForTarget(MaintenanceTarget.BED),
        )
    }

    @Test
    fun `areas exclude FERTILIZE and include the area-only work`() {
        val area = activitiesForTarget(MaintenanceTarget.AREA)
        assertFalse(area.contains("FERTILIZE"))
        assertTrue(area.contains("MOW"))
        assertTrue(area.contains("WEED"))
    }

    @Test
    fun `the two targets together cover every activity`() {
        val union = (activitiesForTarget(MaintenanceTarget.BED) +
            activitiesForTarget(MaintenanceTarget.AREA)).toSet()
        assertEquals(MAINTENANCE_ACTIVITIES.toSet(), union)
    }

    @Test
    fun `AREA_CATEGORIES matches the backend enum`() {
        assertEquals(
            listOf(
                "WALKWAY", "LAWN", "HEDGE", "COMPOST",
                "GREENHOUSE", "WATER_FEATURE", "STRUCTURE", "OTHER",
            ),
            AREA_CATEGORIES,
        )
    }

    @Test
    fun `an overdue rule reports how many days late`() {
        val state = dueState(rule(nextDueDate = "2026-06-20"), "2026-06-25")
        assertTrue(state is DueState.Overdue)
        assertEquals(5, (state as DueState.Overdue).days)
    }

    @Test
    fun `a rule due today is Due, not Overdue`() {
        assertTrue(dueState(rule(nextDueDate = "2026-06-25"), "2026-06-25") is DueState.Due)
    }

    @Test
    fun `an upcoming rule reports days remaining`() {
        val state = dueState(rule(nextDueDate = "2026-07-02"), "2026-06-25")
        assertEquals(7, (state as DueState.Upcoming).days)
    }

    @Test
    fun `a paused rule is Inactive however overdue its date looks`() {
        val state = dueState(rule(active = false, nextDueDate = "2020-01-01"), "2026-06-25")
        assertTrue(state is DueState.Inactive)
    }

    @Test
    fun `a season window needs all four bounds`() {
        assertFalse(hasSeasonWindow(rule()))
        assertTrue(hasSeasonWindow(rule(
            seasonStartMonth = 4, seasonStartDay = 1, seasonEndMonth = 10, seasonEndDay = 15,
        )))
        // Three of four is not a window — the server rejects it, so neither do we.
        assertFalse(hasSeasonWindow(rule(
            seasonStartMonth = 4, seasonStartDay = 1, seasonEndMonth = 10,
        )))
    }

    @Test
    fun `seasonWindowMonthDays returns the pair in stored order, wrap-around included`() {
        assertNull(seasonWindowMonthDays(rule()))

        val summer = seasonWindowMonthDays(rule(
            seasonStartMonth = 4, seasonStartDay = 1, seasonEndMonth = 10, seasonEndDay = 15,
        ))!!
        assertEquals(4 to 1, summer.first)
        assertEquals(10 to 15, summer.second)

        // Nov 1 – Mar 31 is one continuous winter season, not an error.
        val winter = seasonWindowMonthDays(rule(
            seasonStartMonth = 11, seasonStartDay = 1, seasonEndMonth = 3, seasonEndDay = 31,
        ))!!
        assertEquals(11 to 1, winter.first)
        assertEquals(3 to 31, winter.second)
    }
}
