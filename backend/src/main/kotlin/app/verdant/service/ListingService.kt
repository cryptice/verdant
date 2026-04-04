package app.verdant.service

import app.verdant.dto.*
import app.verdant.entity.Listing
import app.verdant.repository.ListingRepository
import app.verdant.repository.SpeciesRepository
import app.verdant.repository.UserRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.BadRequestException
import jakarta.ws.rs.NotFoundException
import java.util.UUID

@ApplicationScoped
class ListingService(
    private val repo: ListingRepository,
    private val speciesRepo: SpeciesRepository,
    private val userRepo: UserRepository,
    private val storageService: StorageService,
) {

    fun getActiveListings(limit: Int = 50, offset: Int = 0): List<ListingResponse> {
        val listings = repo.findActive(limit, offset)
        return mapListings(listings)
    }

    fun getListingsForUser(orgId: Long, limit: Int = 50, offset: Int = 0): List<ListingResponse> {
        val listings = repo.findByOrgId(orgId, limit, offset)
        return mapListings(listings)
    }

    fun getListing(id: Long, orgId: Long): ListingResponse {
        val listing = repo.findById(id) ?: throw NotFoundException("Listing not found")
        if (!listing.isActive && listing.orgId != orgId) throw NotFoundException("Listing not found")
        return mapListings(listOf(listing)).first()
    }

    fun createListing(request: CreateListingRequest, orgId: Long): ListingResponse {
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
                orgId = orgId,
                speciesId = request.speciesId,
                title = request.title,
                description = request.description,
                quantityAvailable = request.quantityAvailable,
                pricePerStemSek = request.pricePerStemSek,
                availableFrom = request.availableFrom,
                availableUntil = request.availableUntil,
                imageUrl = imageUrl,
            )
        )
        return mapListings(listOf(listing)).first()
    }

    fun updateListing(id: Long, request: UpdateListingRequest, orgId: Long): ListingResponse {
        val listing = repo.findById(id) ?: throw NotFoundException("Listing not found")
        if (listing.orgId != orgId) throw NotFoundException("Listing not found")
        val updated = listing.copy(
            title = request.title ?: listing.title,
            description = request.description ?: listing.description,
            quantityAvailable = request.quantityAvailable ?: listing.quantityAvailable,
            pricePerStemSek = request.pricePerStemSek ?: listing.pricePerStemSek,
            availableFrom = request.availableFrom ?: listing.availableFrom,
            availableUntil = request.availableUntil ?: listing.availableUntil,
            isActive = request.isActive ?: listing.isActive,
        )
        repo.update(updated)
        return mapListings(listOf(updated)).first()
    }

    fun deleteListing(id: Long, orgId: Long) {
        val listing = repo.findById(id) ?: throw NotFoundException("Listing not found")
        if (listing.orgId != orgId) throw NotFoundException("Listing not found")
        repo.delete(id)
    }

    private fun mapListings(listings: List<Listing>): List<ListingResponse> {
        if (listings.isEmpty()) return emptyList()
        val speciesIds = listings.map { it.speciesId }.toSet()
        val orgIds = listings.map { it.orgId }.toSet()
        val speciesById = speciesRepo.findByIds(speciesIds)
        val usersById = userRepo.findByIds(orgIds)
        return listings.map { listing ->
            val species = speciesById[listing.speciesId]
            val user = usersById[listing.orgId]
            ListingResponse(
                id = listing.id!!,
                sellerName = user?.displayName ?: "Unknown",
                producerName = user?.displayName ?: "Unknown",
                speciesId = listing.speciesId,
                speciesName = species?.commonName ?: "Unknown",
                speciesNameSv = species?.commonNameSv,
                title = listing.title,
                description = listing.description,
                quantityAvailable = listing.quantityAvailable,
                pricePerStemSek = listing.pricePerStemSek,
                availableFrom = listing.availableFrom,
                availableUntil = listing.availableUntil,
                imageUrl = listing.imageUrl,
                isActive = listing.isActive,
                createdAt = listing.createdAt,
            )
        }
    }
}
