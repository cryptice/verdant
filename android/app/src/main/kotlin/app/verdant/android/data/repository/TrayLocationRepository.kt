package app.verdant.android.data.repository

import app.verdant.android.data.api.VerdantApi
import app.verdant.android.data.model.BulkLocationNoteRequest
import app.verdant.android.data.model.CreateTrayLocationRequest
import app.verdant.android.data.model.MoveTrayLocationRequest
import app.verdant.android.data.model.UpdateTrayLocationRequest
import javax.inject.Inject
import javax.inject.Singleton

/** Tray locations + bulk water/note/move scoped by location. */
@Singleton
class TrayLocationRepository @Inject constructor(private val api: VerdantApi) {
    suspend fun list() = api.getTrayLocations()
    suspend fun create(name: String) = api.createTrayLocation(CreateTrayLocationRequest(name))
    suspend fun update(id: Long, name: String) = api.updateTrayLocation(id, UpdateTrayLocationRequest(name))
    suspend fun delete(id: Long) = api.deleteTrayLocation(id)
    suspend fun water(id: Long) = api.waterTrayLocation(id)
    suspend fun note(id: Long, text: String) = api.noteTrayLocation(id, BulkLocationNoteRequest(text))
    suspend fun move(id: Long, request: MoveTrayLocationRequest) = api.moveTrayLocation(id, request)
}
