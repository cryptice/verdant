package app.verdant.android.data.repository

import app.verdant.android.data.api.VerdantApi
import app.verdant.android.data.model.CreateSeasonRequest
import app.verdant.android.data.model.SeasonResponse
import javax.inject.Inject
import javax.inject.Singleton

/** Seasons. Interface so ViewModels can be tested with a fake. */
interface SeasonRepository {
    suspend fun list(): List<SeasonResponse>
    suspend fun create(request: CreateSeasonRequest): SeasonResponse
    suspend fun update(id: Long, request: Map<String, Any?>): SeasonResponse
    suspend fun delete(id: Long)
}

@Singleton
class SeasonRepositoryImpl @Inject constructor(private val api: VerdantApi) : SeasonRepository {
    override suspend fun list() = api.getSeasons()
    override suspend fun create(request: CreateSeasonRequest) = api.createSeason(request)
    override suspend fun update(id: Long, request: Map<String, Any?>) = api.updateSeason(id, request)
    override suspend fun delete(id: Long) { api.deleteSeason(id) }
}
