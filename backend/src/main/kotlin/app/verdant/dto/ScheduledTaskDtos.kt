package app.verdant.dto

import java.time.Instant
import java.time.LocalDate

data class ScheduledTaskResponse(
    val id: Long,
    val speciesId: Long,
    val speciesName: String,
    val activityType: String,
    val deadline: LocalDate,
    val targetCount: Int,
    val remainingCount: Int,
    val status: String,
    val notes: String?,
    val seasonId: Long?,
    val successionScheduleId: Long?,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class CreateScheduledTaskRequest(
    val speciesId: Long,
    val activityType: String,
    val deadline: LocalDate,
    val targetCount: Int,
    val notes: String? = null,
    val seasonId: Long? = null,
    val successionScheduleId: Long? = null,
)

data class UpdateScheduledTaskRequest(
    val speciesId: Long? = null,
    val activityType: String? = null,
    val deadline: LocalDate? = null,
    val targetCount: Int? = null,
    val notes: String? = null,
    val seasonId: Long? = null,
)

data class CompleteTaskPartiallyRequest(
    val processedCount: Int,
)
