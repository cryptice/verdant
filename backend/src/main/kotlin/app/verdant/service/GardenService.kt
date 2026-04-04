package app.verdant.service

import app.verdant.dto.*
import app.verdant.entity.Bed
import app.verdant.entity.Garden
import app.verdant.repository.BedRepository
import app.verdant.repository.GardenRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.NotFoundException
import java.util.logging.Logger

@ApplicationScoped
class GardenService(
    private val gardenRepository: GardenRepository,
    private val bedRepository: BedRepository,
    private val aiService: AiService
) {

    fun getGardensForUser(orgId: Long): List<GardenResponse> =
        gardenRepository.findByOrgId(orgId).map { it.toResponse() }

    private val log = Logger.getLogger(GardenService::class.java.name)

    fun getGarden(gardenId: Long, orgId: Long): GardenResponse {
        log.info("getGarden: gardenId=$gardenId, orgId=$orgId")
        val garden = gardenRepository.findById(gardenId)
        if (garden == null) {
            log.warning("Garden $gardenId not found in DB")
            throw NotFoundException("Garden not found")
        }
        log.info("Found garden: id=${garden.id}, orgId=${garden.orgId}")
        if (garden.orgId != orgId) throw NotFoundException("Garden not found")
        return garden.toResponse()
    }

    fun createGarden(request: CreateGardenRequest, orgId: Long): GardenResponse {
        val garden = gardenRepository.persist(
            Garden(
                name = request.name,
                description = request.description,
                emoji = request.emoji,
                orgId = orgId,
                latitude = request.latitude,
                longitude = request.longitude,
                address = request.address,
                boundaryJson = request.boundaryJson
            )
        )
        return garden.toResponse()
    }

    fun updateGarden(gardenId: Long, request: UpdateGardenRequest, orgId: Long): GardenResponse {
        val garden = gardenRepository.findById(gardenId) ?: throw NotFoundException("Garden not found")
        if (garden.orgId != orgId) throw NotFoundException("Garden not found")
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

    fun deleteGarden(gardenId: Long, orgId: Long) {
        val garden = gardenRepository.findById(gardenId) ?: throw NotFoundException("Garden not found")
        if (garden.orgId != orgId) throw NotFoundException("Garden not found")
        gardenRepository.delete(gardenId)
    }

    fun suggestLayout(request: SuggestLayoutRequest): SuggestLayoutResponse =
        aiService.suggestLayout(request)

    fun createGardenWithLayout(request: CreateGardenWithLayoutRequest, orgId: Long): GardenWithBedsResponse {
        val garden = gardenRepository.persist(
            Garden(
                name = request.name,
                description = request.description,
                emoji = request.emoji,
                orgId = orgId,
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
