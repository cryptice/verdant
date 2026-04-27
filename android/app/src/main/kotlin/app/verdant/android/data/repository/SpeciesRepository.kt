package app.verdant.android.data.repository

import app.verdant.android.data.api.VerdantApi
import app.verdant.android.data.model.CreateSpeciesGroupRequest
import app.verdant.android.data.model.CreateSpeciesRequest
import app.verdant.android.data.model.CreateSpeciesTagRequest
import app.verdant.android.data.model.RecordCommentRequest
import app.verdant.android.data.model.UpdateSpeciesRequest
import javax.inject.Inject
import javax.inject.Singleton

/** Species, groups, tags, frequent comments. */
@Singleton
class SpeciesRepository @Inject constructor(private val api: VerdantApi) {
    suspend fun list() = api.getSpecies()
    suspend fun create(request: CreateSpeciesRequest) = api.createSpecies(request)
    suspend fun update(id: Long, request: UpdateSpeciesRequest) = api.updateSpecies(id, request)
    suspend fun delete(id: Long) = api.deleteSpecies(id)

    suspend fun listGroups() = api.getSpeciesGroups()
    suspend fun createGroup(request: CreateSpeciesGroupRequest) = api.createSpeciesGroup(request)
    suspend fun deleteGroup(id: Long) = api.deleteSpeciesGroup(id)

    suspend fun listTags() = api.getSpeciesTags()
    suspend fun createTag(request: CreateSpeciesTagRequest) = api.createSpeciesTag(request)
    suspend fun deleteTag(id: Long) = api.deleteSpeciesTag(id)

    // Frequent comments are species-scoped in practice
    suspend fun frequentComments() = api.getFrequentComments()
    suspend fun recordComment(request: RecordCommentRequest) = api.recordComment(request)
    suspend fun deleteComment(id: Long) = api.deleteComment(id)
}
