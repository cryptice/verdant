package app.verdant.android.data.repository

import app.verdant.android.data.api.VerdantApi
import app.verdant.android.data.model.CreateSeasonRequest
import javax.inject.Inject
import javax.inject.Singleton

/** Seasons. */
@Singleton
class SeasonRepository @Inject constructor(private val api: VerdantApi) {
    suspend fun list() = api.getSeasons()
    suspend fun create(request: CreateSeasonRequest) = api.createSeason(request)
    suspend fun update(id: Long, request: Map<String, Any?>) = api.updateSeason(id, request)
    suspend fun delete(id: Long) = api.deleteSeason(id)
}
