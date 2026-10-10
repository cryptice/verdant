package app.verdant.android.ui.dashboard

import app.verdant.android.data.model.ScheduledTaskResponse
import java.time.LocalDate

data class DashboardTaskDay(val date: LocalDate, val tasks: List<ScheduledTaskResponse>)

fun dashboardTaskDays(tasks: List<ScheduledTaskResponse>, today: LocalDate = LocalDate.now()): List<DashboardTaskDay> =
    (0L..6L).map { offset ->
        val date = today.plusDays(offset)
        DashboardTaskDay(date, tasks.filter {
            it.status == "PENDING" && (it.earliestDate ?: it.deadline) == date.toString()
        }.sortedWith(compareBy({ it.deadline ?: "" }, { it.id })))
    }
