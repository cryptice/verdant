package app.verdant.entity

/** What a maintenance rule can be attached to. */
enum class MaintenanceTarget { BED, GARDEN_AREA }

/**
 * Work that recurs on a bed or a garden area.
 *
 * [bedEventType] is how the activity is recorded in `bed_event`. Activities
 * without one cannot be attached to a bed, because bed history would have no
 * way to express them — that invariant is pinned by MaintenanceActivityTest.
 */
enum class MaintenanceActivity(
    val targets: Set<MaintenanceTarget>,
    val bedEventType: PlantEventType?,
) {
    WATER(setOf(MaintenanceTarget.BED, MaintenanceTarget.GARDEN_AREA), PlantEventType.WATERED),
    WEED(setOf(MaintenanceTarget.BED, MaintenanceTarget.GARDEN_AREA), PlantEventType.WEEDED),
    FERTILIZE(setOf(MaintenanceTarget.BED), PlantEventType.APPLIED_SUPPLY),
    MOW(setOf(MaintenanceTarget.GARDEN_AREA), null),
    RAKE(setOf(MaintenanceTarget.GARDEN_AREA), null),
    PRUNE(setOf(MaintenanceTarget.GARDEN_AREA), null),
    EDGE(setOf(MaintenanceTarget.GARDEN_AREA), null),
    SWEEP(setOf(MaintenanceTarget.GARDEN_AREA), null),
    TOP_UP(setOf(MaintenanceTarget.GARDEN_AREA), null),
    CLEAN(setOf(MaintenanceTarget.GARDEN_AREA), null),
    INSPECT(setOf(MaintenanceTarget.GARDEN_AREA), null);

    fun appliesTo(target: MaintenanceTarget): Boolean = target in targets

    companion object {
        fun forTarget(target: MaintenanceTarget): List<MaintenanceActivity> =
            entries.filter { it.appliesTo(target) }

        fun parse(value: String): MaintenanceActivity =
            entries.firstOrNull { it.name == value }
                ?: throw IllegalArgumentException("Unknown maintenance activity: $value")
    }
}

enum class GardenAreaCategory {
    WALKWAY, LAWN, HEDGE, COMPOST, GREENHOUSE, WATER_FEATURE, STRUCTURE, OTHER
}

/** Free-text area log entry, stored in the same column as activity events. */
const val AREA_EVENT_NOTE = "NOTE"
