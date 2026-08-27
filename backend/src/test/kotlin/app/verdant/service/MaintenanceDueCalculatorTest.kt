package app.verdant.service

import app.verdant.entity.MaintenanceActivity
import app.verdant.entity.MaintenanceRule
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.MonthDay

class MaintenanceDueCalculatorTest {

    private val summer = SeasonWindow(MonthDay.of(4, 1), MonthDay.of(10, 15))
    private val winter = SeasonWindow(MonthDay.of(11, 1), MonthDay.of(3, 31))

    // --- SeasonWindow.contains -------------------------------------------

    @Test
    fun `summer window contains a midsummer date`() {
        assertTrue(summer.contains(LocalDate.of(2026, 6, 15)))
    }

    @Test
    fun `summer window excludes a winter date`() {
        assertFalse(summer.contains(LocalDate.of(2026, 1, 15)))
    }

    @Test
    fun `window bounds are inclusive at both ends`() {
        assertTrue(summer.contains(LocalDate.of(2026, 4, 1)))
        assertTrue(summer.contains(LocalDate.of(2026, 10, 15)))
    }

    @Test
    fun `wrap-around window contains dates on both sides of new year`() {
        assertTrue(winter.contains(LocalDate.of(2026, 12, 20)))
        assertTrue(winter.contains(LocalDate.of(2026, 2, 10)))
        assertFalse(winter.contains(LocalDate.of(2026, 6, 1)))
    }

    // --- SeasonWindow.nextOpening ----------------------------------------

    @Test
    fun `nextOpening returns the date itself when already inside the window`() {
        assertEquals(LocalDate.of(2026, 6, 15), summer.nextOpening(LocalDate.of(2026, 6, 15)))
    }

    @Test
    fun `nextOpening jumps to next spring for a date after the window closes`() {
        assertEquals(LocalDate.of(2027, 4, 1), summer.nextOpening(LocalDate.of(2026, 10, 31)))
    }

    @Test
    fun `nextOpening jumps forward within the same year for a date before it opens`() {
        assertEquals(LocalDate.of(2026, 4, 1), summer.nextOpening(LocalDate.of(2026, 2, 1)))
    }

    @Test
    fun `nextOpening on a wrap-around window returns the autumn opening`() {
        assertEquals(LocalDate.of(2026, 11, 1), winter.nextOpening(LocalDate.of(2026, 6, 15)))
    }

    // --- SeasonWindow.of --------------------------------------------------

    @Test
    fun `of returns null when no bounds are set`() {
        assertNull(SeasonWindow.of(null, null, null, null))
    }

    @Test
    fun `of builds a window from four bounds`() {
        assertEquals(summer, SeasonWindow.of(4, 1, 10, 15))
    }

    // --- dueDate ----------------------------------------------------------

    @Test
    fun `a rule never done is due today`() {
        assertEquals(
            LocalDate.of(2026, 6, 1),
            MaintenanceDueCalculator.dueDate(null, 21, null, LocalDate.of(2026, 6, 1)),
        )
    }

    @Test
    fun `due is last done plus the interval`() {
        assertEquals(
            LocalDate.of(2026, 6, 22),
            MaintenanceDueCalculator.dueDate(LocalDate.of(2026, 6, 1), 21, null, LocalDate.of(2026, 6, 10)),
        )
    }

    @Test
    fun `a due date inside the window is left alone`() {
        assertEquals(
            LocalDate.of(2026, 6, 22),
            MaintenanceDueCalculator.dueDate(LocalDate.of(2026, 6, 1), 21, summer, LocalDate.of(2026, 6, 10)),
        )
    }

    @Test
    fun `weeding done in october is next due when the window reopens in april`() {
        assertEquals(
            LocalDate.of(2027, 4, 1),
            MaintenanceDueCalculator.dueDate(LocalDate.of(2026, 10, 10), 21, summer, LocalDate.of(2026, 10, 20)),
        )
    }

    @Test
    fun `a never-done rule out of season waits for the window to open`() {
        assertEquals(
            LocalDate.of(2026, 4, 1),
            MaintenanceDueCalculator.dueDate(null, 21, summer, LocalDate.of(2026, 1, 15)),
        )
    }

    @Test
    fun `a never-done wrap-around rule in summer waits until november`() {
        assertEquals(
            LocalDate.of(2026, 11, 1),
            MaintenanceDueCalculator.dueDate(null, 30, winter, LocalDate.of(2026, 6, 15)),
        )
    }

    @Test
    fun `snow clearing done in december is due again in january`() {
        assertEquals(
            LocalDate.of(2027, 1, 14),
            MaintenanceDueCalculator.dueDate(LocalDate.of(2026, 12, 15), 30, winter, LocalDate.of(2026, 12, 20)),
        )
    }

    @Test
    fun `a february 29 window opening falls back to february 28 in a common year`() {
        val leapWindow = SeasonWindow(MonthDay.of(2, 29), MonthDay.of(3, 31))
        assertEquals(LocalDate.of(2027, 2, 28), leapWindow.nextOpening(LocalDate.of(2027, 1, 1)))
    }

    // --- effectiveLastDone ------------------------------------------------

    private fun rule(anchorDate: LocalDate? = null) = MaintenanceRule(
        id = 1L, orgId = 10L, gardenAreaId = 5L,
        activity = MaintenanceActivity.WEED, intervalDays = 21,
        anchorDate = anchorDate,
    )

    @Test
    fun `with neither anchor nor event there is no last-done date`() {
        assertNull(MaintenanceDueCalculator.effectiveLastDone(rule(), null))
    }

    @Test
    fun `an anchor alone is the last-done date`() {
        assertEquals(
            LocalDate.of(2026, 6, 1),
            MaintenanceDueCalculator.effectiveLastDone(rule(LocalDate.of(2026, 6, 1)), null),
        )
    }

    @Test
    fun `a resolved event alone is the last-done date`() {
        assertEquals(
            LocalDate.of(2026, 6, 4),
            MaintenanceDueCalculator.effectiveLastDone(rule(), LocalDate.of(2026, 6, 4)),
        )
    }

    @Test
    fun `a newer event wins over an older anchor`() {
        assertEquals(
            LocalDate.of(2026, 6, 20),
            MaintenanceDueCalculator.effectiveLastDone(
                rule(LocalDate.of(2026, 5, 1)), LocalDate.of(2026, 6, 20),
            ),
        )
    }

    @Test
    fun `a newer anchor wins over an older event`() {
        assertEquals(
            LocalDate.of(2026, 6, 20),
            MaintenanceDueCalculator.effectiveLastDone(
                rule(LocalDate.of(2026, 6, 20)), LocalDate.of(2026, 5, 1),
            ),
        )
    }

    @Test
    fun `an anchor pushes the due date out by the full interval`() {
        val r = rule(LocalDate.of(2026, 6, 1))
        assertEquals(
            LocalDate.of(2026, 6, 22),
            MaintenanceDueCalculator.dueDate(
                MaintenanceDueCalculator.effectiveLastDone(r, null),
                r.intervalDays,
                MaintenanceDueCalculator.windowOf(r),
                LocalDate.of(2026, 6, 1),
            ),
        )
    }
}
