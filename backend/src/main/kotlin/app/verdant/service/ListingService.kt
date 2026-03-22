package app.verdant.service

import app.verdant.dto.*
import app.verdant.entity.Listing
import app.verdant.repository.ListingRepository
import app.verdant.repository.SpeciesRepository
import app.verdant.repository.UserRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.BadRequestException
import jakarta.ws.rs.ForbiddenException
import jakarta.ws.rs.NotFoundException
import java.util.UUID

@ApplicationScoped
class ListingService(
    private val repo: ListingRepository,
    private val speciesRepo: SpeciesRepository,
    private val userRepo: UserRepository,
    private val storageService: StorageService,
) {

    fun getActiveListings(): List<ListingResponse> =
        repo.findActive().map { it.toResponse() }

    fun getListingsForUser(userId: Long): List<ListingResponse> =
        repo.findByUserId(userId).map { it.toResponse() }

    fun getListing(id: Long): ListingResponse {
        val listing = repo.findById(id) ?: throw NotFoundException("Listing not found")
        return listing.toResponse()
    }

    fun createListing(request: CreateListingRequest, userId: Long): ListingResponse {
        val species = speciesRepo.findById(request.speciesId)
            ?: throw BadRequestException("Species not found")

        var imageUrl: String? = null
        if (request.imageBase64 != null) {
            imageUrl = storageService.uploadImage(
                request.imageBase64,
                "listings/${UUID.randomUUID()}.jpg"
            )
        }

        val listing = repo.persist(
            Listing(
                userId = userId,
                speciesId = request.speciesId,
                title = request.title,
                description = request.description,
                quantityAvailable = request.quantityAvailable,
                pricePerStemCents = request.pricePerStemCents,
                availableFrom = request.availableFrom,
                availableUntil = request.availableUntil,
                imageUrl = imageUrl,
            )
        )
        return listing.toResponse()
    }

    fun updateListing(id: Long, request: UpdateListingRequest, userId: Long): ListingResponse {
        val listing = repo.findById(id) ?: throw NotFoundException("Listing not found")
        if (listing.userId != userId) throw ForbiddenException()
        val updated = listing.copy(
            title = request.title ?: listing.title,
            description = request.description ?: listing.description,
            quantityAvailable = request.quantityAvailable ?: listing.quantityAvailable,
            pricePerStemCents = request.pricePerStemCents ?: listing.pricePerStemCents,
            availableFrom = request.availableFrom ?: listing.availableFrom,
            availableUntil = request.availableUntil ?: listing.availableUntil,
            isActive = request.isActive ?: listing.isActive,
        )
        repo.update(updated)
        return updated.toResponse()
    }

    fun deleteListing(id: Long, userId: Long) {
        val listing = repo.findById(id) ?: throw NotFoundException("Listing not found")
        if (listing.userId != userId) throw ForbiddenException()
        repo.delete(id)
    }

    private fun Listing.toResponse(): ListingResponse {
        val species = speciesRepo.findById(speciesId)
        val user = userRepo.findById(userId)
        return ListingResponse(
            id = id!!,
            userId = userId,
            producerName = user?.displayName ?: "Unknown",
            speciesId = speciesId,
            speciesName = species?.commonName ?: "Unknown",
            speciesNameSv = species?.commonNameSv,
            title = title,
            description = description,
            quantityAvailable = quantityAvailable,
            pricePerStemCents = pricePerStemCents,
            availableFrom = availableFrom,
            availableUntil = availableUntil,
            imageUrl = imageUrl,
            isActive = isActive,
            createdAt = createdAt,
        )
    }
}
