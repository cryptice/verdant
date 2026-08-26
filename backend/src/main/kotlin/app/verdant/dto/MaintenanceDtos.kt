package app.verdant.dto

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant
import java.time.LocalDate

data class MaintenanceRuleResponse(
    val id: Long,
    val bedId: Long?,
    val bedName: String?,
    val gardenAreaId: Long?,
    val gardenAreaName: String?,
    val activityType: String,
    val intervalDays: Int,
    val anchorDate: LocalDate?,
    val seasonStartMonth: Int?,
    val seasonStartDay: Int?,
    val seasonEndMonth: Int?,
    val seasonEndDay: Int?,
    val active: Boolean,
    val notes: String?,
    /** Derived from the event log, not stored. Null when never done. */
    val lastDoneDate: LocalDate?,
    /** Derived: when the next task will be created. */
    val nextDueDate: LocalDate,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class CreateMaintenanceRuleRequest(
    val bedId: Long? = null,
    val gardenAreaId: Long? = null,
    @field:NotBlank
    val activityType: String,
    @field:Min(1)
    val intervalDays: Int,
    val anchorDate: LocalDate? = null,
    val seasonStartMonth: Int? = null,
    val seasonStartDay: Int? = null,
    val seasonEndMonth: Int? = null,
    val seasonEndDay: Int? = null,
    @field:Size(max = 2000)
    val notes: String? = null,
)

data class UpdateMaintenanceRuleRequest(
    val activityType: String? = null,
    @field:Min(1)
    val intervalDays: Int? = null,
    val anchorDate: LocalDate? = null,
    val seasonStartMonth: Int? = null,
    val seasonStartDay: Int? = null,
    val seasonEndMonth: Int? = null,
    val seasonEndDay: Int? = null,
    /** Set true to remove an existing season window so the rule applies year-round. */
    val clearSeasonWindow: Boolean = false,
    val active: Boolean? = null,
    @field:Size(max = 2000)
    val notes: String? = null,
)
