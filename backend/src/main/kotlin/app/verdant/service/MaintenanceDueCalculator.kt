package app.verdant.service

import app.verdant.entity.MaintenanceRule
import java.time.LocalDate
import java.time.Month
import java.time.MonthDay
import java.time.Year

/**
 * A yearly-repeating window of dates, inclusive at both ends.
 *
 * When [start] is after [end] the window wraps the new year, so
 * `Nov 1 – Mar 31` is a single continuous winter season rather than an
 * empty range.
 */
data class SeasonWindow(val start: MonthDay, val end: MonthDay) {

    fun contains(date: LocalDate): Boolean {
        val md = MonthDay.from(date)
        return if (start <= end) md >= start && md <= end
        else md >= start || md <= end
    }

    /** The first date on or after [from] that falls inside the window. */
    fun nextOpening(from: LocalDate): LocalDate {
        if (contains(from)) return from
        val thisYear = start.atYearClamped(from.year)
        return if (thisYear >= from) thisYear else start.atYearClamped(from.year + 1)
    }

    /** Feb 29 in a common year lands on Feb 28 rather than throwing. */
    private fun MonthDay.atYearClamped(year: Int): LocalDate =
        if (month == Month.FEBRUARY && dayOfMonth == 29 && !Year.isLeap(year.toLong())) {
            LocalDate.of(year, 2, 28)
        } else {
            LocalDate.of(year, month, dayOfMonth)
        }

    companion object {
        /** Null unless all four bounds are present — the DB enforces all-or-none. */
        fun of(startMonth: Int?, startDay: Int?, endMonth: Int?, endDay: Int?): SeasonWindow? {
            if (startMonth == null || startDay == null || endMonth == null || endDay == null) return null
            return SeasonWindow(MonthDay.of(startMonth, startDay), MonthDay.of(endMonth, endDay))
        }
    }
}

object MaintenanceDueCalculator {

    /**
     * When the work next falls due.
     *
     * A rule that has never been done is due immediately, which is what a
     * gardener means by "weed the path every three weeks" typed in June.
     * A due date outside the season window slides to the window's next
     * opening, so nothing nags out of season and nothing piles up.
     */
    fun dueDate(
        lastDone: LocalDate?,
        intervalDays: Int,
        window: SeasonWindow?,
        today: LocalDate,
    ): LocalDate {
        val naive = lastDone?.plusDays(intervalDays.toLong()) ?: today
        return window?.nextOpening(naive) ?: naive
    }

    fun windowOf(rule: MaintenanceRule): SeasonWindow? = SeasonWindow.of(
        rule.seasonStartMonth, rule.seasonStartDay, rule.seasonEndMonth, rule.seasonEndDay,
    )

    /**
     * The date [dueDate] should count from: the later of the rule's anchor and
     * the newest matching event ([resolved], from LastDoneResolver).
     *
     * The anchor seeds a rule for work already done before the rule existed —
     * "prune every 90 days, last done three weeks ago" must wait, not fire
     * today. Taking the later of the two means an anchor can never drag the
     * clock backwards once real events exist.
     */
    fun effectiveLastDone(rule: MaintenanceRule, resolved: LocalDate?): LocalDate? =
        listOfNotNull(resolved, rule.anchorDate).maxOrNull()
}
