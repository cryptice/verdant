package app.verdant.service

import app.verdant.dto.*
import app.verdant.entity.Bed
import app.verdant.repository.BedRepository
import app.verdant.repository.GardenRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.ForbiddenException
import jakarta.ws.rs.NotFoundException

@ApplicationScoped
class BedService(
    private val bedRepository: BedRepository,
    private val gardenRepository: GardenRepository
) {
    fun getAllBedsForUser(userId: Long): List<BedWithGardenResponse> {
        return bedRepository.findByUserIdWithGardenName(userId).map {
            BedWithGardenResponse(
                id = it.bed.id!!,
                name = it.bed.name,
                description = it.bed.description,
                gardenId = it.bed.gardenId,
                gardenName = it.gardenName,
                boundaryJson = it.bed.boundaryJson,
            )
        }
    }

    fun getBedsForGarden(gardenId: Long, userId: Long): List<BedResponse> {
        val garden = gardenRepository.findById(gardenId) ?: throw NotFoundException("Garden not found")
        if (garden.ownerId != userId) throw ForbiddenException()
        return bedRepository.findByGardenId(gardenId).map { it.toResponse() }
    }

    fun getBed(bedId: Long, userId: Long): BedResponse {
        val bed = bedRepository.findById(bedId) ?: throw NotFoundException("Bed not found")
        val garden = gardenRepository.findById(bed.gardenId) ?: throw NotFoundException("Garden not found")
        if (garden.ownerId != userId) throw ForbiddenException()
        return bed.toResponse()
    }

    fun createBed(gardenId: Long, request: CreateBedRequest, userId: Long): BedResponse {
        val garden = gardenRepository.findById(gardenId) ?: throw NotFoundException("Garden not found")
        if (garden.ownerId != userId) throw ForbiddenException()
        val bed = bedRepository.persist(
            Bed(name = request.name, description = request.description, gardenId = gardenId, boundaryJson = request.boundaryJson)
        )
        return bed.toResponse()
    }

    fun updateBed(bedId: Long, request: UpdateBedRequest, userId: Long): BedResponse {
        val bed = bedRepository.findById(bedId) ?: throw NotFoundException("Bed not found")
        val garden = gardenRepository.findById(bed.gardenId) ?: throw NotFoundException("Garden not found")
        if (garden.ownerId != userId) throw ForbiddenException()
        val updated = bed.copy(
            name = request.name ?: bed.name,
            description = request.description ?: bed.description,
            boundaryJson = request.boundaryJson ?: bed.boundaryJson,
        )
        bedRepository.update(updated)
        return updated.toResponse()
    }

    fun deleteBed(bedId: Long, userId: Long) {
        val bed = bedRepository.findById(bedId) ?: throw NotFoundException("Bed not found")
        val garden = gardenRepository.findById(bed.gardenId) ?: throw NotFoundException("Garden not found")
        if (garden.ownerId != userId) throw ForbiddenException()
        bedRepository.delete(bedId)
    }
}

fun Bed.toResponse() = BedResponse(
    id = id!!, name = name, description = description,
    gardenId = gardenId, boundaryJson = boundaryJson,
    lengthMeters = lengthMeters, widthMeters = widthMeters,
    createdAt = createdAt, updatedAt = updatedAt
)
