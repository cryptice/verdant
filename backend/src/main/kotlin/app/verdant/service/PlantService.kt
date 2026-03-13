package app.verdant.service

import app.verdant.dto.*
import app.verdant.entity.Plant
import app.verdant.repository.BedRepository
import app.verdant.repository.GardenRepository
import app.verdant.repository.PlantRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.ForbiddenException
import jakarta.ws.rs.NotFoundException

@ApplicationScoped
class PlantService(
    private val plantRepository: PlantRepository,
    private val bedRepository: BedRepository,
    private val gardenRepository: GardenRepository
) {
    private fun checkBedOwnership(bedId: Long, userId: Long) {
        val bed = bedRepository.findById(bedId) ?: throw NotFoundException("Bed not found")
        val garden = gardenRepository.findById(bed.gardenId) ?: throw NotFoundException("Garden not found")
        if (garden.ownerId != userId) throw ForbiddenException()
    }

    fun getPlantsForBed(bedId: Long, userId: Long): List<PlantResponse> {
        checkBedOwnership(bedId, userId)
        return plantRepository.findByBedId(bedId).map { it.toResponse() }
    }

    fun getPlant(plantId: Long, userId: Long): PlantResponse {
        val plant = plantRepository.findById(plantId) ?: throw NotFoundException("Plant not found")
        checkBedOwnership(plant.bedId, userId)
        return plant.toResponse()
    }

    fun createPlant(bedId: Long, request: CreatePlantRequest, userId: Long): PlantResponse {
        checkBedOwnership(bedId, userId)
        val plant = plantRepository.persist(
            Plant(
                name = request.name,
                species = request.species,
                plantedDate = request.plantedDate,
                status = request.status,
                bedId = bedId,
            )
        )
        return plant.toResponse()
    }

    fun updatePlant(plantId: Long, request: UpdatePlantRequest, userId: Long): PlantResponse {
        val plant = plantRepository.findById(plantId) ?: throw NotFoundException("Plant not found")
        checkBedOwnership(plant.bedId, userId)
        val updated = plant.copy(
            name = request.name ?: plant.name,
            species = request.species ?: plant.species,
            plantedDate = request.plantedDate ?: plant.plantedDate,
            status = request.status ?: plant.status,
        )
        plantRepository.update(updated)
        return updated.toResponse()
    }

    fun deletePlant(plantId: Long, userId: Long) {
        val plant = plantRepository.findById(plantId) ?: throw NotFoundException("Plant not found")
        checkBedOwnership(plant.bedId, userId)
        plantRepository.delete(plantId)
    }
}

fun Plant.toResponse() = PlantResponse(
    id = id!!, name = name, species = species,
    plantedDate = plantedDate, status = status, bedId = bedId,
    createdAt = createdAt, updatedAt = updatedAt
)
