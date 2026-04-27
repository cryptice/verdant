package app.verdant.android.data.repository

import app.verdant.android.data.api.VerdantApi
import app.verdant.android.data.model.UpdateUserRequest
import javax.inject.Inject
import javax.inject.Singleton

/** Authenticated user profile. */
@Singleton
class UserRepository @Inject constructor(private val api: VerdantApi) {
    suspend fun me() = api.getMe()
    suspend fun updateMe(request: UpdateUserRequest) = api.updateMe(request)
    suspend fun deleteMe() = api.deleteMe()
}
