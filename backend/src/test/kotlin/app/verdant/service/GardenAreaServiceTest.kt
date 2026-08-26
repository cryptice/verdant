package app.verdant.service

import app.verdant.dto.CreateGardenAreaEventRequest
import app.verdant.dto.CreateGardenAreaRequest
import app.verdant.dto.UpdateGardenAreaRequest
import app.verdant.entity.Garden
import app.verdant.entity.GardenArea
import app.verdant.entity.GardenAreaCategory
import app.verdant.entity.GardenAreaEvent
import app.verdant.repository.GardenAreaEventRepository
import app.verdant.repository.GardenAreaPhotoRepository
import app.verdant.repository.GardenAreaRepository
import app.verdant.repository.GardenRepository
import jakarta.ws.rs.BadRequestException
import jakarta.ws.rs.NotFoundException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDate

class GardenAreaServiceTest {

    private val areas: GardenAreaRepository = mock()
    private val events: GardenAreaEventRepository = mock()
    private val photos: GardenAreaPhotoRepository = mock()
    private val gardens: GardenRepository = mock()
    private val storage: StorageService = mock()
    private val service = GardenAreaService(areas, events, photos, gardens, storage)

    private val orgId = 10L
    private val otherOrgId = 99L
    private val gardenId = 1L
    private val areaId = 5L

    private val garden = Garden(id = gardenId, name = "Trädgården", orgId = orgId)
    private val area = GardenArea(
        id = areaId, gardenId = gardenId, name = "Grusgång",
        category = GardenAreaCategory.WALKWAY,
    )

    @Test
    fun `createArea persists under the garden`() {
        whenever(gardens.findById(gardenId)).thenReturn(garden)
        whenever(areas.persist(any())).thenReturn(area)

        val result = service.createArea(
            gardenId,
            CreateGardenAreaRequest(name = "Grusgång", category = "WALKWAY"),
            orgId,
        )

        assertEquals("Grusgång", result.name)
        assertEquals("WALKWAY", result.category)
        assertEquals(gardenId, result.gardenId)
    }

    @Test
    fun `createArea rejects an unknown category`() {
        whenever(gardens.findById(gardenId)).thenReturn(garden)

        assertThrows<BadRequestException> {
            service.createArea(gardenId, CreateGardenAreaRequest(name = "X", category = "PATIO"), orgId)
        }
        verify(areas, never()).persist(any())
    }

    @Test
    fun `createArea hides a garden belonging to another org`() {
        whenever(gardens.findById(gardenId)).thenReturn(garden.copy(orgId = otherOrgId))

        assertThrows<NotFoundException> {
            service.createArea(gardenId, CreateGardenAreaRequest(name = "X", category = "LAWN"), orgId)
        }
    }

    @Test
    fun `getArea hides an area in another org's garden`() {
        whenever(areas.findById(areaId)).thenReturn(area)
        whenever(gardens.findById(gardenId)).thenReturn(garden.copy(orgId = otherOrgId))

        assertThrows<NotFoundException> { service.getArea(areaId, orgId) }
    }

    @Test
    fun `updateArea leaves omitted fields untouched`() {
        whenever(areas.findById(areaId)).thenReturn(area.copy(description = "Ursprunglig"))
        whenever(gardens.findById(gardenId)).thenReturn(garden)

        val result = service.updateArea(areaId, UpdateGardenAreaRequest(name = "Nytt namn"), orgId)

        assertEquals("Nytt namn", result.name)
        assertEquals("Ursprunglig", result.description)
        assertEquals("WALKWAY", result.category)
    }

    @Test
    fun `logEvent accepts an activity that applies to areas`() {
        whenever(areas.findById(areaId)).thenReturn(area)
        whenever(gardens.findById(gardenId)).thenReturn(garden)
        whenever(events.persist(any())).thenAnswer { it.arguments[0] as GardenAreaEvent }

        val result = service.logEvent(
            areaId,
            CreateGardenAreaEventRequest(activityType = "WEED", eventDate = LocalDate.of(2026, 6, 1)),
            orgId,
        )

        assertEquals("WEED", result.eventType)
        assertEquals(LocalDate.of(2026, 6, 1), result.eventDate)
    }

    @Test
    fun `logEvent accepts a plain note`() {
        whenever(areas.findById(areaId)).thenReturn(area)
        whenever(gardens.findById(gardenId)).thenReturn(garden)
        whenever(events.persist(any())).thenAnswer { it.arguments[0] as GardenAreaEvent }

        val result = service.logEvent(
            areaId,
            CreateGardenAreaEventRequest(activityType = "NOTE", notes = "Grus behöver fyllas på"),
            orgId,
        )

        assertEquals("NOTE", result.eventType)
    }

    @Test
    fun `logEvent rejects a bed-only activity`() {
        whenever(areas.findById(areaId)).thenReturn(area)
        whenever(gardens.findById(gardenId)).thenReturn(garden)

        assertThrows<BadRequestException> {
            service.logEvent(areaId, CreateGardenAreaEventRequest(activityType = "FERTILIZE"), orgId)
        }
        verify(events, never()).persist(any())
    }
}
