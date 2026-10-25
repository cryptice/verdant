package app.verdant.android.ui.dashboard

import app.verdant.android.data.model.ScheduledTaskResponse
import java.time.LocalDate

data class DashboardTaskDay(val date: LocalDate, val tasks: List<ScheduledTaskResponse>)

private fun ScheduledTaskResponse.isOverdue(today: LocalDate): Boolean =
    status == "PENDING" && deadline != null && deadline.take(10) < today.toString()

/** Pending tasks whose deadline has passed; they get their own section ahead of the days. */
fun dashboardOverdueTasks(tasks: List<ScheduledTaskResponse>, today: LocalDate = LocalDate.now()): List<ScheduledTaskResponse> =
    tasks.filter { it.isOverdue(today) }.sortedWith(compareBy({ it.deadline }, { it.id }))

fun dashboardTaskDays(tasks: List<ScheduledTaskResponse>, today: LocalDate = LocalDate.now()): List<DashboardTaskDay> =
    (0L..6L).map { offset ->
        val date = today.plusDays(offset)
        DashboardTaskDay(date, tasks.filter {
            it.status == "PENDING" && !it.isOverdue(today) && (it.earliestDate ?: it.deadline) == date.toString()
        }.sortedWith(compareBy({ it.deadline ?: "" }, { it.id })))
    }
