package app.verdant.android.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EnumsTest {

    @Test
    fun `PlantEventType fromWire parses known names`() {
        assertEquals(PlantEventType.SEEDED, PlantEventType.fromWire("SEEDED"))
        assertEquals(PlantEventType.APPLIED_SUPPLY, PlantEventType.fromWire("APPLIED_SUPPLY"))
        assertEquals(PlantEventType.WEEDED, PlantEventType.fromWire("WEEDED"))
    }

    @Test
    fun `PlantEventType fromWire returns null for unknown or null`() {
        assertNull(PlantEventType.fromWire(null))
        assertNull(PlantEventType.fromWire(""))
        assertNull(PlantEventType.fromWire("NOT_A_REAL_EVENT"))
    }

    @Test
    fun `PlantStatus fromWire parses all defined values`() {
        PlantStatus.entries.forEach { status ->
            assertEquals(status, PlantStatus.fromWire(status.name))
        }
    }

    @Test
    fun `TaskActivity BED_SCOPED contains exactly water fertilize weed`() {
        assertEquals(
            setOf(TaskActivity.WATER, TaskActivity.FERTILIZE, TaskActivity.WEED),
            TaskActivity.BED_SCOPED,
        )
    }

    @Test
    fun `TaskActivity BED_SCOPED excludes species-scoped activities`() {
        assertTrue(TaskActivity.SOW !in TaskActivity.BED_SCOPED)
        assertTrue(TaskActivity.HARVEST !in TaskActivity.BED_SCOPED)
        assertTrue(TaskActivity.PLANT !in TaskActivity.BED_SCOPED)
    }
}
