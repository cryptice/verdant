package app.verdant.service

import app.verdant.dto.*
import app.verdant.entity.Plant
import app.verdant.entity.PlantEvent
import app.verdant.entity.PlantEventType
import app.verdant.entity.PlantStatus
import app.verdant.repository.*
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import jakarta.ws.rs.BadRequestException
import jakarta.ws.rs.NotFoundException

@ApplicationScoped
class PlantService(
    private val plantRepository: PlantRepository,
    private val plantEventRepository: PlantEventRepository,
    private val bedRepository: BedRepository,
    private val gardenRepository: GardenRepository,
    private val speciesRepository: SpeciesRepository,
    private val storageService: StorageService,
    private val workflowRepository: WorkflowRepository,
    private val trayLocationRepository: app.verdant.repository.TrayLocationRepository,
) {
    @Transactional
    fun weedBed(bedId: Long, orgId: Long): BulkLocationActionResponse =
        bulkBedEvent(bedId, orgId, PlantEventType.WEEDED)

    @Transactional
    fun waterBed(bedId: Long, orgId: Long): BulkLocationActionResponse =
        bulkBedEvent(bedId, orgId, PlantEventType.WATERED)

    private fun bulkBedEvent(bedId: Long, orgId: Long, eventType: PlantEventType): BulkLocationActionResponse {
        checkBedOwnership(bedId, orgId)
        val plants = plantRepository.findByBedId(bedId)
            .filter { it.status != PlantStatus.REMOVED }
        val today = java.time.LocalDate.now()
        plants.forEach { plant ->
            plantEventRepository.persist(
                PlantEvent(
                    plantId = plant.id!!,
                    eventType = eventType,
                    eventDate = today,
                    plantCount = 1,
                )
            )
        }
        return BulkLocationActionResponse(plantsAffected = plants.size)
    }

    @Transactional
    fun moveTrayPlants(orgId: Long, request: MoveTrayPlantsRequest): BulkLocationActionResponse {
        if (request.fromTrayLocationId == null && request.toTrayLocationId == null)
            throw BadRequestException("fromTrayLocationId and toTrayLocationId cannot both be null")
        request.fromTrayLocationId?.let { id ->
            val loc = trayLocationRepository.findById(id) ?: throw NotFoundException("Source location not found")
            if (loc.orgId != orgId) throw NotFoundException("Source location not found")
        }
        request.toTrayLocationId?.let { id ->
            val loc = trayLocationRepository.findById(id) ?: throw NotFoundException("Target location not found")
            if (loc.orgId != orgId) throw NotFoundException("Target location not found")
        }
        val plants = if (request.fromTrayLocationId != null) {
            plantRepository.findActiveByTrayLocationFiltered(
                orgId = orgId,
                locationId = request.fromTrayLocationId,
                speciesId = request.speciesId,
                status = request.status,
                limit = request.count,
            )
        } else {
            plantRepository.findActiveUnassignedTrayFiltered(
                orgId = orgId,
                speciesId = request.speciesId,
                status = request.status,
                limit = request.count,
            )
        }
        val today = java.time.LocalDate.now()
        plants.forEach { plant ->
            plantRepository.update(plant.copy(trayLocationId = request.toTrayLocationId))
            plantEventRepository.persist(
                PlantEvent(
                    plantId = plant.id!!,
                    eventType = PlantEventType.MOVED,
                    eventDate = today,
                    plantCount = 1,
                    fromTrayLocationId = request.fromTrayLocationId,
                    toTrayLocationId = request.toTrayLocationId,
                )
            )
        }
        return BulkLocationActionResponse(plantsAffected = plants.size)
    }

    private fun checkBedOwnership(bedId: Long, orgId: Long) {
        val bed = bedRepository.findById(bedId) ?: throw NotFoundException("Bed not found")
        val garden = gardenRepository.findById(bed.gardenId) ?: throw NotFoundException("Garden not found")
        if (garden.orgId != orgId) throw NotFoundException("Bed not found")
    }

    private fun checkPlantOwnership(plantId: Long, orgId: Long): Plant {
        val plant = plantRepository.findById(plantId) ?: throw NotFoundException("Plant not found")
        if (plant.orgId != orgId) throw NotFoundException("Plant not found")
        return plant
    }

    // ── Plant CRUD ──

    fun getAllPlantsForUser(orgId: Long, status: PlantStatus? = null, seasonId: Long? = null, limit: Int = 50, offset: Int = 0): List<PlantResponse> {
        val plants = plantRepository.findByOrgId(orgId, status, seasonId, limit, offset)
        val speciesNames = speciesRepository.findNamesByIds(plants.mapNotNull { it.speciesId }.toSet())
        return plants.map { it.toResponse(speciesNames[it.speciesId]) }
    }

    fun getPlantsForBed(bedId: Long, orgId: Long, seasonId: Long? = null): List<PlantResponse> {
        checkBedOwnership(bedId, orgId)
        val plants = plantRepository.findByBedId(bedId, seasonId)
        val speciesNames = speciesRepository.findNamesByIds(plants.mapNotNull { it.speciesId }.toSet())
        return plants.map { it.toResponse(speciesNames[it.speciesId]) }
    }

    fun getPlant(plantId: Long, orgId: Long): PlantResponse {
        val plant = checkPlantOwnership(plantId, orgId)
        val speciesName = plant.speciesId?.let { speciesRepository.findNamesByIds(setOf(it))[it] }
        return plant.toResponse(speciesName)
    }

    fun createPlant(bedId: Long?, request: CreatePlantRequest, orgId: Long): PlantResponse {
        if (bedId != null) checkBedOwnership(bedId, orgId)
        val plant = plantRepository.persist(
            Plant(
                name = request.name,
                speciesId = request.speciesId,
                plantedDate = request.plantedDate,
                status = request.status,
                seedCount = request.seedCount,
                survivingCount = request.survivingCount,
                bedId = bedId,
                orgId = orgId,
            )
        )
        val speciesName = plant.speciesId?.let { speciesRepository.findNamesByIds(setOf(it))[it] }
        return plant.toResponse(speciesName)
    }

    @Transactional
    fun batchSow(request: BatchSowRequest, orgId: Long): BatchSowResponse {
        if (request.seedCount <= 0 || request.seedCount > 10000) {
            throw BadRequestException("seedCount must be between 1 and 10000")
        }
        if (request.name.isBlank()) {
            throw BadRequestException("name must not be blank")
        }
        if (request.name.length > 255) {
            throw BadRequestException("name must not exceed 255 characters")
        }
        if (request.bedId != null) checkBedOwnership(request.bedId, orgId)
        if (request.bedId != null && request.trayLocationId != null) {
            throw BadRequestException("bedId and trayLocationId are mutually exclusive")
        }
        if (request.trayLocationId != null) {
            val loc = trayLocationRepository.findById(request.trayLocationId)
                ?: throw NotFoundException("Tray location not found")
            if (loc.orgId != orgId) throw NotFoundException("Tray location not found")
        }
        val today = request.plantedDate ?: java.time.LocalDate.now()
        val plantIds = mutableListOf<Long>()
        // Upload image once if provided
        var imageUrl: String? = null
        for (i in 1..request.seedCount) {
            val plant = plantRepository.persist(
                Plant(
                    name = "${request.name} #$i",
                    speciesId = request.speciesId,
                    plantedDate = today,
                    status = PlantStatus.SEEDED,
                    seedCount = 1,
                    survivingCount = 1,
                    bedId = request.bedId,
                    trayLocationId = if (request.bedId == null) request.trayLocationId else null,
                    orgId = orgId,
                )
            )
            if (i == 1 && request.imageBase64 != null) {
                imageUrl = storageService.uploadImage(request.imageBase64, "plants/${plant.id!!}/events/seeded.jpg")
            }
            plantEventRepository.persist(
                PlantEvent(
                    plantId = plant.id!!,
                    eventType = PlantEventType.SEEDED,
                    eventDate = today,
                    plantCount = 1,
                    notes = request.notes,
                    imageUrl = imageUrl,
                )
            )
            plantIds.add(plant.id!!)
        }
        return BatchSowResponse(plantIds = plantIds, count = plantIds.size)
    }

    fun getTraySummary(orgId: Long): List<TraySummaryEntry> {
        return plantRepository.traySummary(orgId)
    }

    fun getPlantGroups(orgId: Long, status: String, trayOnly: Boolean): List<PlantGroupResponse> {
        val plantStatus = try {
            PlantStatus.valueOf(status)
        } catch (e: IllegalArgumentException) {
            throw BadRequestException("Invalid plant status: '$status'. Valid values: ${PlantStatus.entries.joinToString()}")
        }
        return plantRepository.findGroupedBySpecies(orgId, plantStatus, trayOnly).map { row ->
            PlantGroupResponse(
                speciesId = row["speciesId"] as? Long ?: 0,
                speciesName = row["speciesName"] as? String,
                variantName = row["variantName"] as? String,
                bedId = row["bedId"] as? Long,
                bedName = row["bedName"] as? String,
                gardenName = row["gardenName"] as? String,
                plantedDate = row["plantedDate"] as? String,
                status = row["status"] as? String ?: status,
                count = row["count"] as? Int ?: 0,
            )
        }
    }

    @Transactional
    fun batchEvent(request: BatchEventRequest, orgId: Long): BatchEventResponse {
        val plantStatus = PlantStatus.valueOf(request.status)
        val newStatus = PlantStatus.valueOf(request.eventType.let {
            when (it) {
                "POTTED_UP" -> "POTTED_UP"
                "PLANTED_OUT" -> "PLANTED_OUT"
                else -> it
            }
        })
        val plantedDate = request.plantedDate?.let { java.time.LocalDate.parse(it) }
        val plants = plantRepository.findByGroup(orgId, request.speciesId, request.bedId, plantedDate, plantStatus, request.count)
        // Pre-fetch workflow steps for the species (all plants in a batch share the same species)
        val matchingStepIds = request.speciesId?.let { speciesId ->
            workflowRepository.findStepsBySpeciesId(speciesId)
                .filter { it.eventType == request.eventType }
                .mapNotNull { it.id }
        } ?: emptyList()
        var imageUrl: String? = null
        for ((i, plant) in plants.withIndex()) {
            if (i == 0 && request.imageBase64 != null) {
                imageUrl = storageService.uploadImage(request.imageBase64, "plants/${plant.id!!}/events/${request.eventType.lowercase()}.jpg")
            }
            plantEventRepository.persist(
                PlantEvent(
                    plantId = plant.id!!,
                    eventType = PlantEventType.valueOf(request.eventType),
                    eventDate = java.time.LocalDate.now(),
                    plantCount = 1,
                    notes = request.notes,
                    imageUrl = imageUrl,
                )
            )
            val updated = plant.copy(
                status = newStatus,
                bedId = request.targetBedId ?: plant.bedId,
            )
            plantRepository.update(updated)

            // Auto-record workflow progress if a matching step exists
            for (stepId in matchingStepIds) {
                workflowRepository.recordProgress(plant.id!!, stepId)
            }
        }
        return BatchEventResponse(updatedCount = plants.size)
    }

    fun updatePlant(plantId: Long, request: UpdatePlantRequest, orgId: Long): PlantResponse {
        val plant = checkPlantOwnership(plantId, orgId)
        val updated = plant.copy(
            name = request.name ?: plant.name,
            speciesId = request.speciesId ?: plant.speciesId,
            plantedDate = request.plantedDate ?: plant.plantedDate,
            status = request.status ?: plant.status,
            seedCount = request.seedCount ?: plant.seedCount,
            survivingCount = request.survivingCount ?: plant.survivingCount,
        )
        plantRepository.update(updated)
        val speciesName = updated.speciesId?.let { speciesRepository.findNamesByIds(setOf(it))[it] }
        return updated.toResponse(speciesName)
    }

    fun deletePlant(plantId: Long, orgId: Long) {
        checkPlantOwnership(plantId, orgId)
        plantRepository.delete(plantId)
    }

    // ── Plant Events ──

    fun getEventsForPlant(plantId: Long, orgId: Long): List<PlantEventResponse> {
        checkPlantOwnership(plantId, orgId)
        return plantEventRepository.findByPlantId(plantId).map { it.toResponse() }
    }

    fun addEvent(plantId: Long, request: CreatePlantEventRequest, orgId: Long): PlantEventResponse {
        val plant = checkPlantOwnership(plantId, orgId)

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
            PlantEventType.LIFTED, PlantEventType.STORED -> plant.copy(
                status = PlantStatus.DORMANT,
            )
            PlantEventType.NOTE, PlantEventType.BUDDING, PlantEventType.FIRST_BLOOM,
            PlantEventType.PEAK_BLOOM, PlantEventType.LAST_BLOOM, PlantEventType.DIVIDED,
            PlantEventType.PINCHED, PlantEventType.DISBUDDED, PlantEventType.APPLIED_SUPPLY,
            PlantEventType.WATERED, PlantEventType.MOVED, PlantEventType.WEEDED -> plant
        }

        if (updatedPlant !== plant) {
            plantRepository.update(updatedPlant)
        }

        // Auto-record workflow progress if a matching step exists
        plant.speciesId?.let { speciesId ->
            val steps = workflowRepository.findStepsBySpeciesId(speciesId)
            val eventTypeName = request.eventType.name
            steps.filter { it.eventType == eventTypeName }.forEach { step ->
                workflowRepository.recordProgress(plantId, step.id!!)
            }
        }

        return event.toResponse()
    }

    fun deleteEvent(plantId: Long, eventId: Long, orgId: Long) {
        checkPlantOwnership(plantId, orgId)
        val event = plantEventRepository.findById(eventId) ?: throw NotFoundException("Event not found")
        if (event.plantId != plantId) throw NotFoundException("Event not found")
        plantEventRepository.delete(eventId)
    }

    // ── Species Plant Summary ──

    fun getSpeciesPlantSummary(orgId: Long) = plantRepository.speciesSummary(orgId)

    fun getSpeciesLocations(orgId: Long, speciesId: Long) = plantRepository.speciesLocations(orgId, speciesId)

    fun getSpeciesEventSummary(orgId: Long, speciesId: Long, trayOnly: Boolean) =
        plantRepository.speciesEventSummary(orgId, speciesId, trayOnly)

    fun deleteSpeciesEvent(orgId: Long, speciesId: Long, request: DeleteSpeciesEventRequest): DeleteSpeciesEventResponse {
        if (request.count <= 0) throw BadRequestException("count must be > 0")
        val (events, plants) = plantRepository.deleteSpeciesEventForCount(
            orgId = orgId,
            speciesId = speciesId,
            eventType = request.eventType,
            eventDate = request.eventDate,
            count = request.count,
            currentStatus = request.currentStatus,
            trayOnly = request.trayOnly,
        )
        return DeleteSpeciesEventResponse(eventsDeleted = events, plantsRemoved = plants)
    }

    fun updateSpeciesEventDate(orgId: Long, speciesId: Long, request: UpdateSpeciesEventDateRequest): UpdateSpeciesEventDateResponse {
        val updated = plantRepository.updateSpeciesEventDate(
            orgId = orgId,
            speciesId = speciesId,
            eventType = request.eventType,
            oldDate = request.oldDate,
            newDate = request.newDate,
            currentStatus = request.currentStatus,
            trayOnly = request.trayOnly,
        )
        return UpdateSpeciesEventDateResponse(updated)
    }

    // ── Stats ──

    fun getHarvestStats(orgId: Long): List<HarvestStatRow> {
        return plantEventRepository.harvestStatsBySpecies(orgId).map {
            HarvestStatRow(
                species = it.species,
                totalWeightGrams = it.totalWeightGrams,
                totalQuantity = it.totalQuantity,
                harvestCount = it.harvestCount,
                totalStems = it.totalStems,
            )
        }
    }

    // ── Mapping ──

    private fun Plant.toResponse(speciesName: String?) = PlantResponse(
        id = id!!, name = name, speciesId = speciesId,
        speciesName = speciesName,
        plantedDate = plantedDate, status = status,
        seedCount = seedCount, survivingCount = survivingCount,
        bedId = bedId, seasonId = seasonId,
        createdAt = createdAt, updatedAt = updatedAt
    )
}

fun PlantEvent.toResponse() = PlantEventResponse(
    id = id!!, plantId = plantId, eventType = eventType,
    eventDate = eventDate, plantCount = plantCount,
    weightGrams = weightGrams, quantity = quantity,
    notes = notes, imageUrl = imageUrl,
    aiSuggestions = aiSuggestions,
    stemCount = stemCount, stemLengthCm = stemLengthCm,
    qualityGrade = qualityGrade, vaseLifeDays = vaseLifeDays,
    harvestDestinationId = harvestDestinationId,
    customerName = null,
    supplyApplicationId = supplyApplicationId,
    createdAt = createdAt,
)
