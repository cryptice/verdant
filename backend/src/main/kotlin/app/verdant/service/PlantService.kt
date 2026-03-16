package app.verdant.service

import app.verdant.dto.*
import app.verdant.entity.Plant
import app.verdant.entity.PlantEvent
import app.verdant.entity.PlantEventType
import app.verdant.entity.PlantStatus
import app.verdant.repository.*
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.ForbiddenException
import jakarta.ws.rs.NotFoundException

@ApplicationScoped
class PlantService(
    private val plantRepository: PlantRepository,
    private val plantEventRepository: PlantEventRepository,
    private val bedRepository: BedRepository,
    private val gardenRepository: GardenRepository,
    private val speciesRepository: SpeciesRepository,
    private val storageService: StorageService,
) {
    private fun checkBedOwnership(bedId: Long, userId: Long) {
        val bed = bedRepository.findById(bedId) ?: throw NotFoundException("Bed not found")
        val garden = gardenRepository.findById(bed.gardenId) ?: throw NotFoundException("Garden not found")
        if (garden.ownerId != userId) throw ForbiddenException()
    }

    private fun checkPlantOwnership(plantId: Long, userId: Long): Plant {
        val plant = plantRepository.findById(plantId) ?: throw NotFoundException("Plant not found")
        if (plant.userId != userId) throw ForbiddenException()
        return plant
    }

    private fun resolveSpeciesName(speciesId: Long?): String? =
        speciesId?.let { speciesRepository.findById(it)?.commonName }

    // ── Plant CRUD ──

    fun getAllPlantsForUser(userId: Long, status: PlantStatus? = null): List<PlantResponse> {
        return plantRepository.findByUserId(userId, status).map { it.toResponse() }
    }

    fun getPlantsForBed(bedId: Long, userId: Long): List<PlantResponse> {
        checkBedOwnership(bedId, userId)
        return plantRepository.findByBedId(bedId).map { it.toResponse() }
    }

    fun getPlant(plantId: Long, userId: Long): PlantResponse {
        return checkPlantOwnership(plantId, userId).toResponse()
    }

    fun createPlant(bedId: Long?, request: CreatePlantRequest, userId: Long): PlantResponse {
        if (bedId != null) checkBedOwnership(bedId, userId)
        val plant = plantRepository.persist(
            Plant(
                name = request.name,
                speciesId = request.speciesId,
                plantedDate = request.plantedDate,
                status = request.status,
                seedCount = request.seedCount,
                survivingCount = request.survivingCount,
                bedId = bedId,
                userId = userId,
            )
        )
        return plant.toResponse()
    }

    fun updatePlant(plantId: Long, request: UpdatePlantRequest, userId: Long): PlantResponse {
        val plant = checkPlantOwnership(plantId, userId)
        val updated = plant.copy(
            name = request.name ?: plant.name,
            speciesId = request.speciesId ?: plant.speciesId,
            plantedDate = request.plantedDate ?: plant.plantedDate,
            status = request.status ?: plant.status,
            seedCount = request.seedCount ?: plant.seedCount,
            survivingCount = request.survivingCount ?: plant.survivingCount,
        )
        plantRepository.update(updated)
        return updated.toResponse()
    }

    fun deletePlant(plantId: Long, userId: Long) {
        checkPlantOwnership(plantId, userId)
        plantRepository.delete(plantId)
    }

    // ── Plant Events ──

    fun getEventsForPlant(plantId: Long, userId: Long): List<PlantEventResponse> {
        checkPlantOwnership(plantId, userId)
        return plantEventRepository.findByPlantId(plantId).map { it.toResponse() }
    }

    fun addEvent(plantId: Long, request: CreatePlantEventRequest, userId: Long): PlantEventResponse {
        val plant = checkPlantOwnership(plantId, userId)

        var event = plantEventRepository.persist(
            PlantEvent(
                plantId = plantId,
                eventType = request.eventType,
                eventDate = request.eventDate,
                plantCount = request.plantCount,
                weightGrams = request.weightGrams,
                quantity = request.quantity,
                notes = request.notes,
                aiSuggestions = request.aiSuggestions,
            )
        )
        // Upload event image to GCS if provided
        if (request.imageBase64 != null) {
            val eid = event.id!!
            val imageUrl = storageService.uploadEventPhoto(eid, request.imageBase64)
            event = event.copy(imageUrl = imageUrl)
            plantEventRepository.updateImageUrl(eid, imageUrl)
        }

        // Derive plant status from event
        val updatedPlant = when (request.eventType) {
            PlantEventType.SEEDED -> plant.copy(
                status = PlantStatus.SEEDED,
                seedCount = request.plantCount ?: plant.seedCount,
                survivingCount = request.plantCount ?: plant.survivingCount,
            )
            PlantEventType.POTTED_UP -> plant.copy(
                status = PlantStatus.POTTED_UP,
                survivingCount = request.plantCount ?: plant.survivingCount,
            )
            PlantEventType.PLANTED_OUT -> plant.copy(
                status = PlantStatus.GROWING,
                survivingCount = request.plantCount ?: plant.survivingCount,
            )
            PlantEventType.HARVESTED -> plant.copy(
                status = PlantStatus.HARVESTED,
            )
            PlantEventType.RECOVERED -> plant.copy(
                status = PlantStatus.RECOVERED,
                survivingCount = request.plantCount ?: plant.survivingCount,
            )
            PlantEventType.REMOVED -> plant.copy(
                status = PlantStatus.REMOVED,
            )
            PlantEventType.NOTE -> plant
        }

        if (updatedPlant !== plant) {
            plantRepository.update(updatedPlant)
        }

        return event.toResponse()
    }

    fun deleteEvent(plantId: Long, eventId: Long, userId: Long) {
        checkPlantOwnership(plantId, userId)
        val event = plantEventRepository.findById(eventId) ?: throw NotFoundException("Event not found")
        if (event.plantId != plantId) throw NotFoundException("Event not found")
        plantEventRepository.delete(eventId)
    }

    // ── Species Plant Summary ──

    fun getSpeciesPlantSummary(userId: Long) = plantRepository.speciesSummary(userId)

    fun getSpeciesLocations(userId: Long, speciesId: Long) = plantRepository.speciesLocations(userId, speciesId)

    // ── Stats ──

    fun getHarvestStats(userId: Long): List<HarvestStatRow> {
        return plantEventRepository.harvestStatsBySpecies(userId).map {
            HarvestStatRow(
                species = it.species,
                totalWeightGrams = it.totalWeightGrams,
                totalQuantity = it.totalQuantity,
                harvestCount = it.harvestCount,
            )
        }
    }

    // ── Mapping ──

    private fun Plant.toResponse() = PlantResponse(
        id = id!!, name = name, speciesId = speciesId,
        speciesName = resolveSpeciesName(speciesId),
        plantedDate = plantedDate, status = status,
        seedCount = seedCount, survivingCount = survivingCount,
        bedId = bedId, createdAt = createdAt, updatedAt = updatedAt
    )
}

fun PlantEvent.toResponse() = PlantEventResponse(
    id = id!!, plantId = plantId, eventType = eventType,
    eventDate = eventDate, plantCount = plantCount,
    weightGrams = weightGrams, quantity = quantity,
    notes = notes, imageUrl = imageUrl,
    aiSuggestions = aiSuggestions, createdAt = createdAt,
)
