package app.verdant.service

import app.verdant.dto.*
import app.verdant.entity.Garden
import app.verdant.repository.GardenRepository
import app.verdant.repository.UserRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import jakarta.ws.rs.ForbiddenException
import jakarta.ws.rs.NotFoundException

@ApplicationScoped
class GardenService(
    private val gardenRepository: GardenRepository,
    private val userRepository: UserRepository
) {
    fun getGardensForUser(userId: Long): List<GardenResponse> =
        gardenRepository.findByOwnerId(userId).map { it.toResponse() }

    fun getGarden(gardenId: Long, userId: Long): GardenResponse {
        val garden = gardenRepository.findById(gardenId) ?: throw NotFoundException("Garden not found")
        if (garden.owner.id != userId) throw ForbiddenException()
        return garden.toResponse()
    }

    @Transactional
    fun createGarden(request: CreateGardenRequest, userId: Long): GardenResponse {
        val owner = userRepository.findById(userId) ?: throw NotFoundException("User not found")
        val garden = Garden().apply {
            name = request.name
            description = request.description
            emoji = request.emoji
            this.owner = owner
        }
        gardenRepository.persist(garden)
        return garden.toResponse()
    }

    @Transactional
    fun updateGarden(gardenId: Long, request: UpdateGardenRequest, userId: Long): GardenResponse {
        val garden = gardenRepository.findById(gardenId) ?: throw NotFoundException("Garden not found")
        if (garden.owner.id != userId) throw ForbiddenException()
        request.name?.let { garden.name = it }
        request.description?.let { garden.description = it }
        request.emoji?.let { garden.emoji = it }
        return garden.toResponse()
    }

    @Transactional
    fun deleteGarden(gardenId: Long, userId: Long) {
        val garden = gardenRepository.findById(gardenId) ?: throw NotFoundException("Garden not found")
        if (garden.owner.id != userId) throw ForbiddenException()
        gardenRepository.delete(garden)
    }
}

fun Garden.toResponse() = GardenResponse(
    id = id!!, name = name, description = description,
    emoji = emoji, createdAt = createdAt, updatedAt = updatedAt
)
