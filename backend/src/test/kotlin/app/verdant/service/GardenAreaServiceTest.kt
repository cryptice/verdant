package app.verdant.service

import app.verdant.dto.CreateGardenAreaEventRequest
import app.verdant.dto.CreateGardenAreaPhotoRequest
import app.verdant.dto.CreateGardenAreaRequest
import app.verdant.dto.UpdateGardenAreaRequest
import app.verdant.entity.BedPhotoReason
import app.verdant.entity.Garden
import app.verdant.entity.GardenArea
import app.verdant.entity.GardenAreaCategory
import app.verdant.entity.GardenAreaEvent
import app.verdant.entity.GardenAreaPhoto
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
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
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
    private val photoId = 77L

    private val garden = Garden(id = gardenId, name = "Trädgården", orgId = orgId)
    private val area = GardenArea(
        id = areaId, gardenId = gardenId, name = "Grusgång",
        category = GardenAreaCategory.WALKWAY,
    )

    @Test
    fun `createArea persists under the garden`() {
        whenever(gardens.findById(gardenId)).thenReturn(garden)
        // Echo back what the service actually built (plus the id real
        // persistence would assign) rather than a fixture, so the assertions
        // below genuinely check that the request fields were threaded through.
        whenever(areas.persist(any()))
            .thenAnswer { (it.arguments[0] as GardenArea).copy(id = areaId) }

        val result = service.createArea(
            gardenId,
            CreateGardenAreaRequest(
                name = "Grusgång",
                description = "Runt rabatten",
                category = "WALKWAY",
                sizeSqm = 12.5,
            ),
            orgId,
        )

        assertEquals(areaId, result.id)
        assertEquals("Grusgång", result.name)
        assertEquals("Runt rabatten", result.description)
        assertEquals("WALKWAY", result.category)
        assertEquals(12.5, result.sizeSqm)
        assertEquals(gardenId, result.gardenId)
        assertEquals("Trädgården", result.gardenName)
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

    // ── photos ──────────────────────────────────────────────────────────────

    @Test
    fun `addPhoto uploads and stores a server-minted url`() {
        val mintedUrl = "https://storage.googleapis.com/verdant-species/areas/$areaId/$photoId.jpg"
        whenever(areas.findById(areaId)).thenReturn(area)
        whenever(gardens.findById(gardenId)).thenReturn(garden)
        whenever(photos.persist(any()))
            .thenAnswer { (it.arguments[0] as GardenAreaPhoto).copy(id = photoId) }
        whenever(storage.uploadGardenAreaPhoto(eq(areaId), eq(photoId), any())).thenReturn(mintedUrl)

        val result = service.addPhoto(
            areaId,
            CreateGardenAreaPhotoRequest(imageBase64 = "QUJD", reason = "PROGRESS"),
            orgId,
        )

        // The row goes in with an empty URL; only StorageService decides the
        // final one. The client has no say in the storage path, so a later
        // deletePhoto can never be aimed at another org's blob.
        val persisted = argumentCaptor<GardenAreaPhoto>()
        verify(photos).persist(persisted.capture())
        assertEquals("", persisted.firstValue.photoUrl)
        assertEquals(areaId, persisted.firstValue.gardenAreaId)

        verify(storage).uploadGardenAreaPhoto(areaId, photoId, "QUJD")
        verify(photos).updatePhotoUrl(photoId, mintedUrl)
        assertEquals(mintedUrl, result.photoUrl)
        assertEquals("PROGRESS", result.reason)
    }

    @Test
    fun `addPhoto rejects an unknown photo reason`() {
        whenever(areas.findById(areaId)).thenReturn(area)
        whenever(gardens.findById(gardenId)).thenReturn(garden)

        assertThrows<BadRequestException> {
            service.addPhoto(
                areaId,
                CreateGardenAreaPhotoRequest(imageBase64 = "QUJD", reason = "SELFIE"),
                orgId,
            )
        }
        verify(photos, never()).persist(any())
        verify(storage, never()).uploadGardenAreaPhoto(any(), any(), any())
    }

    @Test
    fun `deletePhoto refuses a photo belonging to another area`() {
        whenever(areas.findById(areaId)).thenReturn(area)
        whenever(gardens.findById(gardenId)).thenReturn(garden)
        whenever(photos.findById(photoId)).thenReturn(
            GardenAreaPhoto(
                id = photoId,
                gardenAreaId = areaId + 1,
                photoUrl = "https://storage.googleapis.com/verdant-species/areas/${areaId + 1}/$photoId.jpg",
                reason = BedPhotoReason.PROGRESS,
            )
        )

        assertThrows<NotFoundException> { service.deletePhoto(areaId, photoId, orgId) }
        verify(storage, never()).deleteByPath(any())
        verify(photos, never()).delete(any())
    }
}
