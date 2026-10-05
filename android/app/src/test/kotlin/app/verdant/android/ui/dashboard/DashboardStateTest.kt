package app.verdant.android.ui.dashboard

import app.verdant.android.data.model.DashboardResponse
import app.verdant.android.data.model.DashboardStats
import app.verdant.android.data.model.GardenSummary
import app.verdant.android.data.model.UserResponse
import org.junit.Assert.*
import org.junit.Test

class DashboardStateTest {
    private val garden = GardenSummary(7, "Garden", null, 0, 0)
    private fun state(vararg gardens: GardenSummary) = DashboardState(
        isLoading = false,
        dashboard = DashboardResponse(
            UserResponse(1, "test@example.com", "Test", null, "USER", createdAt = ""),
            gardens.toList(),
            DashboardStats(gardens.size, gardens.sumOf { it.bedCount }, 0),
        ),
    )

    @Test fun `prompt requires loaded data and exactly one empty garden`() {
        assertNull(DashboardState().gardenNeedingBeds)
        assertNull(state().gardenNeedingBeds)
        assertEquals(garden, state(garden).gardenNeedingBeds)
        assertNull(state(garden, garden.copy(id = 8)).gardenNeedingBeds)
        assertNull(state(garden, garden.copy(id = 8, bedCount = 1)).gardenNeedingBeds)
    }

    @Test fun `prompt disappears when first bed is added`() {
        assertEquals(7L, state(garden).gardenNeedingBeds?.id)
        assertNull(state(garden.copy(bedCount = 1)).gardenNeedingBeds)
    }
}
