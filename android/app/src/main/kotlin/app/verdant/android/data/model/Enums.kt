package app.verdant.android.data.model

/** Mirrors backend `PlantEventType`. Used as wire strings; keep names in sync. */
enum class PlantEventType {
    SEEDED, POTTED_UP, PLANTED_OUT, HARVESTED, RECOVERED, REMOVED,
    NOTE, BUDDING, FIRST_BLOOM, PEAK_BLOOM, LAST_BLOOM,
    LIFTED, DIVIDED, STORED, PINCHED, DISBUDDED,
    APPLIED_SUPPLY, WATERED, MOVED, WEEDED;

    companion object {
        fun fromWire(value: String?): PlantEventType? =
            value?.let { runCatching { valueOf(it) }.getOrNull() }
    }
}

/** Mirrors backend `PlantStatus`. */
enum class PlantStatus {
    SEEDED, POTTED_UP, PLANTED_OUT, GROWING, HARVESTED, RECOVERED, REMOVED, DORMANT;

    companion object {
        fun fromWire(value: String?): PlantStatus? =
            value?.let { runCatching { valueOf(it) }.getOrNull() }
    }
}

/**
 * Activity types for `ScheduledTask.activityType` and various task-routing call
 * sites. Some are species-scoped, some are bed-scoped — see [BED_SCOPED].
 */
enum class TaskActivity {
    SOW, POT_UP, PLANT, HARVEST, RECOVER, DISCARD,
    WATER, FERTILIZE, WEED;

    companion object {
        val BED_SCOPED: Set<TaskActivity> = setOf(WATER, FERTILIZE, WEED)
        fun fromWire(value: String?): TaskActivity? =
            value?.let { runCatching { valueOf(it) }.getOrNull() }
    }
}
