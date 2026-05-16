package app.verdant.android.data.repository

import app.verdant.android.data.api.VerdantApi
import app.verdant.android.data.model.CreateSpeciesGroupRequest
import app.verdant.android.data.model.CreateSpeciesRequest
import app.verdant.android.data.model.CreateSpeciesTagRequest
import app.verdant.android.data.model.FrequentCommentResponse
import app.verdant.android.data.model.RecordCommentRequest
import app.verdant.android.data.model.SpeciesGroupResponse
import app.verdant.android.data.model.SpeciesResponse
import app.verdant.android.data.model.SpeciesTagResponse
import app.verdant.android.data.model.UpdateSpeciesRequest
import javax.inject.Inject
import javax.inject.Singleton

/** Species, groups, tags, frequent comments. Interface so ViewModels can be tested with a fake. */
interface SpeciesRepository {
    suspend fun list(): List<SpeciesResponse>
    suspend fun create(request: CreateSpeciesRequest): SpeciesResponse
    suspend fun update(id: Long, request: UpdateSpeciesRequest): SpeciesResponse
    suspend fun delete(id: Long)
    suspend fun listGroups(): List<SpeciesGroupResponse>
    suspend fun createGroup(request: CreateSpeciesGroupRequest): SpeciesGroupResponse
    suspend fun deleteGroup(id: Long)
    suspend fun listTags(): List<SpeciesTagResponse>
    suspend fun createTag(request: CreateSpeciesTagRequest): SpeciesTagResponse
    suspend fun deleteTag(id: Long)
    suspend fun frequentComments(): List<FrequentCommentResponse>
    suspend fun recordComment(request: RecordCommentRequest): FrequentCommentResponse
    suspend fun deleteComment(id: Long)
}

@Singleton
class SpeciesRepositoryImpl @Inject constructor(private val api: VerdantApi) : SpeciesRepository {
    override suspend fun list() = api.getSpecies()
    override suspend fun create(request: CreateSpeciesRequest) = api.createSpecies(request)
    override suspend fun update(id: Long, request: UpdateSpeciesRequest) = api.updateSpecies(id, request)
    override suspend fun delete(id: Long) { api.deleteSpecies(id) }
    override suspend fun listGroups() = api.getSpeciesGroups()
    override suspend fun createGroup(request: CreateSpeciesGroupRequest) = api.createSpeciesGroup(request)
    override suspend fun deleteGroup(id: Long) { api.deleteSpeciesGroup(id) }
    override suspend fun listTags() = api.getSpeciesTags()
    override suspend fun createTag(request: CreateSpeciesTagRequest) = api.createSpeciesTag(request)
    override suspend fun deleteTag(id: Long) { api.deleteSpeciesTag(id) }
    override suspend fun frequentComments() = api.getFrequentComments()
    override suspend fun recordComment(request: RecordCommentRequest) = api.recordComment(request)
    override suspend fun deleteComment(id: Long) { api.deleteComment(id) }
}
