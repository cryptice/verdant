package app.verdant.entity

import java.time.Instant
import java.time.LocalDate

/**
 * A recurring piece of maintenance on exactly one bed or garden area.
 *
 * There is no last-done field: see LastDoneResolver, which derives it from
 * the event log so that hand-logged work also resets the clock.
 */
data class MaintenanceRule(
    val id: Long? = null,
    val orgId: Long,
    val bedId: Long? = null,
    val gardenAreaId: Long? = null,
    val activity: MaintenanceActivity,
    val intervalDays: Int,
    val anchorDate: LocalDate? = null,
    val seasonStartMonth: Int? = null,
    val seasonStartDay: Int? = null,
    val seasonEndMonth: Int? = null,
    val seasonEndDay: Int? = null,
    val active: Boolean = true,
    val notes: String? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
) {
    val target: MaintenanceTarget
        get() = if (bedId != null) MaintenanceTarget.BED else MaintenanceTarget.GARDEN_AREA
}
