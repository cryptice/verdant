package app.verdant.service

import app.verdant.dto.*
import app.verdant.entity.*
import app.verdant.repository.*
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.ForbiddenException
import jakarta.ws.rs.NotFoundException

@ApplicationScoped
class SpeciesService(
    private val speciesRepository: SpeciesRepository,
    private val groupRepository: SpeciesGroupRepository,
    private val tagRepository: SpeciesTagRepository,
    private val photoRepository: SpeciesPhotoRepository,
    private val providerRepository: ProviderRepository,
    private val speciesProviderRepository: SpeciesProviderRepository,
    private val storageService: StorageService,
) {
    // ── Species CRUD ──

    fun getSpeciesForUser(userId: Long): List<SpeciesResponse> {
        val groups = groupRepository.findByUserId(userId).associateBy { it.id }
        val tags = tagRepository.findByUserId(userId).associateBy { it.id }
        return speciesRepository.findByUserId(userId).map { it.toResponse(groups, tags) }
    }

    fun searchSpeciesForUser(userId: Long, query: String, limit: Int = 20): List<SpeciesResponse> {
        val groups = groupRepository.findByUserId(userId).associateBy { it.id }
        val tags = tagRepository.findByUserId(userId).associateBy { it.id }
        return speciesRepository.searchByUserId(userId, query, limit).map { it.toResponse(groups, tags) }
    }

    fun getSpecies(speciesId: Long, userId: Long): SpeciesResponse {
        val species = speciesRepository.findById(speciesId) ?: throw NotFoundException("Species not found")
        if (species.userId != null && species.userId != userId) throw ForbiddenException()
        val groups: Map<Long?, SpeciesGroup> = species.groupId?.let { groupRepository.findById(it) }?.let { mapOf(it.id to it) } ?: emptyMap()
        val tags = tagRepository.findByUserId(userId).associateBy { it.id }
        return species.toResponse(groups, tags)
    }

    fun createSpecies(request: CreateSpeciesRequest, userId: Long): SpeciesResponse {
        val species = speciesRepository.persist(
            Species(
                userId = userId,
                commonName = request.commonName,
                variantName = request.variantName,
                commonNameSv = request.commonNameSv,
                variantNameSv = request.variantNameSv,
                scientificName = request.scientificName,
                daysToSprout = request.daysToSprout,
                daysToHarvest = request.daysToHarvest,
                germinationTimeDays = request.germinationTimeDays,
                sowingDepthMm = request.sowingDepthMm,
                growingPositions = request.growingPositions ?: emptyList(),
                soils = request.soils ?: emptyList(),
                heightCm = request.heightCm,
                bloomMonths = request.bloomMonths ?: emptyList(),
                sowingMonths = request.sowingMonths ?: emptyList(),
                germinationRate = request.germinationRate,
                groupId = request.groupId,
            )
        )
        val sid = species.id!!
        // Upload images to GCS
        val frontUrl = request.imageFrontBase64?.let { storageService.uploadSpeciesFront(sid, it) }
        val backUrl = request.imageBackBase64?.let { storageService.uploadSpeciesBack(sid, it) }
        if (frontUrl != null || backUrl != null) {
            speciesRepository.update(species.copy(imageFrontUrl = frontUrl, imageBackUrl = backUrl))
        }
        if (!request.tagIds.isNullOrEmpty()) {
            speciesRepository.setTagsForSpecies(sid, request.tagIds)
        }
        return getSpecies(sid, userId)
    }

    fun updateSpecies(speciesId: Long, request: UpdateSpeciesRequest, userId: Long): SpeciesResponse {
        val species = speciesRepository.findById(speciesId) ?: throw NotFoundException("Species not found")
        if (species.userId == null) throw ForbiddenException("Cannot modify system species")
        if (species.userId != userId) throw ForbiddenException()
        // Upload new images if provided
        val frontUrl = request.imageFrontBase64?.let { storageService.uploadSpeciesFront(speciesId, it) } ?: species.imageFrontUrl
        val backUrl = request.imageBackBase64?.let { storageService.uploadSpeciesBack(speciesId, it) } ?: species.imageBackUrl
        val updated = species.copy(
            commonName = request.commonName ?: species.commonName,
            variantName = request.variantName ?: species.variantName,
            commonNameSv = request.commonNameSv ?: species.commonNameSv,
            variantNameSv = request.variantNameSv ?: species.variantNameSv,
            scientificName = request.scientificName ?: species.scientificName,
            imageFrontUrl = frontUrl,
            imageBackUrl = backUrl,
            daysToSprout = request.daysToSprout ?: species.daysToSprout,
            daysToHarvest = request.daysToHarvest ?: species.daysToHarvest,
            germinationTimeDays = request.germinationTimeDays ?: species.germinationTimeDays,
            sowingDepthMm = request.sowingDepthMm ?: species.sowingDepthMm,
            growingPositions = request.growingPositions ?: species.growingPositions,
            soils = request.soils ?: species.soils,
            heightCm = request.heightCm ?: species.heightCm,
            bloomMonths = request.bloomMonths ?: species.bloomMonths,
            sowingMonths = request.sowingMonths ?: species.sowingMonths,
            germinationRate = request.germinationRate ?: species.germinationRate,
            groupId = request.groupId ?: species.groupId,
        )
        speciesRepository.update(updated)
        if (request.tagIds != null) {
            speciesRepository.setTagsForSpecies(speciesId, request.tagIds)
        }
        return getSpecies(speciesId, userId)
    }

    fun deleteSpecies(speciesId: Long, userId: Long) {
        val species = speciesRepository.findById(speciesId) ?: throw NotFoundException("Species not found")
        if (species.userId == null) throw ForbiddenException("Cannot delete system species")
        if (species.userId != userId) throw ForbiddenException()
        speciesRepository.delete(speciesId)
    }

    // ── Admin Species CRUD ──

    fun getAllSpecies(): List<SpeciesResponse> {
        val groups = groupRepository.findAll().associateBy { it.id }
        val tags = tagRepository.findAll().associateBy { it.id }
        return speciesRepository.findAll().map { it.toResponse(groups, tags) }
    }

    fun getSpeciesAdmin(speciesId: Long): SpeciesResponse {
        val species = speciesRepository.findById(speciesId) ?: throw NotFoundException("Species not found")
        val groups: Map<Long?, SpeciesGroup> = species.groupId?.let { groupRepository.findById(it) }?.let { mapOf(it.id to it) } ?: emptyMap()
        val tags = tagRepository.findAll().associateBy { it.id }
        return species.toResponse(groups, tags)
    }

    fun createSpeciesAdmin(request: CreateSpeciesRequest): SpeciesResponse {
        val species = speciesRepository.persist(
            Species(
                userId = null,
                commonName = request.commonName,
                variantName = request.variantName,
                commonNameSv = request.commonNameSv,
                variantNameSv = request.variantNameSv,
                scientificName = request.scientificName,
                daysToSprout = request.daysToSprout,
                daysToHarvest = request.daysToHarvest,
                germinationTimeDays = request.germinationTimeDays,
                sowingDepthMm = request.sowingDepthMm,
                growingPositions = request.growingPositions ?: emptyList(),
                soils = request.soils ?: emptyList(),
                heightCm = request.heightCm,
                bloomMonths = request.bloomMonths ?: emptyList(),
                sowingMonths = request.sowingMonths ?: emptyList(),
                germinationRate = request.germinationRate,
                groupId = request.groupId,
            )
        )
        val sid = species.id!!
        val frontUrl = request.imageFrontBase64?.let { storageService.uploadSpeciesFront(sid, it) }
        val backUrl = request.imageBackBase64?.let { storageService.uploadSpeciesBack(sid, it) }
        if (frontUrl != null || backUrl != null) {
            speciesRepository.update(species.copy(imageFrontUrl = frontUrl, imageBackUrl = backUrl))
        }
        if (!request.tagIds.isNullOrEmpty()) {
            speciesRepository.setTagsForSpecies(sid, request.tagIds)
        }
        return getSpeciesAdmin(sid)
    }

    fun updateSpeciesAdmin(speciesId: Long, request: UpdateSpeciesRequest): SpeciesResponse {
        val species = speciesRepository.findById(speciesId) ?: throw NotFoundException("Species not found")
        val frontUrl = request.imageFrontBase64?.let { storageService.uploadSpeciesFront(speciesId, it) } ?: species.imageFrontUrl
        val backUrl = request.imageBackBase64?.let { storageService.uploadSpeciesBack(speciesId, it) } ?: species.imageBackUrl
        val updated = species.copy(
            commonName = request.commonName ?: species.commonName,
            variantName = request.variantName ?: species.variantName,
            commonNameSv = request.commonNameSv ?: species.commonNameSv,
            variantNameSv = request.variantNameSv ?: species.variantNameSv,
            scientificName = request.scientificName ?: species.scientificName,
            imageFrontUrl = frontUrl,
            imageBackUrl = backUrl,
            daysToSprout = request.daysToSprout ?: species.daysToSprout,
            daysToHarvest = request.daysToHarvest ?: species.daysToHarvest,
            germinationTimeDays = request.germinationTimeDays ?: species.germinationTimeDays,
            sowingDepthMm = request.sowingDepthMm ?: species.sowingDepthMm,
            growingPositions = request.growingPositions ?: species.growingPositions,
            soils = request.soils ?: species.soils,
            heightCm = request.heightCm ?: species.heightCm,
            bloomMonths = request.bloomMonths ?: species.bloomMonths,
            sowingMonths = request.sowingMonths ?: species.sowingMonths,
            germinationRate = request.germinationRate ?: species.germinationRate,
            groupId = request.groupId ?: species.groupId,
        )
        speciesRepository.update(updated)
        if (request.tagIds != null) {
            speciesRepository.setTagsForSpecies(speciesId, request.tagIds)
        }
        return getSpeciesAdmin(speciesId)
    }

    fun deleteSpeciesAdmin(speciesId: Long) {
        speciesRepository.findById(speciesId) ?: throw NotFoundException("Species not found")
        speciesRepository.delete(speciesId)
    }

    fun uploadSpeciesPhoto(speciesId: Long, base64: String): SpeciesPhotoResponse {
        speciesRepository.findById(speciesId) ?: throw NotFoundException("Species not found")
        val url = storageService.uploadSpeciesPhoto(speciesId, base64)
        val existingPhotos = photoRepository.findBySpeciesId(speciesId)
        val nextSortOrder = (existingPhotos.maxOfOrNull { it.sortOrder } ?: -1) + 1
        val photo = photoRepository.persist(SpeciesPhoto(speciesId = speciesId, imageUrl = url, sortOrder = nextSortOrder))
        return SpeciesPhotoResponse(photo.id!!, photo.imageUrl, photo.sortOrder)
    }

    fun deleteSpeciesPhoto(speciesId: Long, photoId: Long) {
        val photo = photoRepository.findById(photoId) ?: throw NotFoundException("Photo not found")
        if (photo.speciesId != speciesId) throw NotFoundException("Photo not found for this species")
        storageService.deleteByPath(photo.imageUrl)
        photoRepository.delete(photoId)
    }

    // ── Groups ──

    fun getGroupsForUser(userId: Long): List<SpeciesGroupResponse> =
        groupRepository.findByUserId(userId).map { SpeciesGroupResponse(it.id!!, it.name) }

    fun createGroup(request: CreateSpeciesGroupRequest, userId: Long): SpeciesGroupResponse {
        val group = groupRepository.persist(SpeciesGroup(userId = userId, name = request.name))
        return SpeciesGroupResponse(group.id!!, group.name)
    }

    fun deleteGroup(groupId: Long, userId: Long) {
        val group = groupRepository.findById(groupId) ?: throw NotFoundException("Group not found")
        if (group.userId == null) throw ForbiddenException("Cannot delete system group")
        if (group.userId != userId) throw ForbiddenException()
        groupRepository.delete(groupId)
    }

    // ── Tags ──

    fun getTagsForUser(userId: Long): List<SpeciesTagResponse> =
        tagRepository.findByUserId(userId).map { SpeciesTagResponse(it.id!!, it.name) }

    fun createTag(request: CreateSpeciesTagRequest, userId: Long): SpeciesTagResponse {
        val tag = tagRepository.persist(SpeciesTag(userId = userId, name = request.name))
        return SpeciesTagResponse(tag.id!!, tag.name)
    }

    fun deleteTag(tagId: Long, userId: Long) {
        val tag = tagRepository.findById(tagId) ?: throw NotFoundException("Tag not found")
        if (tag.userId == null) throw ForbiddenException("Cannot delete system tag")
        if (tag.userId != userId) throw ForbiddenException()
        tagRepository.delete(tagId)
    }

    // ── Export / Import ──

    fun exportSpecies(): List<SpeciesExportEntry> {
        val groups = groupRepository.findAll().associateBy { it.id }
        val tags = tagRepository.findAll().associateBy { it.id }
        val providers = providerRepository.findAll().associateBy { it.id }
        return speciesRepository.findAll().map { species ->
            val tagIds = speciesRepository.findTagIdsForSpecies(species.id!!)
            val speciesProviders = speciesProviderRepository.findBySpeciesId(species.id!!).map { sp ->
                val provider = providers[sp.providerId]
                SpeciesExportProvider(
                    providerName = provider?.name ?: "",
                    providerIdentifier = provider?.identifier ?: "",
                    imageFrontUrl = sp.imageFrontUrl,
                    imageBackUrl = sp.imageBackUrl,
                    productUrl = sp.productUrl,
                )
            }
            SpeciesExportEntry(
                commonName = species.commonName,
                variantName = species.variantName,
                commonNameSv = species.commonNameSv,
                variantNameSv = species.variantNameSv,
                scientificName = species.scientificName,
                imageFrontUrl = species.imageFrontUrl,
                imageBackUrl = species.imageBackUrl,
                daysToSprout = species.daysToSprout,
                daysToHarvest = species.daysToHarvest,
                germinationTimeDays = species.germinationTimeDays,
                sowingDepthMm = species.sowingDepthMm,
                growingPositions = species.growingPositions.map { it.name },
                soils = species.soils.map { it.name },
                heightCm = species.heightCm,
                bloomMonths = species.bloomMonths,
                sowingMonths = species.sowingMonths,
                germinationRate = species.germinationRate,
                groupName = species.groupId?.let { groups[it]?.name },
                tagNames = tagIds.mapNotNull { tags[it]?.name },
                providers = speciesProviders,
            )
        }
    }

    @jakarta.transaction.Transactional
    fun importSpecies(entries: List<SpeciesExportEntry>): ImportResult {
        val existingSpecies = speciesRepository.findAll()
        val existingKeys = existingSpecies.map { (it.commonName to it.variantName) }.toSet()

        // Build mutable lookup maps for groups and tags (system-level, userId = null)
        val groupsByName = groupRepository.findAll()
            .filter { it.userId == null }
            .associateBy { it.name }
            .toMutableMap()
        val tagsByName = tagRepository.findAll()
            .filter { it.userId == null }
            .associateBy { it.name }
            .toMutableMap()

        var created = 0
        var skipped = 0

        for (entry in entries) {
            val key = entry.commonName to entry.variantName
            if (key in existingKeys) {
                skipped++
                continue
            }

            // Resolve group
            val groupId = entry.groupName?.let { name ->
                val group = groupsByName.getOrPut(name) {
                    groupRepository.persist(SpeciesGroup(userId = null, name = name))
                }
                group.id
            }

            // Resolve tags
            val tagIds = entry.tagNames.map { name ->
                val tag = tagsByName.getOrPut(name) {
                    tagRepository.persist(SpeciesTag(userId = null, name = name))
                }
                tag.id!!
            }

            val species = speciesRepository.persist(
                Species(
                    userId = null,
                    commonName = entry.commonName,
                    variantName = entry.variantName,
                    commonNameSv = entry.commonNameSv,
                    variantNameSv = entry.variantNameSv,
                    scientificName = entry.scientificName,
                    imageFrontUrl = entry.imageFrontUrl,
                    imageBackUrl = entry.imageBackUrl,
                    daysToSprout = entry.daysToSprout,
                    daysToHarvest = entry.daysToHarvest,
                    germinationTimeDays = entry.germinationTimeDays,
                    sowingDepthMm = entry.sowingDepthMm,
                    growingPositions = entry.growingPositions.map { GrowingPosition.valueOf(it) },
                    soils = entry.soils.map { SoilType.valueOf(it) },
                    heightCm = entry.heightCm,
                    bloomMonths = entry.bloomMonths,
                    sowingMonths = entry.sowingMonths,
                    germinationRate = entry.germinationRate,
                    groupId = groupId,
                )
            )

            if (tagIds.isNotEmpty()) {
                speciesRepository.setTagsForSpecies(species.id!!, tagIds)
            }

            // Restore providers
            val providersByIdentifier = providerRepository.findAll().associateBy { it.identifier }.toMutableMap()
            for (ep in entry.providers) {
                val provider = providersByIdentifier.getOrPut(ep.providerIdentifier) {
                    providerRepository.persist(Provider(name = ep.providerName, identifier = ep.providerIdentifier))
                }
                speciesProviderRepository.persist(
                    SpeciesProvider(
                        speciesId = species.id!!,
                        providerId = provider.id!!,
                        imageFrontUrl = ep.imageFrontUrl,
                        imageBackUrl = ep.imageBackUrl,
                        productUrl = ep.productUrl,
                    )
                )
            }

            created++
        }

        return ImportResult(created = created, skipped = skipped)
    }

    // ── Admin Species Providers ──

    fun addSpeciesProviderAdmin(speciesId: Long, request: AddSpeciesProviderRequest): SpeciesProviderResponse {
        speciesRepository.findById(speciesId) ?: throw NotFoundException("Species not found")
        providerRepository.findById(request.providerId) ?: throw NotFoundException("Provider not found")
        var sp = speciesProviderRepository.persist(
            SpeciesProvider(speciesId = speciesId, providerId = request.providerId, productUrl = request.productUrl)
        )
        val frontUrl = request.imageFrontBase64?.let { storageService.uploadImage(it, "species/$speciesId/providers/${sp.id}/front.jpg") }
        val backUrl = request.imageBackBase64?.let { storageService.uploadImage(it, "species/$speciesId/providers/${sp.id}/back.jpg") }
        if (frontUrl != null || backUrl != null) {
            sp = sp.copy(imageFrontUrl = frontUrl, imageBackUrl = backUrl)
            speciesProviderRepository.update(sp)
        }
        return sp.toResponse()
    }

    fun updateSpeciesProviderAdmin(speciesId: Long, spId: Long, request: UpdateSpeciesProviderRequest): SpeciesProviderResponse {
        speciesRepository.findById(speciesId) ?: throw NotFoundException("Species not found")
        val sp = speciesProviderRepository.findById(spId) ?: throw NotFoundException("Species provider not found")
        if (sp.speciesId != speciesId) throw NotFoundException("Species provider not found")
        val frontUrl = request.imageFrontBase64?.let { storageService.uploadImage(it, "species/$speciesId/providers/${sp.id}/front.jpg") } ?: sp.imageFrontUrl
        val backUrl = request.imageBackBase64?.let { storageService.uploadImage(it, "species/$speciesId/providers/${sp.id}/back.jpg") } ?: sp.imageBackUrl
        val updated = sp.copy(imageFrontUrl = frontUrl, imageBackUrl = backUrl, productUrl = request.productUrl ?: sp.productUrl)
        speciesProviderRepository.update(updated)
        return updated.toResponse()
    }

    fun removeSpeciesProviderAdmin(speciesId: Long, spId: Long) {
        val sp = speciesProviderRepository.findById(spId) ?: throw NotFoundException("Species provider not found")
        if (sp.speciesId != speciesId) throw NotFoundException("Species provider not found")
        speciesProviderRepository.delete(spId)
    }

    // ── Species Providers ──

    fun getProvidersForSpecies(speciesId: Long, userId: Long): List<SpeciesProviderResponse> {
        val species = speciesRepository.findById(speciesId) ?: throw NotFoundException("Species not found")
        if (species.userId != null && species.userId != userId) throw ForbiddenException()
        return speciesProviderRepository.findBySpeciesId(speciesId).map { it.toResponse() }
    }

    fun addProviderToSpecies(speciesId: Long, request: AddSpeciesProviderRequest, userId: Long): SpeciesProviderResponse {
        val species = speciesRepository.findById(speciesId) ?: throw NotFoundException("Species not found")
        if (species.userId != null && species.userId != userId) throw ForbiddenException()
        providerRepository.findById(request.providerId) ?: throw NotFoundException("Provider not found")
        var sp = speciesProviderRepository.persist(
            SpeciesProvider(
                speciesId = speciesId,
                providerId = request.providerId,
                productUrl = request.productUrl,
            )
        )
        val frontUrl = request.imageFrontBase64?.let { storageService.uploadImage(it, "species/$speciesId/providers/${sp.id}/front.jpg") }
        val backUrl = request.imageBackBase64?.let { storageService.uploadImage(it, "species/$speciesId/providers/${sp.id}/back.jpg") }
        if (frontUrl != null || backUrl != null) {
            sp = sp.copy(imageFrontUrl = frontUrl, imageBackUrl = backUrl)
            speciesProviderRepository.update(sp)
        }
        return sp.toResponse()
    }

    fun updateSpeciesProvider(speciesId: Long, speciesProviderId: Long, request: UpdateSpeciesProviderRequest, userId: Long): SpeciesProviderResponse {
        val species = speciesRepository.findById(speciesId) ?: throw NotFoundException("Species not found")
        if (species.userId != null && species.userId != userId) throw ForbiddenException()
        val sp = speciesProviderRepository.findById(speciesProviderId) ?: throw NotFoundException("Species provider not found")
        if (sp.speciesId != speciesId) throw NotFoundException("Species provider not found")
        val frontUrl = request.imageFrontBase64?.let { storageService.uploadImage(it, "species/$speciesId/providers/${sp.id}/front.jpg") } ?: sp.imageFrontUrl
        val backUrl = request.imageBackBase64?.let { storageService.uploadImage(it, "species/$speciesId/providers/${sp.id}/back.jpg") } ?: sp.imageBackUrl
        val updated = sp.copy(
            imageFrontUrl = frontUrl,
            imageBackUrl = backUrl,
            productUrl = request.productUrl ?: sp.productUrl,
        )
        speciesProviderRepository.update(updated)
        return updated.toResponse()
    }

    fun removeProviderFromSpecies(speciesId: Long, speciesProviderId: Long, userId: Long) {
        val species = speciesRepository.findById(speciesId) ?: throw NotFoundException("Species not found")
        if (species.userId != null && species.userId != userId) throw ForbiddenException()
        val sp = speciesProviderRepository.findById(speciesProviderId) ?: throw NotFoundException("Species provider not found")
        if (sp.speciesId != speciesId) throw NotFoundException("Species provider not found")
        speciesProviderRepository.delete(speciesProviderId)
    }

    private fun SpeciesProvider.toResponse(): SpeciesProviderResponse {
        val provider = providerRepository.findById(providerId)
            ?: throw NotFoundException("Provider not found")
        return SpeciesProviderResponse(
            id = id!!,
            providerId = provider.id!!,
            providerName = provider.name,
            providerIdentifier = provider.identifier,
            imageFrontUrl = imageFrontUrl,
            imageBackUrl = imageBackUrl,
            productUrl = productUrl,
        )
    }

    // ── Mapping ──

    private fun Species.toResponse(
        groups: Map<Long?, SpeciesGroup>,
        tags: Map<Long?, SpeciesTag>,
    ): SpeciesResponse {
        val tagIds = speciesRepository.findTagIdsForSpecies(id!!)
        val photos = photoRepository.findBySpeciesId(id)
        val providers = speciesProviderRepository.findBySpeciesId(id).map { it.toResponse() }
        return SpeciesResponse(
            id = id,
            commonName = commonName,
            variantName = variantName,
            commonNameSv = commonNameSv,
            variantNameSv = variantNameSv,
            scientificName = scientificName,
            imageFrontUrl = imageFrontUrl,
            imageBackUrl = imageBackUrl,
            photos = photos.map { SpeciesPhotoResponse(it.id!!, it.imageUrl, it.sortOrder) },
            daysToSprout = daysToSprout,
            daysToHarvest = daysToHarvest,
            germinationTimeDays = germinationTimeDays,
            sowingDepthMm = sowingDepthMm,
            growingPositions = growingPositions,
            soils = soils,
            heightCm = heightCm,
            bloomMonths = bloomMonths,
            sowingMonths = sowingMonths,
            germinationRate = germinationRate,
            groupId = groupId,
            groupName = groupId?.let { groups[it]?.name },
            tags = tagIds.mapNotNull { tags[it]?.let { t -> SpeciesTagResponse(t.id!!, t.name) } },
            providers = providers,
            isSystem = userId == null,
            createdAt = createdAt,
        )
    }
}
