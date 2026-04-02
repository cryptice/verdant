package app.verdant.dto

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
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
    @field:NotNull
    val speciesId: Long,
    @field:NotBlank @field:Size(max = 255)
    val activityType: String,
    @field:NotNull
    val deadline: LocalDate,
    @field:Min(1)
    val targetCount: Int,
    @field:Size(max = 2000)
    val notes: String? = null,
    val seasonId: Long? = null,
    val successionScheduleId: Long? = null,
)

data class UpdateScheduledTaskRequest(
    val speciesId: Long? = null,
    @field:Size(max = 255)
    val activityType: String? = null,
    val deadline: LocalDate? = null,
    @field:Min(1)
    val targetCount: Int? = null,
    @field:Size(max = 2000)
    val notes: String? = null,
    val seasonId: Long? = null,
)

data class CompleteTaskPartiallyRequest(
    @field:Min(1)
    val processedCount: Int,
)
