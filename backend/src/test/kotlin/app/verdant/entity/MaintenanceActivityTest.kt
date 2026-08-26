package app.verdant.entity

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class MaintenanceActivityTest {

    @Test
    fun `every bed activity maps to a plant event type`() {
        val bedActivities = MaintenanceActivity.forTarget(MaintenanceTarget.BED)
        assertTrue(bedActivities.isNotEmpty())
        bedActivities.forEach { activity ->
            assertTrue(
                activity.bedEventType != null,
                "$activity applies to beds but has no bedEventType, so bed history could not record it",
            )
        }
    }

    @Test
    fun `bed activities are exactly water weed and fertilize`() {
        assertEquals(
            listOf(MaintenanceActivity.WATER, MaintenanceActivity.WEED, MaintenanceActivity.FERTILIZE),
            MaintenanceActivity.forTarget(MaintenanceTarget.BED),
        )
    }

    @Test
    fun `fertilize does not apply to areas`() {
        assertFalse(MaintenanceActivity.FERTILIZE.appliesTo(MaintenanceTarget.GARDEN_AREA))
        assertTrue(MaintenanceActivity.FERTILIZE.appliesTo(MaintenanceTarget.BED))
    }

    @Test
    fun `mow applies only to areas`() {
        assertTrue(MaintenanceActivity.MOW.appliesTo(MaintenanceTarget.GARDEN_AREA))
        assertFalse(MaintenanceActivity.MOW.appliesTo(MaintenanceTarget.BED))
    }

    @Test
    fun `parse accepts a known name and rejects an unknown one`() {
        assertEquals(MaintenanceActivity.WEED, MaintenanceActivity.parse("WEED"))
        assertThrows<IllegalArgumentException> { MaintenanceActivity.parse("MULCH") }
    }

    @Test
    fun `every activity applies to at least one target`() {
        MaintenanceActivity.entries.forEach { activity ->
            assertTrue(activity.targets.isNotEmpty(), "$activity applies to nothing")
        }
    }
}
