package app.verdant.android.data.repository

import app.verdant.android.data.TokenStore
import app.verdant.android.data.api.VerdantApi
import app.verdant.android.data.model.AuthResponse
import app.verdant.android.data.model.GoogleAuthRequest
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val tokenStore: TokenStore,
    private val api: VerdantApi
) {
    val token: Flow<String?> = tokenStore.token

    suspend fun getToken(): String? = tokenStore.getToken()

    suspend fun signIn(googleIdToken: String): AuthResponse {
        val response = api.googleAuth(GoogleAuthRequest(googleIdToken))
        tokenStore.saveToken(response.token)
        return response
    }

    suspend fun signOut() {
        tokenStore.clear()
    }
}
