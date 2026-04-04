package app.verdant.entity

import java.time.Instant
import java.time.LocalDate

data class ScheduledTask(
    val id: Long? = null,
    val orgId: Long,
    val speciesId: Long? = null,
    val activityType: String,
    val deadline: LocalDate,
    val targetCount: Int,
    val remainingCount: Int,
    val status: ScheduledTaskStatus = ScheduledTaskStatus.PENDING,
    val notes: String? = null,
    val seasonId: Long? = null,
    val successionScheduleId: Long? = null,
    val originGroupId: Long? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
)

enum class ScheduledTaskStatus { PENDING, COMPLETED }
