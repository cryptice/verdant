package app.verdant.service

import app.verdant.dto.*
import app.verdant.entity.Bed
import app.verdant.entity.Garden
import app.verdant.repository.BedRepository
import app.verdant.repository.GardenRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.ForbiddenException
import jakarta.ws.rs.NotFoundException

@ApplicationScoped
class GardenService(
    private val gardenRepository: GardenRepository,
    private val bedRepository: BedRepository,
    private val aiService: AiService
) {

    fun getGardensForUser(userId: Long): List<GardenResponse> =
        gardenRepository.findByOwnerId(userId).map { it.toResponse() }

    fun getGarden(gardenId: Long, userId: Long): GardenResponse {
        val garden = gardenRepository.findById(gardenId) ?: throw NotFoundException("Garden not found")
        if (garden.ownerId != userId) throw ForbiddenException()
        return garden.toResponse()
    }

    fun createGarden(request: CreateGardenRequest, userId: Long): GardenResponse {
        val garden = gardenRepository.persist(
            Garden(
                name = request.name,
                description = request.description,
                emoji = request.emoji,
                ownerId = userId,
                latitude = request.latitude,
                longitude = request.longitude,
                address = request.address,
                boundaryJson = request.boundaryJson
            )
        )
        return garden.toResponse()
    }

    fun updateGarden(gardenId: Long, request: UpdateGardenRequest, userId: Long): GardenResponse {
        val garden = gardenRepository.findById(gardenId) ?: throw NotFoundException("Garden not found")
        if (garden.ownerId != userId) throw ForbiddenException()
        val updated = garden.copy(
            name = request.name ?: garden.name,
            description = request.description ?: garden.description,
            emoji = request.emoji ?: garden.emoji,
            latitude = request.latitude ?: garden.latitude,
            longitude = request.longitude ?: garden.longitude,
            address = request.address ?: garden.address,
            boundaryJson = request.boundaryJson ?: garden.boundaryJson,
        )
        gardenRepository.update(updated)
        return updated.toResponse()
    }

    fun deleteGarden(gardenId: Long, userId: Long) {
        val garden = gardenRepository.findById(gardenId) ?: throw NotFoundException("Garden not found")
        if (garden.ownerId != userId) throw ForbiddenException()
        gardenRepository.delete(gardenId)
    }

    fun suggestLayout(request: SuggestLayoutRequest): SuggestLayoutResponse =
        aiService.suggestLayout(request)

    fun createGardenWithLayout(request: CreateGardenWithLayoutRequest, userId: Long): GardenWithBedsResponse {
        val garden = gardenRepository.persist(
            Garden(
                name = request.name,
                description = request.description,
                emoji = request.emoji,
                ownerId = userId,
                latitude = request.latitude,
                longitude = request.longitude,
                address = request.address,
                boundaryJson = request.boundaryJson
            )
        )

        val beds = request.beds.map { bedLayout ->
            bedRepository.persist(
                Bed(
                    name = bedLayout.name,
                    description = bedLayout.description,
                    gardenId = garden.id!!,
                    boundaryJson = bedLayout.boundaryJson
                )
            )
        }

        return GardenWithBedsResponse(
            garden = garden.toResponse(),
            beds = beds.map { it.toResponse() }
        )
    }
}

fun Garden.toResponse() = GardenResponse(
    id = id!!, name = name, description = description,
    emoji = emoji, latitude = latitude, longitude = longitude,
    address = address, boundaryJson = boundaryJson,
    createdAt = createdAt, updatedAt = updatedAt
)
