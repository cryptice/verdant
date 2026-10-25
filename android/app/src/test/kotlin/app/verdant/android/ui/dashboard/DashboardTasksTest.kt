package app.verdant.android.ui.dashboard

import app.verdant.android.data.api.VerdantApi
import app.verdant.android.data.model.ScheduledTaskResponse
import app.verdant.android.data.repository.DefaultTaskRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.lang.reflect.Proxy
import java.time.LocalDate

class DashboardTasksTest {
    private fun task(id: Long, deadline: String?, earliestDate: String? = null, status: String = "PENDING") = ScheduledTaskResponse(
        id = id, speciesId = null, speciesName = null, activityType = "TODO", earliestDate = earliestDate, deadline = deadline,
        targetCount = 1, remainingCount = 1, status = status, notes = "Task $id", createdAt = "", updatedAt = "",
    )
    @Test fun `seven day window groups tasks and excludes completed undated and outside dates`() {
        val days = dashboardTaskDays(listOf(task(1, "2026-10-10"), task(2, "2026-10-11"), task(3, "2026-10-16"), task(4, "2026-10-17"), task(5, null), task(6, "2026-10-09"), task(7, "2026-10-10", status = "COMPLETED"), task(8, "2026-10-20", "2026-10-12")), LocalDate.parse("2026-10-10"))
        assertEquals(listOf(listOf(1L), listOf(2L), listOf(8L), emptyList(), emptyList(), emptyList(), listOf(3L)), days.map { day -> day.tasks.map { it.id } })
    }
    @Test fun `overdue collects pending tasks past their deadline oldest first and keeps them out of the days`() {
        val tasks = listOf(task(1, "2026-10-09"), task(2, "2026-10-01"), task(3, "2026-10-10"), task(4, "2026-10-08", status = "COMPLETED"), task(5, null), task(6, "2026-10-09", "2026-10-10"))
        val today = LocalDate.parse("2026-10-10")
        assertEquals(listOf(2L, 1L, 6L), dashboardOverdueTasks(tasks, today).map { it.id })
        assertEquals(listOf(3L), dashboardTaskDays(tasks, today)[0].tasks.map { it.id })
    }
    @Test fun `calendar days span month and year boundaries`() {
        val days = dashboardTaskDays(listOf(task(1, "2027-01-01")), LocalDate.parse("2026-12-30"))
        assertEquals(1L, days[2].tasks.single().id)
        assertEquals(LocalDate.parse("2027-01-05"), days.last().date)
    }
    @Test fun `dashboard fetches tasks beyond the first page`() = runTest {
        val offsets = mutableListOf<Int>()
        val api = Proxy.newProxyInstance(VerdantApi::class.java.classLoader, arrayOf(VerdantApi::class.java)) { _, method, args ->
            check(method.name == "getTasks")
            assertEquals(200, args!![0])
            val offset = args[1] as Int
            offsets += offset
            if (offset == 0) (1L..200L).map { task(it, "2026-10-20") } else listOf(task(201, "2026-10-10"))
        } as VerdantApi
        val tasks = DefaultTaskRepository(api).listAll()
        assertEquals(listOf(0, 200), offsets)
        assertEquals(201, tasks.size)
        assertEquals(201L, dashboardTaskDays(tasks, LocalDate.parse("2026-10-10"))[0].tasks.single().id)
    }
}
