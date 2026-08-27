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

    /**
     * The rule carries a date this client cannot read. [dueState] runs inside
     * composition, so a parse failure here would take down the whole detail
     * screen — this renders as no badge instead.
     */
    data object Unknown : DueState
}

/** Whole days between two ISO yyyy-MM-dd strings, or null if either will not parse. */
private fun daysBetween(fromIso: String?, toIso: String?): Int? {
    if (fromIso == null || toIso == null) return null
    return runCatching {
        java.time.temporal.ChronoUnit.DAYS.between(
            java.time.LocalDate.parse(fromIso),
            java.time.LocalDate.parse(toIso),
        ).toInt()
    }.getOrNull()
}

/**
 * [MaintenanceRuleResponse.nextDueDate] is computed server-side; this only
 * classifies it for display. A paused rule is never overdue — pausing must
 * stop a rule nagging, not freeze it in a red state.
 */
fun dueState(rule: MaintenanceRuleResponse, todayIso: String): DueState {
    if (!rule.active) return DueState.Inactive
    val delta = daysBetween(todayIso, rule.nextDueDate) ?: return DueState.Unknown
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

/**
 * The update to send for an edited rule.
 *
 * Every field of [UpdateMaintenanceRuleRequest] is nullable, so the server
 * cannot tell "omitted" from "explicitly null" and reads both as "keep the
 * current value". Emptying a field is therefore an explicit flag — and the
 * server rejects a flag that travels with a replacement value for the same
 * field. Both rules are enforced here, once, rather than in each screen.
 *
 * A season window is all-or-none: pass all four bounds to set one, or none
 * of them to remove whatever the rule currently has.
 */
fun maintenanceRuleUpdate(
    rule: MaintenanceRuleResponse,
    activityType: String,
    intervalDays: Int,
    seasonStartMonth: Int? = null,
    seasonStartDay: Int? = null,
    seasonEndMonth: Int? = null,
    seasonEndDay: Int? = null,
    notes: String? = null,
): UpdateMaintenanceRuleRequest {
    val wantsWindow = seasonStartMonth != null && seasonStartDay != null &&
        seasonEndMonth != null && seasonEndDay != null
    return UpdateMaintenanceRuleRequest(
        activityType = activityType,
        intervalDays = intervalDays,
        seasonStartMonth = if (wantsWindow) seasonStartMonth else null,
        seasonStartDay = if (wantsWindow) seasonStartDay else null,
        seasonEndMonth = if (wantsWindow) seasonEndMonth else null,
        seasonEndDay = if (wantsWindow) seasonEndDay else null,
        clearSeasonWindow = !wantsWindow && hasSeasonWindow(rule),
        notes = notes,
        clearNotes = notes == null && rule.notes != null,
    )
}
