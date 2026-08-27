package app.verdant.android.data.model

/** Mirrors the backend's MaintenanceActivity enum, in declaration order. */
val MAINTENANCE_ACTIVITIES = listOf(
    "WATER", "WEED", "FERTILIZE",
    "MOW", "RAKE", "PRUNE", "EDGE", "SWEEP", "TOP_UP", "CLEAN", "INSPECT",
)

/** Mirrors the backend's GardenAreaCategory enum, in declaration order. */
val AREA_CATEGORIES = listOf(
    "WALKWAY", "LAWN", "HEDGE", "COMPOST",
    "GREENHOUSE", "WATER_FEATURE", "STRUCTURE", "OTHER",
)

enum class MaintenanceTarget { BED, AREA }

private val BED_ACTIVITIES = listOf("WATER", "WEED", "FERTILIZE")

/**
 * Which activities the server will accept for a target. Offering one it
 * rejects — FERTILIZE on an area, MOW on a bed — is a guaranteed 400, so
 * pickers must filter by this rather than showing everything.
 */
fun activitiesForTarget(target: MaintenanceTarget): List<String> = when (target) {
    MaintenanceTarget.BED -> BED_ACTIVITIES
    MaintenanceTarget.AREA -> MAINTENANCE_ACTIVITIES.filter { it != "FERTILIZE" }
}

sealed interface DueState {
    data object Inactive : DueState
    data class Overdue(val days: Int) : DueState
    data object Due : DueState
    data class Upcoming(val days: Int) : DueState
}

/** Whole days between two ISO yyyy-MM-dd strings. */
private fun daysBetween(fromIso: String, toIso: String): Int {
    val from = java.time.LocalDate.parse(fromIso)
    val to = java.time.LocalDate.parse(toIso)
    return java.time.temporal.ChronoUnit.DAYS.between(from, to).toInt()
}

/**
 * [MaintenanceRuleResponse.nextDueDate] is computed server-side; this only
 * classifies it for display. A paused rule is never overdue — pausing must
 * stop a rule nagging, not freeze it in a red state.
 */
fun dueState(rule: MaintenanceRuleResponse, todayIso: String): DueState {
    if (!rule.active) return DueState.Inactive
    val delta = daysBetween(todayIso, rule.nextDueDate)
    return when {
        delta < 0 -> DueState.Overdue(-delta)
        delta == 0 -> DueState.Due
        else -> DueState.Upcoming(delta)
    }
}

fun hasSeasonWindow(rule: MaintenanceRuleResponse): Boolean =
    rule.seasonStartMonth != null && rule.seasonStartDay != null &&
        rule.seasonEndMonth != null && rule.seasonEndDay != null

/**
 * The window as ((startMonth, startDay), (endMonth, endDay)), or null when
 * the rule applies year-round. Returned in stored order: a start after the
 * end means the window wraps the new year, which is valid.
 */
fun seasonWindowMonthDays(
    rule: MaintenanceRuleResponse,
): Pair<Pair<Int, Int>, Pair<Int, Int>>? {
    if (!hasSeasonWindow(rule)) return null
    return (rule.seasonStartMonth!! to rule.seasonStartDay!!) to
        (rule.seasonEndMonth!! to rule.seasonEndDay!!)
}
