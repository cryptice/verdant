package app.verdant.android.data.model

import com.google.gson.annotations.SerializedName

data class GoogleAuthRequest(@SerializedName("idToken") val idToken: String)
data class AuthResponse(@SerializedName("token") val token: String, @SerializedName("user") val user: UserResponse)

data class UserOrgMembership(
    @SerializedName("orgId") val orgId: Long,
    @SerializedName("orgName") val orgName: String,
    @SerializedName("orgEmoji") val orgEmoji: String?,
    @SerializedName("role") val role: String,
)

data class UserResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("email") val email: String,
    @SerializedName("displayName") val displayName: String,
    @SerializedName("avatarUrl") val avatarUrl: String?,
    @SerializedName("role") val role: String,
    @SerializedName("language") val language: String = "sv",
    @SerializedName("onboarding") val onboarding: String? = null,
    @SerializedName("advancedMode") val advancedMode: Boolean = false,
    @SerializedName("organizations") val organizations: List<UserOrgMembership> = emptyList(),
    @SerializedName("createdAt") val createdAt: String
)

data class UpdateUserRequest(
    @SerializedName("displayName") val displayName: String? = null,
    @SerializedName("avatarUrl") val avatarUrl: String? = null,
    @SerializedName("language") val language: String? = null,
)
