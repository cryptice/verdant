package app.verdant.service

import app.verdant.dto.CreateSupplyApplicationRequest
import app.verdant.entity.Bed
import app.verdant.entity.Garden
import app.verdant.entity.Plant
import app.verdant.entity.PlantStatus
import app.verdant.entity.SpeciesWorkflowStep
import app.verdant.entity.SupplyApplication
import app.verdant.entity.SupplyApplicationScope
import app.verdant.entity.SupplyInventory
import app.verdant.entity.SupplyType
import app.verdant.entity.SupplyCategory
import app.verdant.entity.SupplyUnit
import app.verdant.repository.BedRepository
import app.verdant.repository.GardenRepository
import app.verdant.repository.PlantEventRepository
import app.verdant.repository.PlantRepository
import app.verdant.repository.SupplyApplicationRepository
import app.verdant.repository.SupplyInventoryRepository
import app.verdant.repository.SupplyTypeRepository
import app.verdant.repository.UserRepository
import app.verdant.repository.WorkflowRepository
import jakarta.ws.rs.BadRequestException
import jakarta.ws.rs.NotFoundException
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal

class SupplyApplicationServiceTest {

    private val applicationRepo: SupplyApplicationRepository = mock()
    private val inventoryRepo: SupplyInventoryRepository = mock()
    private val supplyTypeRepo: SupplyTypeRepository = mock()
    private val plantRepo: PlantRepository = mock()
    private val plantEventRepo: PlantEventRepository = mock()
    private val workflowRepo: WorkflowRepository = mock()
    private val userRepo: UserRepository = mock()
    private val bedRepo: BedRepository = mock()
    private val gardenRepo: GardenRepository = mock()

    private val service = SupplyApplicationService(
        applicationRepo, inventoryRepo, supplyTypeRepo, plantRepo,
        plantEventRepo, workflowRepo, userRepo, bedRepo, gardenRepo,
    )

    private val orgId = 1L
    private val userId = 10L
    private val bedId = 42L
    private val gardenId = 5L
    private val inventoryId = 7L
    private val supplyTypeId = 3L

    private fun defaultBed() = Bed(id = bedId, name = "Bed A", gardenId = gardenId)
    private fun defaultGarden() = Garden(id = gardenId, name = "Garden A", orgId = orgId)
    private fun defaultInventory(quantity: BigDecimal = BigDecimal("100")) = SupplyInventory(
        id = inventoryId, orgId = orgId, supplyTypeId = supplyTypeId, quantity = quantity
    )
    private fun defaultSupplyType() = SupplyType(
        id = supplyTypeId, orgId = orgId, name = "Fertilizer A",
        category = SupplyCategory.FERTILIZER, unit = SupplyUnit.KILOGRAMS,
    )
    private fun defaultPersistedApp() = SupplyApplication(
        id = 99L, orgId = orgId, bedId = bedId,
        supplyInventoryId = inventoryId, supplyTypeId = supplyTypeId,
        quantity = BigDecimal("60"), targetScope = SupplyApplicationScope.PLANTS,
        appliedBy = userId,
    )
    private fun defaultBedRequest(
        quantity: BigDecimal = BigDecimal("10"),
        plantIds: List<Long> = emptyList(),
        workflowStepId: Long? = null,
    ) = CreateSupplyApplicationRequest(
        bedId = bedId,
        supplyInventoryId = inventoryId,
        quantity = quantity,
        targetScope = "BED",
        plantIds = plantIds,
        workflowStepId = workflowStepId,
    )
    private fun defaultPlantsRequest(
        quantity: BigDecimal = BigDecimal("60"),
        plantIds: List<Long> = listOf(101L, 102L),
        workflowStepId: Long? = null,
    ) = CreateSupplyApplicationRequest(
        bedId = bedId,
        supplyInventoryId = inventoryId,
        quantity = quantity,
        targetScope = "PLANTS",
        plantIds = plantIds,
        workflowStepId = workflowStepId,
    )

    @Test
    fun `rejects quantity greater than lot quantity`() {
        whenever(bedRepo.findById(bedId)).thenReturn(defaultBed())
        whenever(gardenRepo.findById(gardenId)).thenReturn(defaultGarden())
        whenever(inventoryRepo.findById(inventoryId)).thenReturn(defaultInventory(BigDecimal("10")))

        assertThrows(BadRequestException::class.java) {
            service.create(defaultBedRequest(quantity = BigDecimal("50")), orgId, userId)
        }

        verify(inventoryRepo, never()).decrementQuantity(any(), any())
        verify(applicationRepo, never()).insert(any())
    }

    @Test
    fun `rejects cross-org bed`() {
        val otherOrgGarden = Garden(id = gardenId, name = "Other Garden", orgId = 999L)
        whenever(bedRepo.findById(bedId)).thenReturn(defaultBed())
        whenever(gardenRepo.findById(gardenId)).thenReturn(otherOrgGarden)

        assertThrows(NotFoundException::class.java) {
            service.create(defaultBedRequest(), orgId, userId)
        }

        verify(applicationRepo, never()).insert(any())
    }

    @Test
    fun `rejects cross-org supply inventory`() {
        val otherOrgInventory = defaultInventory().copy(orgId = 999L)
        whenever(bedRepo.findById(bedId)).thenReturn(defaultBed())
        whenever(gardenRepo.findById(gardenId)).thenReturn(defaultGarden())
        whenever(inventoryRepo.findById(inventoryId)).thenReturn(otherOrgInventory)

        assertThrows(NotFoundException::class.java) {
            service.create(defaultBedRequest(), orgId, userId)
        }

        verify(applicationRepo, never()).insert(any())
    }

    @Test
    fun `rejects PLANTS scope with empty plantIds`() {
        assertThrows(BadRequestException::class.java) {
            service.create(defaultPlantsRequest(plantIds = emptyList()), orgId, userId)
        }

        verify(bedRepo, never()).findById(any())
        verify(applicationRepo, never()).insert(any())
    }

    @Test
    fun `rejects BED scope with non-empty plantIds`() {
        assertThrows(BadRequestException::class.java) {
            service.create(defaultBedRequest(plantIds = listOf(101L)), orgId, userId)
        }

        verify(bedRepo, never()).findById(any())
        verify(applicationRepo, never()).insert(any())
    }

    @Test
    fun `rejects plant not in bed`() {
        val plantInBed = Plant(id = 101L, name = "Plant 1", orgId = orgId, bedId = bedId, status = PlantStatus.GROWING)
        val plantInOtherBed = Plant(id = 102L, name = "Plant 2", orgId = orgId, bedId = 999L, status = PlantStatus.GROWING)

        whenever(bedRepo.findById(bedId)).thenReturn(defaultBed())
        whenever(gardenRepo.findById(gardenId)).thenReturn(defaultGarden())
        whenever(inventoryRepo.findById(inventoryId)).thenReturn(defaultInventory())
        whenever(plantRepo.findByIds(listOf(101L, 102L))).thenReturn(listOf(plantInBed, plantInOtherBed))

        assertThrows(BadRequestException::class.java) {
            service.create(defaultPlantsRequest(plantIds = listOf(101L, 102L)), orgId, userId)
        }

        verify(applicationRepo, never()).insert(any())
    }

    @Test
    fun `rejects plant with REMOVED status`() {
        val removedPlant = Plant(id = 101L, name = "Plant 1", orgId = orgId, bedId = bedId, status = PlantStatus.REMOVED)
        val goodPlant = Plant(id = 102L, name = "Plant 2", orgId = orgId, bedId = bedId, status = PlantStatus.GROWING)

        whenever(bedRepo.findById(bedId)).thenReturn(defaultBed())
        whenever(gardenRepo.findById(gardenId)).thenReturn(defaultGarden())
        whenever(inventoryRepo.findById(inventoryId)).thenReturn(defaultInventory())
        whenever(plantRepo.findByIds(listOf(101L, 102L))).thenReturn(listOf(removedPlant, goodPlant))

        assertThrows(BadRequestException::class.java) {
            service.create(defaultPlantsRequest(plantIds = listOf(101L, 102L)), orgId, userId)
        }

        verify(applicationRepo, never()).insert(any())
    }

    @Test
    fun `rejects stepId with BED scope`() {
        assertThrows(BadRequestException::class.java) {
            service.create(defaultBedRequest(workflowStepId = 88L), orgId, userId)
        }

        verify(bedRepo, never()).findById(any())
        verify(applicationRepo, never()).insert(any())
    }

    @Test
    fun `happy path PLANTS inserts app events progress and decrements`() {
        val plant1 = Plant(id = 101L, name = "Plant 1", orgId = orgId, bedId = bedId, status = PlantStatus.GROWING, speciesId = 55L)
        val plant2 = Plant(id = 102L, name = "Plant 2", orgId = orgId, bedId = bedId, status = PlantStatus.GROWING, speciesId = 55L)
        val step = SpeciesWorkflowStep(id = 88L, speciesId = 55L, name = "Fertilize")

        whenever(bedRepo.findById(bedId)).thenReturn(defaultBed())
        whenever(gardenRepo.findById(gardenId)).thenReturn(defaultGarden())
        whenever(inventoryRepo.findById(inventoryId)).thenReturn(defaultInventory(BigDecimal("100")))
        whenever(plantRepo.findByIds(listOf(101L, 102L))).thenReturn(listOf(plant1, plant2))
        whenever(workflowRepo.findSpeciesStepById(88L)).thenReturn(step)
        whenever(applicationRepo.insert(any())).thenReturn(defaultPersistedApp())
        whenever(supplyTypeRepo.findById(supplyTypeId)).thenReturn(defaultSupplyType())
        whenever(userRepo.findById(userId)).thenReturn(null)
        whenever(applicationRepo.findPlantIdsForApplication(any())).thenReturn(listOf(101L, 102L))

        service.create(defaultPlantsRequest(quantity = BigDecimal("60"), plantIds = listOf(101L, 102L), workflowStepId = 88L), orgId, userId)

        verify(inventoryRepo).decrementQuantity(eq(inventoryId.toLong()), eq(BigDecimal("60")))
        verify(applicationRepo).insert(any())
        verify(plantEventRepo, times(2)).persist(any())
        verify(workflowRepo, times(2)).recordProgress(any(), eq(88L))
    }

    @Test
    fun `happy path BED inserts app and decrements zero plant events`() {
        val persistedBedApp = SupplyApplication(
            id = 100L, orgId = orgId, bedId = bedId,
            supplyInventoryId = inventoryId, supplyTypeId = supplyTypeId,
            quantity = BigDecimal("10"), targetScope = SupplyApplicationScope.BED,
            appliedBy = userId,
        )

        whenever(bedRepo.findById(bedId)).thenReturn(defaultBed())
        whenever(gardenRepo.findById(gardenId)).thenReturn(defaultGarden())
        whenever(inventoryRepo.findById(inventoryId)).thenReturn(defaultInventory(BigDecimal("100")))
        whenever(applicationRepo.insert(any())).thenReturn(persistedBedApp)
        whenever(supplyTypeRepo.findById(supplyTypeId)).thenReturn(defaultSupplyType())
        whenever(userRepo.findById(userId)).thenReturn(null)
        whenever(applicationRepo.findPlantIdsForApplication(any())).thenReturn(emptyList())

        service.create(defaultBedRequest(quantity = BigDecimal("10")), orgId, userId)

        verify(inventoryRepo).decrementQuantity(eq(inventoryId.toLong()), eq(BigDecimal("10")))
        verify(applicationRepo).insert(any())
        verify(plantEventRepo, never()).persist(any())
        verify(workflowRepo, never()).recordProgress(any(), any())
    }
}
