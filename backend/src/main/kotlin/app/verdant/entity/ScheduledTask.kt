package app.verdant.entity

import java.time.Instant
import java.time.LocalDate

data class ScheduledTask(
    val id: Long? = null,
    val userId: Long,
    val speciesId: Long,
    val activityType: String,
    val deadline: LocalDate,
    val targetCount: Int,
    val remainingCount: Int,
    val status: ScheduledTaskStatus = ScheduledTaskStatus.PENDING,
    val notes: String? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
)

enum class ScheduledTaskStatus { PENDING, COMPLETED }
