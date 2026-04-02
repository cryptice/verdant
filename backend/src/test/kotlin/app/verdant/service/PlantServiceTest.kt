package app.verdant.service

import app.verdant.dto.BatchEventRequest
import app.verdant.dto.BatchSowRequest
import app.verdant.dto.CreatePlantEventRequest
import app.verdant.entity.Bed
import app.verdant.entity.Garden
import app.verdant.entity.Plant
import app.verdant.entity.PlantEvent
import app.verdant.entity.PlantEventType
import app.verdant.entity.PlantStatus
import app.verdant.repository.BedRepository
import app.verdant.repository.GardenRepository
import app.verdant.repository.PlantEventRepository
import app.verdant.repository.PlantRepository
import app.verdant.repository.SpeciesRepository
import jakarta.ws.rs.BadRequestException
import jakarta.ws.rs.ForbiddenException
import jakarta.ws.rs.NotFoundException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import java.time.LocalDate

class PlantServiceTest {

    private lateinit var plantRepository: PlantRepository
    private lateinit var plantEventRepository: PlantEventRepository
    private lateinit var bedRepository: BedRepository
    private lateinit var gardenRepository: GardenRepository
    private lateinit var speciesRepository: SpeciesRepository
    private lateinit var storageService: StorageService
    private lateinit var service: PlantService

    private val userId = 42L
    private val speciesId = 10L

    @BeforeEach
    fun setup() {
        plantRepository = mock()
        plantEventRepository = mock()
        bedRepository = mock()
        gardenRepository = mock()
        speciesRepository = mock()
        storageService = mock()
        service = PlantService(
            plantRepository,
            plantEventRepository,
            bedRepository,
            gardenRepository,
            speciesRepository,
            storageService,
        )
    }

    // ── Helpers ──

    private fun makePlant(id: Long, name: String = "Sunflower #1", status: PlantStatus = PlantStatus.SEEDED): Plant =
        Plant(
            id = id,
            name = name,
            speciesId = speciesId,
            plantedDate = LocalDate.now(),
            status = status,
            seedCount = 1,
            survivingCount = 1,
            userId = userId,
        )

    private fun makePersistedPlant(id: Long, request: BatchSowRequest, index: Int): Plant =
        Plant(
            id = id,
            name = "${request.name} #$index",
            speciesId = request.speciesId,
            plantedDate = LocalDate.now(),
            status = PlantStatus.SEEDED,
            seedCount = 1,
            survivingCount = 1,
            bedId = request.bedId,
            userId = userId,
        )

    private fun makePersistedEvent(id: Long, plantId: Long, eventType: PlantEventType): PlantEvent =
        PlantEvent(
            id = id,
            plantId = plantId,
            eventType = eventType,
            eventDate = LocalDate.now(),
            plantCount = 1,
        )

    // ── batchSow ──

    @Test
    fun `batchSow creates correct number of plants`() {
        val seedCount = 3
        val request = BatchSowRequest(
            speciesId = speciesId,
            name = "Zinnia",
            seedCount = seedCount,
        )

        whenever(plantRepository.persist(any())).thenAnswer { inv ->
            val plant = inv.getArgument<Plant>(0)
            // Extract the index from the name "Zinnia #N"
            val index = plant.name.substringAfterLast("#").trim().toLong()
            plant.copy(id = index)
        }
        whenever(plantEventRepository.persist(any())).thenAnswer { inv ->
            val event = inv.getArgument<PlantEvent>(0)
            event.copy(id = event.plantId * 10)
        }

        val response = service.batchSow(request, userId)

        assertEquals(seedCount, response.count)
        assertEquals(seedCount, response.plantIds.size)
        verify(plantRepository, times(seedCount)).persist(any())
    }

    @Test
    fun `batchSow names plants sequentially`() {
        val request = BatchSowRequest(
            speciesId = speciesId,
            name = "Dahlia",
            seedCount = 2,
        )

        val persisted = mutableListOf<Plant>()
        whenever(plantRepository.persist(any())).thenAnswer { inv ->
            val plant = inv.getArgument<Plant>(0)
            val withId = plant.copy(id = persisted.size + 1L)
            persisted.add(withId)
            withId
        }
        whenever(plantEventRepository.persist(any())).thenAnswer { inv ->
            inv.getArgument<PlantEvent>(0).copy(id = 99L)
        }

        service.batchSow(request, userId)

        assertEquals("Dahlia #1", persisted[0].name)
        assertEquals("Dahlia #2", persisted[1].name)
    }

    @Test
    fun `batchSow persists plants with SEEDED status`() {
        val request = BatchSowRequest(speciesId = speciesId, name = "Cosmos", seedCount = 2)

        val capturedPlants = mutableListOf<Plant>()
        whenever(plantRepository.persist(any())).thenAnswer { inv ->
            val plant = inv.getArgument<Plant>(0)
            val withId = plant.copy(id = capturedPlants.size + 1L)
            capturedPlants.add(withId)
            withId
        }
        whenever(plantEventRepository.persist(any())).thenAnswer { inv ->
            inv.getArgument<PlantEvent>(0).copy(id = 99L)
        }

        service.batchSow(request, userId)

        capturedPlants.forEach { assertEquals(PlantStatus.SEEDED, it.status) }
    }

    @Test
    fun `batchSow creates SEEDED event for each plant`() {
        val seedCount = 4
        val request = BatchSowRequest(speciesId = speciesId, name = "Peony", seedCount = seedCount)

        var idCounter = 1L
        whenever(plantRepository.persist(any())).thenAnswer { inv ->
            inv.getArgument<Plant>(0).copy(id = idCounter++)
        }
        val capturedEvents = mutableListOf<PlantEvent>()
        whenever(plantEventRepository.persist(any())).thenAnswer { inv ->
            val event = inv.getArgument<PlantEvent>(0)
            capturedEvents.add(event)
            event.copy(id = capturedEvents.size.toLong())
        }

        service.batchSow(request, userId)

        verify(plantEventRepository, times(seedCount)).persist(any())
        capturedEvents.forEach { assertEquals(PlantEventType.SEEDED, it.eventType) }
    }

    @Test
    fun `batchSow uploads image exactly once when seedCount is greater than 1`() {
        val seedCount = 5
        val imageBase64 = "base64encodedimage"
        val request = BatchSowRequest(
            speciesId = speciesId,
            name = "Rose",
            seedCount = seedCount,
            imageBase64 = imageBase64,
        )

        var idCounter = 1L
        whenever(plantRepository.persist(any())).thenAnswer { inv ->
            inv.getArgument<Plant>(0).copy(id = idCounter++)
        }
        whenever(plantEventRepository.persist(any())).thenAnswer { inv ->
            inv.getArgument<PlantEvent>(0).copy(id = 99L)
        }
        whenever(storageService.uploadImage(any(), any())).thenReturn("https://storage.example.com/plants/1/events/seeded.jpg")

        service.batchSow(request, userId)

        verify(storageService, times(1)).uploadImage(eq(imageBase64), any())
    }

    @Test
    fun `batchSow does not call uploadImage when no image provided`() {
        val request = BatchSowRequest(speciesId = speciesId, name = "Tulip", seedCount = 3)

        var idCounter = 1L
        whenever(plantRepository.persist(any())).thenAnswer { inv ->
            inv.getArgument<Plant>(0).copy(id = idCounter++)
        }
        whenever(plantEventRepository.persist(any())).thenAnswer { inv ->
            inv.getArgument<PlantEvent>(0).copy(id = 99L)
        }

        service.batchSow(request, userId)

        verify(storageService, never()).uploadImage(any(), any())
    }

    @Test
    fun `batchSow shares uploaded imageUrl across all events`() {
        val imageBase64 = "base64img"
        val seedCount = 3
        val expectedUrl = "https://storage.example.com/plants/1/events/seeded.jpg"
        val request = BatchSowRequest(
            speciesId = speciesId,
            name = "Aster",
            seedCount = seedCount,
            imageBase64 = imageBase64,
        )

        var idCounter = 1L
        whenever(plantRepository.persist(any())).thenAnswer { inv ->
            inv.getArgument<Plant>(0).copy(id = idCounter++)
        }
        val capturedEvents = mutableListOf<PlantEvent>()
        whenever(plantEventRepository.persist(any())).thenAnswer { inv ->
            val event = inv.getArgument<PlantEvent>(0)
            capturedEvents.add(event)
            event.copy(id = capturedEvents.size.toLong())
        }
        whenever(storageService.uploadImage(any(), any())).thenReturn(expectedUrl)

        service.batchSow(request, userId)

        // All events should have the same image URL
        capturedEvents.forEach { assertEquals(expectedUrl, it.imageUrl) }
    }

    @Test
    fun `batchSow throws BadRequestException when seedCount is zero`() {
        val request = BatchSowRequest(speciesId = speciesId, name = "Lily", seedCount = 0)
        assertThrows<BadRequestException> { service.batchSow(request, userId) }
    }

    @Test
    fun `batchSow throws BadRequestException when seedCount exceeds 10000`() {
        val request = BatchSowRequest(speciesId = speciesId, name = "Lily", seedCount = 10001)
        assertThrows<BadRequestException> { service.batchSow(request, userId) }
    }

    @Test
    fun `batchSow throws BadRequestException when name is blank`() {
        val request = BatchSowRequest(speciesId = speciesId, name = "   ", seedCount = 1)
        assertThrows<BadRequestException> { service.batchSow(request, userId) }
    }

    @Test
    fun `batchSow throws BadRequestException when name exceeds 255 characters`() {
        val request = BatchSowRequest(speciesId = speciesId, name = "A".repeat(256), seedCount = 1)
        assertThrows<BadRequestException> { service.batchSow(request, userId) }
    }

    @Test
    fun `batchSow checks bed ownership when bedId is provided`() {
        val bedId = 7L
        val gardenId = 3L
        val request = BatchSowRequest(speciesId = speciesId, name = "Marigold", seedCount = 1, bedId = bedId)

        whenever(bedRepository.findById(bedId)).thenReturn(Bed(id = bedId, name = "Bed A", gardenId = gardenId))
        whenever(gardenRepository.findById(gardenId)).thenReturn(
            Garden(id = gardenId, name = "Garden A", ownerId = userId)
        )
        var idCounter = 1L
        whenever(plantRepository.persist(any())).thenAnswer { inv ->
            inv.getArgument<Plant>(0).copy(id = idCounter++)
        }
        whenever(plantEventRepository.persist(any())).thenAnswer { inv ->
            inv.getArgument<PlantEvent>(0).copy(id = 99L)
        }

        service.batchSow(request, userId)

        verify(bedRepository).findById(bedId)
        verify(gardenRepository).findById(gardenId)
    }

    @Test
    fun `batchSow throws ForbiddenException when bed belongs to another user`() {
        val bedId = 7L
        val gardenId = 3L
        val otherUserId = 999L
        val request = BatchSowRequest(speciesId = speciesId, name = "Marigold", seedCount = 1, bedId = bedId)

        whenever(bedRepository.findById(bedId)).thenReturn(Bed(id = bedId, name = "Bed A", gardenId = gardenId))
        whenever(gardenRepository.findById(gardenId)).thenReturn(
            Garden(id = gardenId, name = "Garden A", ownerId = otherUserId)
        )

        assertThrows<ForbiddenException> { service.batchSow(request, userId) }
        verify(plantRepository, never()).persist(any())
    }

    // ── addEvent status transitions ──

    @Test
    fun `addEvent SEEDED sets plant status to SEEDED`() {
        val plant = makePlant(1L, status = PlantStatus.POTTED_UP)
        whenever(plantRepository.findById(1L)).thenReturn(plant)
        val event = makePersistedEvent(100L, 1L, PlantEventType.SEEDED)
        whenever(plantEventRepository.persist(any())).thenReturn(event)

        val request = CreatePlantEventRequest(
            eventType = PlantEventType.SEEDED,
            eventDate = LocalDate.now(),
            plantCount = 5,
        )
        service.addEvent(1L, request, userId)

        val captor = argumentCaptor<Plant>()
        verify(plantRepository).update(captor.capture())
        assertEquals(PlantStatus.SEEDED, captor.firstValue.status)
    }

    @Test
    fun `addEvent POTTED_UP sets plant status to POTTED_UP`() {
        val plant = makePlant(1L, status = PlantStatus.SEEDED)
        whenever(plantRepository.findById(1L)).thenReturn(plant)
        val event = makePersistedEvent(100L, 1L, PlantEventType.POTTED_UP)
        whenever(plantEventRepository.persist(any())).thenReturn(event)

        val request = CreatePlantEventRequest(
            eventType = PlantEventType.POTTED_UP,
            eventDate = LocalDate.now(),
        )
        service.addEvent(1L, request, userId)

        val captor = argumentCaptor<Plant>()
        verify(plantRepository).update(captor.capture())
        assertEquals(PlantStatus.POTTED_UP, captor.firstValue.status)
    }

    @Test
    fun `addEvent PLANTED_OUT sets plant status to GROWING`() {
        val plant = makePlant(1L, status = PlantStatus.SEEDED)
        whenever(plantRepository.findById(1L)).thenReturn(plant)
        val event = makePersistedEvent(100L, 1L, PlantEventType.PLANTED_OUT)
        whenever(plantEventRepository.persist(any())).thenReturn(event)

        val request = CreatePlantEventRequest(
            eventType = PlantEventType.PLANTED_OUT,
            eventDate = LocalDate.now(),
        )
        service.addEvent(1L, request, userId)

        val captor = argumentCaptor<Plant>()
        verify(plantRepository).update(captor.capture())
        assertEquals(PlantStatus.GROWING, captor.firstValue.status)
    }

    @Test
    fun `addEvent HARVESTED sets plant status to HARVESTED`() {
        val plant = makePlant(1L, status = PlantStatus.GROWING)
        whenever(plantRepository.findById(1L)).thenReturn(plant)
        val event = makePersistedEvent(100L, 1L, PlantEventType.HARVESTED)
        whenever(plantEventRepository.persist(any())).thenReturn(event)

        val request = CreatePlantEventRequest(
            eventType = PlantEventType.HARVESTED,
            eventDate = LocalDate.now(),
        )
        service.addEvent(1L, request, userId)

        val captor = argumentCaptor<Plant>()
        verify(plantRepository).update(captor.capture())
        assertEquals(PlantStatus.HARVESTED, captor.firstValue.status)
    }

    @Test
    fun `addEvent RECOVERED sets plant status to RECOVERED`() {
        val plant = makePlant(1L, status = PlantStatus.GROWING)
        whenever(plantRepository.findById(1L)).thenReturn(plant)
        val event = makePersistedEvent(100L, 1L, PlantEventType.RECOVERED)
        whenever(plantEventRepository.persist(any())).thenReturn(event)

        val request = CreatePlantEventRequest(
            eventType = PlantEventType.RECOVERED,
            eventDate = LocalDate.now(),
            plantCount = 3,
        )
        service.addEvent(1L, request, userId)

        val captor = argumentCaptor<Plant>()
        verify(plantRepository).update(captor.capture())
        assertEquals(PlantStatus.RECOVERED, captor.firstValue.status)
    }

    @Test
    fun `addEvent REMOVED sets plant status to REMOVED`() {
        val plant = makePlant(1L, status = PlantStatus.GROWING)
        whenever(plantRepository.findById(1L)).thenReturn(plant)
        val event = makePersistedEvent(100L, 1L, PlantEventType.REMOVED)
        whenever(plantEventRepository.persist(any())).thenReturn(event)

        val request = CreatePlantEventRequest(
            eventType = PlantEventType.REMOVED,
            eventDate = LocalDate.now(),
        )
        service.addEvent(1L, request, userId)

        val captor = argumentCaptor<Plant>()
        verify(plantRepository).update(captor.capture())
        assertEquals(PlantStatus.REMOVED, captor.firstValue.status)
    }

    @Test
    fun `addEvent LIFTED sets plant status to DORMANT`() {
        val plant = makePlant(1L, status = PlantStatus.GROWING)
        whenever(plantRepository.findById(1L)).thenReturn(plant)
        val event = makePersistedEvent(100L, 1L, PlantEventType.LIFTED)
        whenever(plantEventRepository.persist(any())).thenReturn(event)

        val request = CreatePlantEventRequest(
            eventType = PlantEventType.LIFTED,
            eventDate = LocalDate.now(),
        )
        service.addEvent(1L, request, userId)

        val captor = argumentCaptor<Plant>()
        verify(plantRepository).update(captor.capture())
        assertEquals(PlantStatus.DORMANT, captor.firstValue.status)
    }

    @Test
    fun `addEvent STORED sets plant status to DORMANT`() {
        val plant = makePlant(1L, status = PlantStatus.GROWING)
        whenever(plantRepository.findById(1L)).thenReturn(plant)
        val event = makePersistedEvent(100L, 1L, PlantEventType.STORED)
        whenever(plantEventRepository.persist(any())).thenReturn(event)

        val request = CreatePlantEventRequest(
            eventType = PlantEventType.STORED,
            eventDate = LocalDate.now(),
        )
        service.addEvent(1L, request, userId)

        val captor = argumentCaptor<Plant>()
        verify(plantRepository).update(captor.capture())
        assertEquals(PlantStatus.DORMANT, captor.firstValue.status)
    }

    @Test
    fun `addEvent NOTE does not update plant status`() {
        val plant = makePlant(1L, status = PlantStatus.GROWING)
        whenever(plantRepository.findById(1L)).thenReturn(plant)
        val event = makePersistedEvent(100L, 1L, PlantEventType.NOTE)
        whenever(plantEventRepository.persist(any())).thenReturn(event)

        val request = CreatePlantEventRequest(
            eventType = PlantEventType.NOTE,
            eventDate = LocalDate.now(),
            notes = "Looking healthy",
        )
        service.addEvent(1L, request, userId)

        verify(plantRepository, never()).update(any())
    }

    @Test
    fun `addEvent BUDDING does not update plant status`() {
        val plant = makePlant(1L, status = PlantStatus.GROWING)
        whenever(plantRepository.findById(1L)).thenReturn(plant)
        val event = makePersistedEvent(100L, 1L, PlantEventType.BUDDING)
        whenever(plantEventRepository.persist(any())).thenReturn(event)

        service.addEvent(
            1L,
            CreatePlantEventRequest(eventType = PlantEventType.BUDDING, eventDate = LocalDate.now()),
            userId,
        )

        verify(plantRepository, never()).update(any())
    }

    @Test
    fun `addEvent SEEDED updates surviving count from request plantCount`() {
        val plant = makePlant(1L, status = PlantStatus.POTTED_UP).copy(seedCount = 10, survivingCount = 10)
        whenever(plantRepository.findById(1L)).thenReturn(plant)
        val event = makePersistedEvent(100L, 1L, PlantEventType.SEEDED)
        whenever(plantEventRepository.persist(any())).thenReturn(event)

        val request = CreatePlantEventRequest(
            eventType = PlantEventType.SEEDED,
            eventDate = LocalDate.now(),
            plantCount = 7,
        )
        service.addEvent(1L, request, userId)

        val captor = argumentCaptor<Plant>()
        verify(plantRepository).update(captor.capture())
        assertEquals(7, captor.firstValue.seedCount)
        assertEquals(7, captor.firstValue.survivingCount)
    }

    @Test
    fun `addEvent POTTED_UP updates survivingCount from request plantCount`() {
        val plant = makePlant(1L, status = PlantStatus.SEEDED).copy(survivingCount = 10)
        whenever(plantRepository.findById(1L)).thenReturn(plant)
        val event = makePersistedEvent(100L, 1L, PlantEventType.POTTED_UP)
        whenever(plantEventRepository.persist(any())).thenReturn(event)

        val request = CreatePlantEventRequest(
            eventType = PlantEventType.POTTED_UP,
            eventDate = LocalDate.now(),
            plantCount = 6,
        )
        service.addEvent(1L, request, userId)

        val captor = argumentCaptor<Plant>()
        verify(plantRepository).update(captor.capture())
        assertEquals(6, captor.firstValue.survivingCount)
    }

    @Test
    fun `addEvent uploads image and updates event imageUrl`() {
        val plant = makePlant(1L)
        whenever(plantRepository.findById(1L)).thenReturn(plant)
        val persistedEvent = makePersistedEvent(55L, 1L, PlantEventType.NOTE)
        whenever(plantEventRepository.persist(any())).thenReturn(persistedEvent)
        val expectedUrl = "https://storage.example.com/events/55.jpg"
        whenever(storageService.uploadEventPhoto(55L, "base64img")).thenReturn(expectedUrl)

        val request = CreatePlantEventRequest(
            eventType = PlantEventType.NOTE,
            eventDate = LocalDate.now(),
            imageBase64 = "base64img",
        )
        val response = service.addEvent(1L, request, userId)

        verify(storageService).uploadEventPhoto(55L, "base64img")
        verify(plantEventRepository).updateImageUrl(55L, expectedUrl)
        assertEquals(expectedUrl, response.imageUrl)
    }

    @Test
    fun `addEvent throws NotFoundException when plant does not exist`() {
        whenever(plantRepository.findById(999L)).thenReturn(null)

        val request = CreatePlantEventRequest(eventType = PlantEventType.NOTE, eventDate = LocalDate.now())

        assertThrows<NotFoundException> { service.addEvent(999L, request, userId) }
    }

    @Test
    fun `addEvent throws ForbiddenException when plant belongs to another user`() {
        val plant = makePlant(1L).copy(userId = 999L)
        whenever(plantRepository.findById(1L)).thenReturn(plant)

        val request = CreatePlantEventRequest(eventType = PlantEventType.NOTE, eventDate = LocalDate.now())

        assertThrows<ForbiddenException> { service.addEvent(1L, request, userId) }
    }

    // ── batchEvent ──

    @Test
    fun `batchEvent updates status of all matched plants`() {
        val plants = listOf(
            makePlant(1L, "Zinnia #1", PlantStatus.SEEDED),
            makePlant(2L, "Zinnia #2", PlantStatus.SEEDED),
            makePlant(3L, "Zinnia #3", PlantStatus.SEEDED),
        )
        val request = BatchEventRequest(
            speciesId = speciesId,
            status = "SEEDED",
            eventType = "POTTED_UP",
            count = 3,
        )
        whenever(plantRepository.findByGroup(userId, speciesId, null, null, PlantStatus.SEEDED, 3))
            .thenReturn(plants)
        whenever(plantEventRepository.persist(any())).thenAnswer { inv ->
            inv.getArgument<PlantEvent>(0).copy(id = 999L)
        }

        val response = service.batchEvent(request, userId)

        assertEquals(3, response.updatedCount)
        verify(plantRepository, times(3)).update(any())
    }

    @Test
    fun `batchEvent sets POTTED_UP status on all updated plants`() {
        val plants = listOf(
            makePlant(1L, status = PlantStatus.SEEDED),
            makePlant(2L, status = PlantStatus.SEEDED),
        )
        val request = BatchEventRequest(
            speciesId = speciesId,
            status = "SEEDED",
            eventType = "POTTED_UP",
            count = 2,
        )
        whenever(plantRepository.findByGroup(userId, speciesId, null, null, PlantStatus.SEEDED, 2))
            .thenReturn(plants)
        whenever(plantEventRepository.persist(any())).thenAnswer { inv ->
            inv.getArgument<PlantEvent>(0).copy(id = 999L)
        }

        service.batchEvent(request, userId)

        val captor = argumentCaptor<Plant>()
        verify(plantRepository, times(2)).update(captor.capture())
        captor.allValues.forEach { assertEquals(PlantStatus.POTTED_UP, it.status) }
    }

    @Test
    fun `batchEvent sets PLANTED_OUT status on all updated plants`() {
        val plants = listOf(makePlant(1L, status = PlantStatus.POTTED_UP))
        val request = BatchEventRequest(
            speciesId = speciesId,
            status = "POTTED_UP",
            eventType = "PLANTED_OUT",
            count = 1,
        )
        whenever(plantRepository.findByGroup(userId, speciesId, null, null, PlantStatus.POTTED_UP, 1))
            .thenReturn(plants)
        whenever(plantEventRepository.persist(any())).thenAnswer { inv ->
            inv.getArgument<PlantEvent>(0).copy(id = 999L)
        }

        service.batchEvent(request, userId)

        val captor = argumentCaptor<Plant>()
        verify(plantRepository).update(captor.capture())
        assertEquals(PlantStatus.PLANTED_OUT, captor.firstValue.status)
    }

    @Test
    fun `batchEvent persists one event per plant with correct eventType`() {
        val plants = listOf(
            makePlant(1L, status = PlantStatus.SEEDED),
            makePlant(2L, status = PlantStatus.SEEDED),
        )
        val request = BatchEventRequest(
            speciesId = speciesId,
            status = "SEEDED",
            eventType = "POTTED_UP",
            count = 2,
        )
        whenever(plantRepository.findByGroup(userId, speciesId, null, null, PlantStatus.SEEDED, 2))
            .thenReturn(plants)
        val capturedEvents = mutableListOf<PlantEvent>()
        whenever(plantEventRepository.persist(any())).thenAnswer { inv ->
            val event = inv.getArgument<PlantEvent>(0)
            capturedEvents.add(event)
            event.copy(id = capturedEvents.size.toLong())
        }

        service.batchEvent(request, userId)

        assertEquals(2, capturedEvents.size)
        capturedEvents.forEach { assertEquals(PlantEventType.POTTED_UP, it.eventType) }
    }

    @Test
    fun `batchEvent uploads image exactly once even with multiple plants`() {
        val imageBase64 = "base64img"
        val plants = listOf(
            makePlant(1L, status = PlantStatus.SEEDED),
            makePlant(2L, status = PlantStatus.SEEDED),
            makePlant(3L, status = PlantStatus.SEEDED),
        )
        val request = BatchEventRequest(
            speciesId = speciesId,
            status = "SEEDED",
            eventType = "POTTED_UP",
            count = 3,
            imageBase64 = imageBase64,
        )
        whenever(plantRepository.findByGroup(userId, speciesId, null, null, PlantStatus.SEEDED, 3))
            .thenReturn(plants)
        whenever(plantEventRepository.persist(any())).thenAnswer { inv ->
            inv.getArgument<PlantEvent>(0).copy(id = 999L)
        }
        whenever(storageService.uploadImage(any(), any())).thenReturn("https://storage.example.com/img.jpg")

        service.batchEvent(request, userId)

        verify(storageService, times(1)).uploadImage(eq(imageBase64), any())
    }

    @Test
    fun `batchEvent moves plants to targetBedId when provided`() {
        val targetBedId = 88L
        val plants = listOf(makePlant(1L, status = PlantStatus.SEEDED))
        val request = BatchEventRequest(
            speciesId = speciesId,
            status = "SEEDED",
            eventType = "POTTED_UP",
            count = 1,
            targetBedId = targetBedId,
        )
        whenever(plantRepository.findByGroup(userId, speciesId, null, null, PlantStatus.SEEDED, 1))
            .thenReturn(plants)
        whenever(plantEventRepository.persist(any())).thenAnswer { inv ->
            inv.getArgument<PlantEvent>(0).copy(id = 999L)
        }

        service.batchEvent(request, userId)

        val captor = argumentCaptor<Plant>()
        verify(plantRepository).update(captor.capture())
        assertEquals(targetBedId, captor.firstValue.bedId)
    }

    @Test
    fun `batchEvent returns zero updatedCount when no plants are matched`() {
        val request = BatchEventRequest(
            speciesId = speciesId,
            status = "SEEDED",
            eventType = "POTTED_UP",
            count = 5,
        )
        whenever(plantRepository.findByGroup(userId, speciesId, null, null, PlantStatus.SEEDED, 5))
            .thenReturn(emptyList())

        val response = service.batchEvent(request, userId)

        assertEquals(0, response.updatedCount)
        verify(plantRepository, never()).update(any())
        verify(plantEventRepository, never()).persist(any())
    }

    @Test
    fun `batchEvent with invalid eventType throws IllegalArgumentException`() {
        val request = BatchEventRequest(
            speciesId = speciesId,
            status = "SEEDED",
            eventType = "INVALID_TYPE",
            count = 1,
        )
        whenever(plantRepository.findByGroup(any(), any(), anyOrNull(), anyOrNull(), any(), any()))
            .thenReturn(listOf(makePlant(1L)))
        whenever(plantEventRepository.persist(any())).thenAnswer { inv ->
            inv.getArgument<PlantEvent>(0).copy(id = 999L)
        }

        assertThrows<IllegalArgumentException> { service.batchEvent(request, userId) }
    }

    @Test
    fun `batchEvent with invalid status throws IllegalArgumentException`() {
        val request = BatchEventRequest(
            speciesId = speciesId,
            status = "INVALID_STATUS",
            eventType = "POTTED_UP",
            count = 1,
        )

        assertThrows<IllegalArgumentException> { service.batchEvent(request, userId) }
    }

    @Test
    fun `batchEvent passes plantedDate filter to repository when provided`() {
        val plantedDateStr = "2025-03-15"
        val plantedDate = LocalDate.parse(plantedDateStr)
        val request = BatchEventRequest(
            speciesId = speciesId,
            status = "SEEDED",
            eventType = "POTTED_UP",
            count = 2,
            plantedDate = plantedDateStr,
        )
        whenever(plantRepository.findByGroup(userId, speciesId, null, plantedDate, PlantStatus.SEEDED, 2))
            .thenReturn(emptyList())

        service.batchEvent(request, userId)

        verify(plantRepository).findByGroup(userId, speciesId, null, plantedDate, PlantStatus.SEEDED, 2)
    }
}
