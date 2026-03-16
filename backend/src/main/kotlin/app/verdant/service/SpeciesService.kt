package app.verdant.service

import app.verdant.dto.*
import app.verdant.entity.Species
import app.verdant.entity.SpeciesGroup
import app.verdant.entity.SpeciesTag
import app.verdant.repository.SpeciesGroupRepository
import app.verdant.repository.SpeciesPhotoRepository
import app.verdant.repository.SpeciesRepository
import app.verdant.repository.SpeciesTagRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.ForbiddenException
import jakarta.ws.rs.NotFoundException

@ApplicationScoped
class SpeciesService(
    private val speciesRepository: SpeciesRepository,
    private val groupRepository: SpeciesGroupRepository,
    private val tagRepository: SpeciesTagRepository,
    private val photoRepository: SpeciesPhotoRepository,
    private val storageService: StorageService,
) {
    // ── Species CRUD ──

    fun getSpeciesForUser(userId: Long): List<SpeciesResponse> {
        val groups = groupRepository.findByUserId(userId).associateBy { it.id }
        val tags = tagRepository.findByUserId(userId).associateBy { it.id }
        return speciesRepository.findByUserId(userId).map { it.toResponse(groups, tags) }
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
                commonNameSv = request.commonNameSv,
                scientificName = request.scientificName,
                daysToSprout = request.daysToSprout,
                daysToHarvest = request.daysToHarvest,
                germinationTimeDays = request.germinationTimeDays,
                sowingDepthMm = request.sowingDepthMm,
                growingPositions = request.growingPositions,
                soils = request.soils,
                heightCm = request.heightCm,
                bloomTime = request.bloomTime,
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
        if (request.tagIds.isNotEmpty()) {
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
            commonNameSv = request.commonNameSv ?: species.commonNameSv,
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
            bloomTime = request.bloomTime ?: species.bloomTime,
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

    // ── Mapping ──

    private fun Species.toResponse(
        groups: Map<Long?, SpeciesGroup>,
        tags: Map<Long?, SpeciesTag>,
    ): SpeciesResponse {
        val tagIds = speciesRepository.findTagIdsForSpecies(id!!)
        val photos = photoRepository.findBySpeciesId(id)
        return SpeciesResponse(
            id = id,
            commonName = commonName,
            commonNameSv = commonNameSv,
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
            bloomTime = bloomTime,
            germinationRate = germinationRate,
            groupId = groupId,
            groupName = groupId?.let { groups[it]?.name },
            tags = tagIds.mapNotNull { tags[it]?.let { t -> SpeciesTagResponse(t.id!!, t.name) } },
            isSystem = userId == null,
            createdAt = createdAt,
        )
    }
}
