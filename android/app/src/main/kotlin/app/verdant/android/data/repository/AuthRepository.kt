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

interface InviteOps {
    suspend fun listPendingInvites(): List<app.verdant.android.data.model.OrgInviteResponse>
    suspend fun acceptInvite(inviteId: Long)
    suspend fun declineInvite(inviteId: Long)
}

interface UserRefresher {
    suspend fun refreshUser(): app.verdant.android.data.model.UserResponse
}

interface Signer {
    suspend fun signOut()
}

@Singleton
class AuthRepository @Inject constructor(
    private val tokenStore: TokenStore,
    private val orgStore: OrgStore,
    private val api: VerdantApi,
) : InviteOps, UserRefresher, Signer {
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
    override suspend fun refreshUser(): UserResponse {
        val user = api.getMe()
        saveFirstOrg(user)
        return user
    }

    override suspend fun signOut() {
        tokenStore.clear()
        orgStore.clear()
    }

    override suspend fun listPendingInvites(): List<app.verdant.android.data.model.OrgInviteResponse> =
        api.listMyInvites()

    override suspend fun acceptInvite(inviteId: Long) {
        val org = api.acceptInvite(inviteId)
        orgStore.saveOrgId(org.id)
    }

    override suspend fun declineInvite(inviteId: Long) {
        api.declineInvite(inviteId)
    }

    private suspend fun saveFirstOrg(user: UserResponse) {
        user.organizations.firstOrNull()?.let { orgStore.saveOrgId(it.orgId) }
    }
}
