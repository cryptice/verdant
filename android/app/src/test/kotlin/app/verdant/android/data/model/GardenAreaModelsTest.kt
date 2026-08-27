package app.verdant.android.data.model

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GardenAreaModelsTest {

    private val gson = Gson()

    @Test
    fun `GardenAreaResponse deserializes every field the backend sends`() {
        val json = """
            {"id":5,"gardenId":1,"gardenName":"Trädgården","name":"Grusgången",
             "description":"Vid växthuset","category":"WALKWAY",
             "boundaryJson":null,"sizeSqm":12.5,
             "createdAt":"2026-06-01T10:00:00Z","updatedAt":"2026-06-01T10:00:00Z"}
        """.trimIndent()

        val area = gson.fromJson(json, GardenAreaResponse::class.java)

        assertEquals(5L, area.id)
        assertEquals(1L, area.gardenId)
        assertEquals("Trädgården", area.gardenName)
        assertEquals("Grusgången", area.name)
        assertEquals("Vid växthuset", area.description)
        assertEquals("WALKWAY", area.category)
        assertNull(area.boundaryJson)
        assertEquals(12.5, area.sizeSqm!!, 0.001)
    }

    @Test
    fun `MaintenanceRuleResponse deserializes the derived dates and a null season window`() {
        val json = """
            {"id":7,"bedId":null,"bedName":null,"gardenAreaId":5,"gardenAreaName":"Grusgången",
             "activityType":"WEED","intervalDays":21,"anchorDate":null,
             "seasonStartMonth":null,"seasonStartDay":null,
             "seasonEndMonth":null,"seasonEndDay":null,
             "active":true,"notes":null,
             "lastDoneDate":null,"nextDueDate":"2026-06-25",
             "createdAt":"2026-06-01T10:00:00Z","updatedAt":"2026-06-01T10:00:00Z"}
        """.trimIndent()

        val rule = gson.fromJson(json, MaintenanceRuleResponse::class.java)

        assertNull(rule.bedId)
        assertEquals(5L, rule.gardenAreaId)
        assertEquals("WEED", rule.activityType)
        assertEquals(21, rule.intervalDays)
        assertNull(rule.lastDoneDate)
        assertEquals("2026-06-25", rule.nextDueDate)
        assertTrue(rule.active)
        assertNull(rule.seasonStartMonth)
    }

    @Test
    fun `MaintenanceRuleResponse deserializes a wrap-around season window`() {
        val json = """
            {"id":8,"bedId":3,"bedName":"Bädd 1","gardenAreaId":null,"gardenAreaName":null,
             "activityType":"WATER","intervalDays":7,"anchorDate":"2026-05-01",
             "seasonStartMonth":11,"seasonStartDay":1,
             "seasonEndMonth":3,"seasonEndDay":31,
             "active":false,"notes":"Vintertid",
             "lastDoneDate":"2026-05-20","nextDueDate":"2026-11-01",
             "createdAt":"2026-06-01T10:00:00Z","updatedAt":"2026-06-01T10:00:00Z"}
        """.trimIndent()

        val rule = gson.fromJson(json, MaintenanceRuleResponse::class.java)

        assertEquals(3L, rule.bedId)
        assertEquals(11, rule.seasonStartMonth)
        assertEquals(31, rule.seasonEndDay)
        assertEquals("2026-05-20", rule.lastDoneDate)
        assertEquals(false, rule.active)
    }

    @Test
    fun `ScheduledTaskResponse carries the new area and rule fields`() {
        val json = """
            {"id":42,"speciesId":null,"speciesName":null,
             "bedId":null,"bedName":null,"gardenName":"Trädgården",
             "gardenAreaId":5,"gardenAreaName":"Grusgången","maintenanceRuleId":7,
             "activityType":"WEED","earliestDate":"2026-06-25","deadline":"2026-06-25",
             "targetCount":1,"remainingCount":1,"status":"PENDING","notes":null,
             "seasonId":null,"successionScheduleId":null,
             "originGroupId":null,"originGroupName":null,"acceptableSpecies":[],
             "createdAt":"2026-06-25T03:30:00Z","updatedAt":"2026-06-25T03:30:00Z"}
        """.trimIndent()

        val task = gson.fromJson(json, ScheduledTaskResponse::class.java)

        assertEquals(5L, task.gardenAreaId)
        assertEquals("Grusgången", task.gardenAreaName)
        assertEquals(7L, task.maintenanceRuleId)
    }

    @Test
    fun `CreateGardenAreaPhotoRequest serializes imageBase64, never a url`() {
        val json = gson.toJson(CreateGardenAreaPhotoRequest(imageBase64 = "QUJD", reason = "PROGRESS"))
        assertTrue(json.contains("imageBase64"))
        // The API mints the storage path itself; a client-supplied URL was a
        // cross-tenant delete hole and no longer exists in the contract.
        assertTrue(!json.contains("photoUrl"))
    }
}
