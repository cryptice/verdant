package app.verdant.dto

import app.verdant.entity.OrgRole
import app.verdant.entity.InviteStatus
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant

data class OrganizationResponse(
    val id: Long,
    val name: String,
    val emoji: String?,
    val role: OrgRole,
    val createdAt: Instant,
)

data class CreateOrganizationRequest(
    @field:NotBlank @field:Size(max = 255)
    val name: String,
    @field:Size(max = 10)
    val emoji: String? = null,
)

data class UpdateOrganizationRequest(
    @field:Size(max = 255)
    val name: String? = null,
    @field:Size(max = 10)
    val emoji: String? = null,
)

data class OrgMemberResponse(
    val id: Long,
    val userId: Long,
    val email: String,
    val displayName: String,
    val avatarUrl: String?,
    val role: OrgRole,
    val joinedAt: Instant,
)

data class InviteMemberRequest(
    @field:NotBlank
    val email: String,
)

data class OrgInviteResponse(
    val id: Long,
    val orgId: Long,
    val orgName: String,
    val email: String,
    val invitedByName: String,
    val status: InviteStatus,
    val createdAt: Instant,
)
