package app.verdant.android.data.model

import com.google.gson.annotations.SerializedName

data class OrganizationResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("emoji") val emoji: String?,
    @SerializedName("role") val role: String,
    @SerializedName("createdAt") val createdAt: String,
)

data class OrgLookupResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("emoji") val emoji: String?,
)

data class OrgMemberResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("userId") val userId: Long,
    @SerializedName("email") val email: String,
    @SerializedName("displayName") val displayName: String,
    @SerializedName("avatarUrl") val avatarUrl: String?,
    @SerializedName("role") val role: String,
    @SerializedName("joinedAt") val joinedAt: String,
)

data class OrgInviteResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("orgId") val orgId: Long,
    @SerializedName("orgName") val orgName: String,
    @SerializedName("email") val email: String,
    @SerializedName("invitedByName") val invitedByName: String,
    @SerializedName("status") val status: String,
    @SerializedName("createdAt") val createdAt: String,
)

data class OrgJoinRequestResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("orgId") val orgId: Long,
    @SerializedName("orgName") val orgName: String,
    @SerializedName("userId") val userId: Long,
    @SerializedName("userEmail") val userEmail: String,
    @SerializedName("userDisplayName") val userDisplayName: String,
    @SerializedName("status") val status: String,
    @SerializedName("createdAt") val createdAt: String,
)

data class CreateOrganizationRequest(
    @SerializedName("name") val name: String,
    @SerializedName("emoji") val emoji: String? = null,
)

data class UpdateOrganizationRequest(
    @SerializedName("name") val name: String? = null,
    @SerializedName("emoji") val emoji: String? = null,
)

data class InviteMemberRequest(
    @SerializedName("email") val email: String,
)
