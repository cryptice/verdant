package app.verdant.android.data.repository

import app.verdant.android.data.OrgStore
import app.verdant.android.data.TokenStore
import app.verdant.android.data.api.VerdantApi
import app.verdant.android.data.model.AuthResponse
import app.verdant.android.data.model.GoogleAuthRequest
import app.verdant.android.data.model.UserResponse
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val tokenStore: TokenStore,
    private val orgStore: OrgStore,
    private val api: VerdantApi
) {
    val token: Flow<String?> = tokenStore.token

    suspend fun getToken(): String? = tokenStore.getToken()

    suspend fun signIn(googleIdToken: String): AuthResponse {
        val response = api.googleAuth(GoogleAuthRequest(googleIdToken))
        tokenStore.saveToken(response.token)
        saveFirstOrg(response.user)
        return response
    }

    /** Refresh the current user's data (hits the org-exempt /api/users/me endpoint)
     *  and persist the active org ID so subsequent API calls carry the X-Organization-Id header. */
    suspend fun refreshUser(): UserResponse {
        val user = api.getMe()
        saveFirstOrg(user)
        return user
    }

    suspend fun signOut() {
        tokenStore.clear()
        orgStore.clear()
    }

    private suspend fun saveFirstOrg(user: UserResponse) {
        user.organizations.firstOrNull()?.let { orgStore.saveOrgId(it.orgId) }
    }
}
