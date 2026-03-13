package app.verdant.service

import app.verdant.dto.*
import app.verdant.entity.Plant
import app.verdant.repository.BedRepository
import app.verdant.repository.PlantRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import jakarta.ws.rs.ForbiddenException
import jakarta.ws.rs.NotFoundException

@ApplicationScoped
class PlantService(
    private val plantRepository: PlantRepository,
    private val bedRepository: BedRepository
) {
    fun getPlantsForBed(bedId: Long, userId: Long): List<PlantResponse> {
        val bed = bedRepository.findById(bedId) ?: throw NotFoundException("Bed not found")
        if (bed.garden.owner.id != userId) throw ForbiddenException()
        return plantRepository.findByBedId(bedId).map { it.toResponse() }
    }

    fun getPlant(plantId: Long, userId: Long): PlantResponse {
        val plant = plantRepository.findById(plantId) ?: throw NotFoundException("Plant not found")
        if (plant.bed.garden.owner.id != userId) throw ForbiddenException()
        return plant.toResponse()
    }

    @Transactional
    fun createPlant(bedId: Long, request: CreatePlantRequest, userId: Long): PlantResponse {
        val bed = bedRepository.findById(bedId) ?: throw NotFoundException("Bed not found")
        if (bed.garden.owner.id != userId) throw ForbiddenException()
        val plant = Plant().apply {
            name = request.name
            species = request.species
            plantedDate = request.plantedDate
            status = request.status
            this.bed = bed
        }
        plantRepository.persist(plant)
        return plant.toResponse()
    }

    @Transactional
    fun updatePlant(plantId: Long, request: UpdatePlantRequest, userId: Long): PlantResponse {
        val plant = plantRepository.findById(plantId) ?: throw NotFoundException("Plant not found")
        if (plant.bed.garden.owner.id != userId) throw ForbiddenException()
        request.name?.let { plant.name = it }
        request.species?.let { plant.species = it }
        request.plantedDate?.let { plant.plantedDate = it }
        request.status?.let { plant.status = it }
        return plant.toResponse()
    }

    @Transactional
    fun deletePlant(plantId: Long, userId: Long) {
        val plant = plantRepository.findById(plantId) ?: throw NotFoundException("Plant not found")
        if (plant.bed.garden.owner.id != userId) throw ForbiddenException()
        plantRepository.delete(plant)
    }
}

fun Plant.toResponse() = PlantResponse(
    id = id!!, name = name, species = species,
    plantedDate = plantedDate, status = status, bedId = bed.id!!,
    createdAt = createdAt, updatedAt = updatedAt
)
