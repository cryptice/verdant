package app.verdant.service

import app.verdant.dto.CreateSupplyApplicationRequest
import app.verdant.dto.SupplyApplicationResponse
import app.verdant.entity.PlantEvent
import app.verdant.entity.PlantEventType
import app.verdant.entity.PlantStatus
import app.verdant.entity.SupplyApplication
import app.verdant.entity.SupplyApplicationScope
import app.verdant.repository.BedRepository
import app.verdant.repository.GardenRepository
import app.verdant.repository.PlantEventRepository
import app.verdant.repository.PlantRepository
import app.verdant.repository.SupplyApplicationRepository
import app.verdant.repository.SupplyInventoryRepository
import app.verdant.repository.SupplyTypeRepository
import app.verdant.repository.UserRepository
import app.verdant.repository.WorkflowRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import jakarta.ws.rs.BadRequestException
import jakarta.ws.rs.NotFoundException
import java.math.BigDecimal
import java.time.ZoneId

@ApplicationScoped
class SupplyApplicationService(
    private val applicationRepo: SupplyApplicationRepository,
    private val inventoryRepo: SupplyInventoryRepository,
    private val supplyTypeRepo: SupplyTypeRepository,
    private val plantRepo: PlantRepository,
    private val plantEventRepo: PlantEventRepository,
    private val workflowRepo: WorkflowRepository,
    private val userRepo: UserRepository,
    private val bedRepo: BedRepository,
    private val gardenRepo: GardenRepository,
) {
    @Transactional
    fun create(request: CreateSupplyApplicationRequest, orgId: Long, userId: Long): SupplyApplicationResponse {
        // 1. Parse & basic validation
        val scope = try {
            SupplyApplicationScope.valueOf(request.targetScope)
        } catch (e: IllegalArgumentException) {
            throw BadRequestException("Invalid targetScope: ${request.targetScope}")
        }
        if (request.quantity <= BigDecimal.ZERO) throw BadRequestException("quantity must be > 0")

        when (scope) {
            SupplyApplicationScope.BED -> {
                if (request.plantIds.isNotEmpty()) throw BadRequestException("plantIds must be empty for BED scope")
                if (request.workflowStepId != null) throw BadRequestException("workflowStepId not allowed for BED scope")
            }
            SupplyApplicationScope.PLANTS -> {
                if (request.plantIds.isEmpty()) throw BadRequestException("plantIds required for PLANTS scope")
            }
        }

        // 2. Authorization chain — fail 404 on any cross-org mismatch
        val bed = bedRepo.findById(request.bedId) ?: throw NotFoundException("Bed not found")
        val garden = gardenRepo.findById(bed.gardenId) ?: throw NotFoundException("Bed not found")
        if (garden.orgId != orgId) throw NotFoundException("Bed not found")

        val inventory = inventoryRepo.findById(request.supplyInventoryId)
            ?: throw NotFoundException("Supply inventory not found")
        if (inventory.orgId != orgId) throw NotFoundException("Supply inventory not found")

        // 3. Plant validation (PLANTS scope)
        val targetPlants = if (scope == SupplyApplicationScope.PLANTS) {
            val plants = plantRepo.findByIds(request.plantIds)
            if (plants.size != request.plantIds.size) throw BadRequestException("One or more plants not found")
            plants.forEach { plant ->
                if (plant.bedId != request.bedId) throw BadRequestException("Plant ${plant.id} not in bed ${request.bedId}")
                if (plant.status == PlantStatus.REMOVED) throw BadRequestException("Plant ${plant.id} is REMOVED")
            }
            plants
        } else emptyList()

        // 4. Workflow step validation
        if (request.workflowStepId != null) {
            val step = workflowRepo.findSpeciesStepById(request.workflowStepId)
                ?: throw NotFoundException("Workflow step not found")
            val speciesIds = targetPlants.mapNotNull { it.speciesId }.toSet()
            if (step.speciesId !in speciesIds)
                throw BadRequestException("Workflow step does not match any target plant's species")
        }

        // 5. Decrement inventory
        if (request.quantity > inventory.quantity)
            throw BadRequestException("Insufficient quantity")
        inventoryRepo.decrementQuantity(inventory.id!!, request.quantity)

        // 6. Insert the application
        val persisted = applicationRepo.insert(
            SupplyApplication(
                orgId = orgId,
                bedId = request.bedId,
                supplyInventoryId = inventory.id,
                supplyTypeId = inventory.supplyTypeId,
                quantity = request.quantity,
                targetScope = scope,
                appliedBy = userId,
                workflowStepId = request.workflowStepId,
                notes = request.notes,
            )
        )

        // 7. Fan out plant events + workflow progress
        if (scope == SupplyApplicationScope.PLANTS) {
            for (plant in targetPlants) {
                plantEventRepo.persist(
                    PlantEvent(
                        plantId = plant.id!!,
                        eventType = PlantEventType.APPLIED_SUPPLY,
                        eventDate = persisted.appliedAt.atZone(ZoneId.systemDefault()).toLocalDate(),
                        notes = request.notes,
                        supplyApplicationId = persisted.id,
                    )
                )
                if (request.workflowStepId != null) {
                    workflowRepo.recordProgress(plant.id, request.workflowStepId)
                }
            }
        }

        return toResponse(persisted, targetPlants.mapNotNull { it.id })
    }

    fun findByBed(bedId: Long, orgId: Long, limit: Int = 20): List<SupplyApplicationResponse> {
        val bed = bedRepo.findById(bedId) ?: throw NotFoundException("Bed not found")
        val garden = gardenRepo.findById(bed.gardenId) ?: throw NotFoundException("Bed not found")
        if (garden.orgId != orgId) throw NotFoundException("Bed not found")
        return applicationRepo.findByBed(bedId, limit).map {
            toResponse(it, applicationRepo.findPlantIdsForApplication(it.id!!))
        }
    }

    fun findByGarden(gardenId: Long, orgId: Long, limit: Int = 20): List<SupplyApplicationResponse> {
        val garden = gardenRepo.findById(gardenId) ?: throw NotFoundException("Garden not found")
        if (garden.orgId != orgId) throw NotFoundException("Garden not found")
        return applicationRepo.findByGarden(gardenId, limit).map {
            toResponse(it, applicationRepo.findPlantIdsForApplication(it.id!!))
        }
    }

    fun findById(id: Long, orgId: Long): SupplyApplicationResponse {
        val app = applicationRepo.findById(id) ?: throw NotFoundException("Application not found")
        if (app.orgId != orgId) throw NotFoundException("Application not found")
        return toResponse(app, applicationRepo.findPlantIdsForApplication(app.id!!))
    }

    private fun toResponse(app: SupplyApplication, plantIds: List<Long>): SupplyApplicationResponse {
        val type = supplyTypeRepo.findById(app.supplyTypeId)!!
        val user = userRepo.findById(app.appliedBy)
        return SupplyApplicationResponse(
            id = app.id!!,
            bedId = app.bedId,
            supplyInventoryId = app.supplyInventoryId,
            supplyTypeId = app.supplyTypeId,
            supplyTypeName = type.name,
            supplyUnit = type.unit.name,
            quantity = app.quantity,
            targetScope = app.targetScope.name,
            appliedAt = app.appliedAt,
            appliedByName = user?.displayName,
            workflowStepId = app.workflowStepId,
            notes = app.notes,
            plantIds = plantIds,
        )
    }
}
