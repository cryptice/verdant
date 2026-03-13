package app.verdant.service

import app.verdant.dto.*
import app.verdant.entity.Bed
import app.verdant.repository.BedRepository
import app.verdant.repository.GardenRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import jakarta.ws.rs.ForbiddenException
import jakarta.ws.rs.NotFoundException

@ApplicationScoped
class BedService(
    private val bedRepository: BedRepository,
    private val gardenRepository: GardenRepository
) {
    fun getBedsForGarden(gardenId: Long, userId: Long): List<BedResponse> {
        val garden = gardenRepository.findById(gardenId) ?: throw NotFoundException("Garden not found")
        if (garden.owner.id != userId) throw ForbiddenException()
        return bedRepository.findByGardenId(gardenId).map { it.toResponse() }
    }

    fun getBed(bedId: Long, userId: Long): BedResponse {
        val bed = bedRepository.findById(bedId) ?: throw NotFoundException("Bed not found")
        if (bed.garden.owner.id != userId) throw ForbiddenException()
        return bed.toResponse()
    }

    @Transactional
    fun createBed(gardenId: Long, request: CreateBedRequest, userId: Long): BedResponse {
        val garden = gardenRepository.findById(gardenId) ?: throw NotFoundException("Garden not found")
        if (garden.owner.id != userId) throw ForbiddenException()
        val bed = Bed().apply {
            name = request.name
            description = request.description
            this.garden = garden
        }
        bedRepository.persist(bed)
        return bed.toResponse()
    }

    @Transactional
    fun updateBed(bedId: Long, request: UpdateBedRequest, userId: Long): BedResponse {
        val bed = bedRepository.findById(bedId) ?: throw NotFoundException("Bed not found")
        if (bed.garden.owner.id != userId) throw ForbiddenException()
        request.name?.let { bed.name = it }
        request.description?.let { bed.description = it }
        return bed.toResponse()
    }

    @Transactional
    fun deleteBed(bedId: Long, userId: Long) {
        val bed = bedRepository.findById(bedId) ?: throw NotFoundException("Bed not found")
        if (bed.garden.owner.id != userId) throw ForbiddenException()
        bedRepository.delete(bed)
    }
}

fun Bed.toResponse() = BedResponse(
    id = id!!, name = name, description = description,
    gardenId = garden.id!!, createdAt = createdAt, updatedAt = updatedAt
)
